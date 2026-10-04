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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Serializable manifest of the contents of a Singularity bundle. The Maven plugin builds an instance at packaging
 * time, writes it to {@code META-INF/singularity/index/singularity.idx}, and {@link SingularityClassLoader} reads it
 * at boot to learn how to form a URL for each class and resource.
 * <p>The index distinguishes two populations:
 * <ul>
 *   <li>Bare files laid down directly in the outer jar (boot classes, user classes, indexed resources).</li>
 *   <li>Entries drawn from bundled libraries, each pointing back to every nested jar that supplies it, in the order
 *   the jars were added (the plugin adds them in runtime class path order).</li>
 * </ul>
 * <p>A bundle built in modular mode additionally carries one {@link ModuleEntry} per module that the plugin resolved
 * onto the module path. Entries belonging to those modules are recorded only in their module entry, never in the two
 * populations above, which then describe the class path alone.
 */
public class SingularityIndex implements Serializable {

  private final HashMap<String, String> inverseEntryMap = new HashMap<>();
  private final HashMap<String, ArrayList<String>> additionalInverseEntryMap = new HashMap<>();
  private final HashSet<String> fileNameSet = new HashSet<>();
  private final HashMap<String, ModuleEntry> moduleEntryMap = new HashMap<>();

  /**
   * Records that an entry observed inside a bundled library jar can be served from that jar at runtime. When several
   * jars contain the same entry, each is kept, in the order in which they were recorded; recording the same jar for
   * the same entry twice has no effect.
   *
   * @param entryName the path of the entry as it appears inside the library jar
   * @param jarName   the filename of the library jar under {@code META-INF/singularity/lib/}
   */
  public void addInverseJarEntry (String entryName, String jarName) {

    String firstJarName;

    // nearly every entry has exactly one source, so only the rare later sources pay for a list
    if (((firstJarName = inverseEntryMap.putIfAbsent(entryName, jarName)) != null) && (!firstJarName.equals(jarName))) {

      ArrayList<String> jarNameList;

      if ((jarNameList = additionalInverseEntryMap.get(entryName)) == null) {
        additionalInverseEntryMap.put(entryName, jarNameList = new ArrayList<>(1));
      }
      if (!jarNameList.contains(jarName)) {
        jarNameList.add(jarName);
      }
    }
  }

  /**
   * Records a file laid down directly inside the outer Singularity jar.
   *
   * @param fileName resource-style path (forward slashes) of the file relative to the jar root
   */
  public void addFileName (String fileName) {

    fileNameSet.add(fileName);
  }

  /**
   * Records a module that the plugin resolved onto the module path, replacing any earlier entry of the same name.
   *
   * @param moduleEntry the module's name, provenance, descriptor, and contents
   */
  public void addModuleEntry (ModuleEntry moduleEntry) {

    moduleEntryMap.put(moduleEntry.moduleName(), moduleEntry);
  }

  /**
   * Returns the modules recorded for the module path; empty for a bundle built in class path mode.
   *
   * @return the recorded module entries, in no particular order
   */
  public Iterable<ModuleEntry> getModuleEntryIterable () {

    return moduleEntryMap.values();
  }

  /**
   * Forms the {@code jar:} URL of a file laid down directly in the outer jar.
   *
   * @param parentJarUrlPart the external form of the outer jar's URL
   * @param entryName        the file's path relative to the jar root
   * @return the URL of the file
   * @throws RuntimeException wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the composed
   *                          URL is not well formed
   */
  public static URL createOuterJarURL (String parentJarUrlPart, String entryName) {

    try {
      return new URI("jar", parentJarUrlPart + "!/" + entryName, null).toURL();
    } catch (URISyntaxException | MalformedURLException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Forms the {@code singularity:} URL of an entry inside a bundled library jar.
   *
   * @param parentJarUrlPart the external form of the outer jar's URL
   * @param jarName          the filename of the library jar under {@code META-INF/singularity/lib/}
   * @param entryName        the entry's path inside the library jar
   * @return the URL of the entry
   * @throws RuntimeException wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the composed
   *                          URL is not well formed
   */
  public static URL createNestedJarURL (String parentJarUrlPart, String jarName, String entryName) {

    try {
      return new URI("singularity", parentJarUrlPart + "@/META-INF/singularity/lib/" + jarName + "!/" + entryName, null).toURL();
    } catch (URISyntaxException | MalformedURLException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Exposes the names of the files laid down directly in the outer jar, without forming their URLs.
   *
   * @return the file names, in arbitrary order
   */
  public Iterable<String> getFileNameIterable () {

    return Collections.unmodifiableSet(fileNameSet);
  }

  /**
   * Exposes every library-jar entry as a {@link SourceEntry}, without forming its URL, in the same order as
   * {@link #getSingularityURLEntryIterable(String)}.
   *
   * @return an {@link Iterable} that yields one source entry per entry and supplying jar
   */
  public Iterable<SourceEntry> getNestedJarSourceIterable () {

    return new NestedJarSourceIterator();
  }

  /**
   * Exposes every directly stored file as a {@link URLEntry} whose URL uses the standard {@code jar:} protocol.
   *
   * @param parentJarUrlPart the external form of the enclosing jar's URL, used as the prefix for each entry
   * @return an {@link Iterable} that yields one URL entry per file, in arbitrary iteration order
   */
  public Iterable<URLEntry> getJarURLEntryIterable (String parentJarUrlPart) {

    return new JarURLIterator(parentJarUrlPart);
  }

  /**
   * Exposes every library-jar entry as a {@link URLEntry} whose URL uses the {@code singularity:} protocol so that
   * the custom connection can resolve it through the nested jar.
   *
   * @param parentJarUrlPart the external form of the enclosing jar's URL, used as the prefix for each entry
   * @return an {@link Iterable} that yields one URL entry per entry and supplying jar: first the earliest-recorded jar
   * of every entry, in arbitrary order, then the later jars of the entries that have them, each entry's jars in the
   * order recorded
   */
  public Iterable<URLEntry> getSingularityURLEntryIterable (String parentJarUrlPart) {

    return new SingularityURLIterator(parentJarUrlPart);
  }

  private class JarURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final Iterator<String> fileNameIter = fileNameSet.iterator();
    private final String parentJarUrlPart;

    /**
     * Captures the parent jar URL that will prefix each {@code jar:}-protocol URL produced by this iterator.
     *
     * @param parentJarUrlPart external form of the outer jar URL
     */
    public JarURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    /**
     * Lets this type serve as its own iterator.
     *
     * @return {@code this}
     */
    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    /**
     * Indicates whether another file name remains to be converted into a {@link URLEntry}.
     *
     * @return {@code true} when at least one unvisited file name remains
     */
    @Override
    public boolean hasNext () {

      return fileNameIter.hasNext();
    }

    /**
     * Produces the next {@link URLEntry} using a standard {@code jar:} URL that references the file in the outer jar.
     *
     * @return a URL entry whose name is the file path and whose URL targets that file inside the outer jar
     * @throws RuntimeException wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the
     *                          composed URL is not well formed
     */
    @Override
    public URLEntry next () {

      String fileName = fileNameIter.next();

      return new URLEntry(fileName, createOuterJarURL(parentJarUrlPart, fileName));
    }

    /**
     * Removal is not meaningful because the backing set is treated as immutable once the index has been written.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  private class SingularityURLIterator implements Iterator<URLEntry>, Iterable<URLEntry> {

    private final NestedJarSourceIterator sourceIter = new NestedJarSourceIterator();
    private final String parentJarUrlPart;

    /**
     * Captures the parent jar URL that will prefix each {@code singularity:}-protocol URL produced by this iterator.
     *
     * @param parentJarUrlPart external form of the outer jar URL
     */
    public SingularityURLIterator (String parentJarUrlPart) {

      this.parentJarUrlPart = parentJarUrlPart;
    }

    /**
     * Lets this type serve as its own iterator.
     *
     * @return {@code this}
     */
    @Override
    public Iterator<URLEntry> iterator () {

      return this;
    }

    /**
     * Indicates whether another entry and jar pairing remains to be converted into a {@link URLEntry}.
     *
     * @return {@code true} when at least one unvisited pairing remains
     */
    @Override
    public boolean hasNext () {

      return sourceIter.hasNext();
    }

    /**
     * Produces the next {@link URLEntry} using a {@code singularity:} URL that points through the outer jar to the
     * specific library jar and, from there, to the requested entry.
     *
     * @return a URL entry whose name is the entry path and whose URL targets that entry inside one bundled jar
     * @throws NoSuchElementException if every pairing has been visited
     * @throws RuntimeException       wrapping a {@link URISyntaxException} or {@link MalformedURLException} when the
     *                                composed URL is not well formed
     */
    @Override
    public URLEntry next () {

      SourceEntry sourceEntry = sourceIter.next();

      return new URLEntry(sourceEntry.entryName(), createNestedJarURL(parentJarUrlPart, sourceEntry.jarName(), sourceEntry.entryName()));
    }

    /**
     * Removal is not meaningful because the backing map is treated as immutable once the index has been written.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  private class NestedJarSourceIterator implements Iterator<SourceEntry>, Iterable<SourceEntry> {

    private final Iterator<Map.Entry<String, String>> inverseEntryIter = inverseEntryMap.entrySet().iterator();
    private final Iterator<Map.Entry<String, ArrayList<String>>> additionalInverseEntryIter = additionalInverseEntryMap.entrySet().iterator();
    private Map.Entry<String, ArrayList<String>> currentAdditionalEntry;
    private int jarIndex;

    /**
     * Lets this type serve as its own iterator.
     *
     * @return {@code this}
     */
    @Override
    public Iterator<SourceEntry> iterator () {

      return this;
    }

    /**
     * Indicates whether another entry and jar pairing remains. Once every earliest-recorded pairing has been visited,
     * advances through the later jars of each entry that has them.
     *
     * @return {@code true} when at least one unvisited pairing remains
     */
    @Override
    public boolean hasNext () {

      if (inverseEntryIter.hasNext()) {

        return true;
      }

      while ((currentAdditionalEntry == null) || (jarIndex >= currentAdditionalEntry.getValue().size())) {
        if (!additionalInverseEntryIter.hasNext()) {

          return false;
        }

        currentAdditionalEntry = additionalInverseEntryIter.next();
        jarIndex = 0;
      }

      return true;
    }

    /**
     * Produces the next pairing of an entry name with a jar that supplies it: first the earliest-recorded jar of every
     * entry, then the later jars of the entries that have them, each entry's jars in the order recorded.
     *
     * @return the next source entry
     * @throws NoSuchElementException if every pairing has been visited
     */
    @Override
    public SourceEntry next () {

      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      if (inverseEntryIter.hasNext()) {

        Map.Entry<String, String> inverseEntry = inverseEntryIter.next();

        return new SourceEntry(inverseEntry.getKey(), inverseEntry.getValue());
      }

      return new SourceEntry(currentAdditionalEntry.getKey(), currentAdditionalEntry.getValue().get(jarIndex++));
    }

    /**
     * Removal is not meaningful because the backing map is treated as immutable once the index has been written.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove () {

      throw new UnsupportedOperationException();
    }
  }

  /**
   * Pairing of an entry's name with the filename of a bundled library jar that supplies it.
   *
   * @param entryName the entry's path inside the library jar
   * @param jarName   the filename of the library jar under {@code META-INF/singularity/lib/}
   */
  public record SourceEntry(String entryName, String jarName) {

  }

  /**
   * Immutable pairing of an entry's logical name (the key used by class/resource lookup) with the {@link URL} at
   * which its bytes can be fetched.
   *
   * @param entryName the logical entry name (class path or resource path)
   * @param entryURL  the URL at which the entry's bytes can be read
   */
  public record URLEntry(String entryName, URL entryURL) {

  }

  /**
   * Build-time description of one module on the module path. An explicit module carries the bytes of the
   * {@code module-info.class} that the plugin's JDK selected (for a multi-release jar, the versioned descriptor); an
   * automatic module carries no descriptor bytes and is rebuilt at run time from its name, packages, and the service
   * providers declared in its {@code META-INF/services} files.
   *
   * @param moduleName      the module name
   * @param jarName         the filename of the library jar under {@code META-INF/singularity/lib/}, or {@code null}
   *                        for the application module, whose entries sit at the root of the outer jar
   * @param descriptorBytes the compiled module descriptor, or {@code null} for an automatic module
   * @param multiRelease    whether entries should be resolved through {@code META-INF/versions/}
   * @param packageSet      every package the module contains
   * @param providerMap     for an automatic module, service type to provider class names; empty otherwise
   * @param entryNameSet    every non-directory entry the module contains, versioned entries included
   */
  public record ModuleEntry(String moduleName, String jarName, byte[] descriptorBytes, boolean multiRelease, Set<String> packageSet, Map<String, List<String>> providerMap, Set<String> entryNameSet) implements Serializable {

  }
}
