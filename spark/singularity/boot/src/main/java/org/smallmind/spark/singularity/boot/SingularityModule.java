/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.spark.singularity.boot;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Run-time view of one module on a Singularity bundle's module path, rebuilt by {@link SingularityClassLoader} from a
 * {@link SingularityIndex.ModuleEntry}. It knows the module's descriptor, how to form the URL of each of its entries
 * (resolving multi-release versions against the running JDK), and whether a resource in one of its packages may be
 * handed to code outside the module.
 * <p>Multi-release resolution is done once, at construction, so a lookup is a single map access; an entry's URL is
 * built the first time it is asked for and reused afterwards.
 */
public class SingularityModule {

  private static final String VERSIONS_PREFIX = "META-INF/versions/";
  private static final int RUNTIME_FEATURE_VERSION = Runtime.version().feature();
  private final ModuleDescriptor moduleDescriptor;
  private final HashMap<String, String> resolvedEntryNameMap = new HashMap<>();
  private final ConcurrentHashMap<String, URL> entryURLMap = new ConcurrentHashMap<>();
  private final String scheme;
  private final String entryPrefix;
  private final URI location;
  private final boolean multiRelease;

  /**
   * Rebuilds a module from its index entry.
   *
   * @param moduleEntry      the build-time description of the module
   * @param parentJarUrlPart the external form of the outer Singularity jar's URL
   * @throws URISyntaxException if the module's location cannot be expressed as a URI
   */
  public SingularityModule (SingularityIndex.ModuleEntry moduleEntry, String parentJarUrlPart)
    throws URISyntaxException {

    multiRelease = moduleEntry.multiRelease();
    resolveEntryNames(moduleEntry);

    if (moduleEntry.jarName() == null) {
      scheme = "jar";
      entryPrefix = parentJarUrlPart + "!/";
      location = new URI("jar", entryPrefix, null);
    } else {
      scheme = "singularity";
      entryPrefix = parentJarUrlPart + "@/META-INF/singularity/lib/" + moduleEntry.jarName() + "!/";
      location = new URI("singularity", parentJarUrlPart + "@/META-INF/singularity/lib/" + moduleEntry.jarName(), null);
    }

    if (moduleEntry.descriptorBytes() != null) {
      moduleDescriptor = ModuleDescriptor.read(ByteBuffer.wrap(moduleEntry.descriptorBytes()), moduleEntry::packageSet);
    } else {

      ModuleDescriptor.Builder builder = ModuleDescriptor.newAutomaticModule(moduleEntry.moduleName()).packages(moduleEntry.packageSet());

      for (Map.Entry<String, List<String>> providerEntry : moduleEntry.providerMap().entrySet()) {
        builder.provides(providerEntry.getKey(), providerEntry.getValue());
      }

      moduleDescriptor = builder.build();
    }
  }

  /**
   * Converts a resource path to the name of the package that would contain it.
   *
   * @param entryName a resource path using forward slashes
   * @return the package name, or the empty string for a root-level entry
   */
  public static String toPackageName (String entryName) {

    int lastSlashPos;

    if ((lastSlashPos = entryName.lastIndexOf('/')) <= 0) {

      return "";
    }

    return entryName.substring(0, lastSlashPos).replace('/', '.');
  }

  /**
   * @return the module's name
   */
  public String getName () {

    return moduleDescriptor.name();
  }

  /**
   * @return the module's descriptor, explicit or automatic
   */
  public ModuleDescriptor getModuleDescriptor () {

    return moduleDescriptor;
  }

  /**
   * @return the location of the module, which is the outer jar for the application module and the nested jar
   * otherwise; it is also the code source of every class the module defines
   */
  public URI getLocation () {

    return location;
  }

  /**
   * @param packageName a package name
   * @return whether the package belongs to this module
   */
  public boolean containsPackage (String packageName) {

    return moduleDescriptor.packages().contains(packageName);
  }

  /**
   * Decides whether a resource of this module may be returned by the module-unaware {@code ClassLoader} lookups, using
   * the JDK's own rule: class files, entries outside every package of the module, and entries in packages that the
   * module opens to everyone are visible; everything else is encapsulated.
   *
   * @param entryName a resource path using forward slashes
   * @return {@code true} if the resource is visible outside the module
   */
  public boolean isVisible (String entryName) {

    return isVisible(entryName, toPackageName(entryName));
  }

  /**
   * Same as {@link #isVisible(String)}, for a caller that has already computed the entry's package name.
   *
   * @param entryName   a resource path using forward slashes
   * @param packageName the package name of {@code entryName}, as returned by {@link #toPackageName(String)}
   * @return {@code true} if the resource is visible outside the module
   */
  public boolean isVisible (String entryName, String packageName) {

    if (entryName.endsWith(".class") || (!containsPackage(packageName)) || moduleDescriptor.isOpen() || moduleDescriptor.isAutomatic()) {

      return true;
    }

    for (ModuleDescriptor.Opens opens : moduleDescriptor.opens()) {
      if (opens.source().equals(packageName) && (!opens.isQualified())) {

        return true;
      }
    }

    return false;
  }

  /**
   * Forms the URL of an entry, selecting the highest {@code META-INF/versions/} variant that the running JDK supports
   * when the module comes from a multi-release jar.
   *
   * @param entryName a resource path using forward slashes, without any version prefix
   * @return the entry's URL, or {@code null} if the module has no such entry
   */
  public URL getEntryURL (String entryName) {

    String resolvedEntryName;
    URL entryURL;

    if ((resolvedEntryName = resolvedEntryNameMap.get(entryName)) == null) {

      return null;
    }

    if ((entryURL = entryURLMap.get(entryName)) == null) {
      try {
        entryURL = new URI(scheme, entryPrefix + resolvedEntryName, null).toURL();
      } catch (URISyntaxException | MalformedURLException exception) {
        throw new RuntimeException(exception);
      }

      entryURLMap.putIfAbsent(entryName, entryURL);
    }

    return entryURL;
  }

  /**
   * Lists the module's entries that lie outside every one of its packages, such as {@code META-INF/} resources, files
   * at the root of the jar, and the literal {@code META-INF/versions/} entries of a multi-release jar (which the JDK's
   * own module reader also serves by name). Lookups of such names cannot be routed by package, so the class loader
   * indexes them by name.
   *
   * @return the names of the module's non-package entries
   */
  public List<String> listNonPackageEntryNames () {

    LinkedList<String> nonPackageEntryNameList = new LinkedList<>();

    for (String entryName : resolvedEntryNameMap.keySet()) {
      if (!containsPackage(toPackageName(entryName))) {
        nonPackageEntryNameList.add(entryName);
      }
    }

    return nonPackageEntryNameList;
  }

  /**
   * @return every logical (unversioned) entry name in the module
   */
  public Stream<String> streamEntryNames () {

    return resolvedEntryNameMap.keySet().stream().filter((entryName) -> !entryName.startsWith(VERSIONS_PREFIX));
  }

  /**
   * @return a module reference over this module, suitable for a {@link java.lang.module.ModuleFinder}
   */
  public ModuleReference createModuleReference () {

    return new SingularityModuleReference();
  }

  /**
   * Maps every entry name to itself and then, for a multi-release jar, maps each logical name to the highest
   * {@code META-INF/versions/<n>/} variant with {@code 9 <= n <=} the running JDK's feature release. Names under
   * {@code META-INF/} are never versioned.
   *
   * @param moduleEntry the build-time description of the module
   */
  private void resolveEntryNames (SingularityIndex.ModuleEntry moduleEntry) {

    HashMap<String, Integer> resolvedVersionMap = new HashMap<>();

    for (String entryName : moduleEntry.entryNameSet()) {
      resolvedEntryNameMap.put(entryName, entryName);
    }

    if (multiRelease) {
      for (String entryName : moduleEntry.entryNameSet()) {
        if (entryName.startsWith(VERSIONS_PREFIX)) {

          int versionEndPos = entryName.indexOf('/', VERSIONS_PREFIX.length());

          if (versionEndPos > VERSIONS_PREFIX.length()) {

            String logicalEntryName = entryName.substring(versionEndPos + 1);
            Integer resolvedVersion;
            int version;

            try {
              version = Integer.parseInt(entryName.substring(VERSIONS_PREFIX.length(), versionEndPos));
            } catch (NumberFormatException numberFormatException) {
              continue;
            }

            if ((version >= 9) && (version <= RUNTIME_FEATURE_VERSION) && (!logicalEntryName.startsWith("META-INF/")) && (((resolvedVersion = resolvedVersionMap.get(logicalEntryName)) == null) || (version > resolvedVersion))) {
              resolvedVersionMap.put(logicalEntryName, version);
              resolvedEntryNameMap.put(logicalEntryName, entryName);
            }
          }
        }
      }
    }
  }

  private class SingularityModuleReference extends ModuleReference {

    private SingularityModuleReference () {

      super(moduleDescriptor, location);
    }

    @Override
    public ModuleReader open () {

      return new SingularityModuleReader();
    }
  }

  private class SingularityModuleReader implements ModuleReader {

    @Override
    public Optional<URI> find (String name)
      throws IOException {

      URL entryURL;

      if ((entryURL = getEntryURL(name)) == null) {

        return Optional.empty();
      }

      try {
        return Optional.of(entryURL.toURI());
      } catch (URISyntaxException uriSyntaxException) {
        throw new IOException(uriSyntaxException);
      }
    }

    @Override
    public Stream<String> list () {

      return streamEntryNames();
    }

    @Override
    public void close () {

    }
  }
}
