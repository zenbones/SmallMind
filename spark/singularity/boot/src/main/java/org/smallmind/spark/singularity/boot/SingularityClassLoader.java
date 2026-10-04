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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.lang.module.ModuleFinder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Class loader that resolves classes and resources out of a Singularity bundle. The bundle packages application
 * classes, boot classes, and every runtime dependency (as nested jars under {@code META-INF/singularity/lib/}) into
 * a single outer jar, and is navigated via the serialized {@link SingularityIndex} written at build time.
 * <p>On first touch the loader installs a {@link SingularityJarURLStreamHandlerFactory} so that {@code singularity:}
 * URLs generated from the index can be opened. Classes in certain JDK-shadowed namespaces (XML/W3C, for example) are
 * refused so that platform-provided implementations are used instead.
 * <p>Class path entries are searched in class path order: files laid down at the root of the outer jar (the
 * application's own classes and resources) first, then the bundled library jars in the order the plugin recorded them,
 * which is the project's runtime class path order. A class or single resource comes from the first source that has it;
 * {@link #findResources(String)} returns every source's copy, in the same order. The index is reduced at construction
 * to each name's sources; a URL is formed only when a lookup needs one, and a single-resource lookup keeps the URL it
 * formed so that asking again is a map access.
 * <p>For a bundle built in modular mode the loader also holds the modules recorded in the index. It is the class loader
 * of every one of them, exactly as the JDK's application class loader is both the loader of the module path and of the
 * class path: {@link SingularityEntryPoint} defines a module layer that maps each module to this loader, after which a
 * class in one of their packages is defined into its named module, resources in their packages obey the modules'
 * {@code opens} declarations, and everything else is served from the class path entries as before.
 */
public class SingularityClassLoader extends ClassLoader {

  private static final PermissionCollection ALL_PERMISSION_COLLECTION;
  private static final String[] INOPERABLE_NAMESPACES = new String[] {"jakarta.xml.", "org.xml.", "org.w3c."};
  private static final String[] OPERABLE_NAMESPACES = new String[] {"jakarta.xml.bind."};
  private static final String OUTER_JAR_SOURCE = "";
  private final ConcurrentHashMap<String, URL> resourceURLMap = new ConcurrentHashMap<>();
  private final Map<String, String> sourceMap;
  private final Map<String, List<String>> additionalSourceMap;
  private final String parentJarUrlPart;
  private final Map<String, SingularityModule> moduleMap;
  private final Map<String, SingularityModule> packageModuleMap;
  private final Map<String, List<SingularityModule>> nonPackageModuleMap;
  private final HashSet<String> packageSet = new HashSet<>();
  private final URL sealBase;
  private final String specificationTitle;
  private final String specificationVersion;
  private final String specificationVendor;
  private final String implementationTitle;
  private final String implementationVersion;
  private final String implementationVendor;

  static {

    AllPermission allPermission = new AllPermission();

    ALL_PERMISSION_COLLECTION = allPermission.newPermissionCollection();
    ALL_PERMISSION_COLLECTION.add(allPermission);

    ClassLoader.registerAsParallelCapable();
    URL.setURLStreamHandlerFactory(new SingularityJarURLStreamHandlerFactory());
  }

  /**
   * Reads the Singularity index from the jar and materializes the URL table used to resolve every class and resource
   * that the bundle provides.
   *
   * @param parent         optional parent class loader; {@code null} delegates to the system loader for fallthrough
   * @param manifest       the outer jar's manifest, consulted for package metadata and the optional {@code Sealed} attribute
   * @param jarURL         the URL of the outer jar, used both to anchor generated entry URLs and to seal packages
   * @param jarInputStream an open stream over the outer jar; advanced until the index entry is located and read
   * @throws IOException            if the {@code META-INF/singularity/index/singularity.idx} entry cannot be located or deserialized
   * @throws ClassNotFoundException if deserializing the index references a class that cannot be loaded
   */
  public SingularityClassLoader (ClassLoader parent, Manifest manifest, URL jarURL, JarInputStream jarInputStream)
    throws IOException, ClassNotFoundException {

    super(parent);

    HashMap<String, String> underConstructionSourceMap = new HashMap<>();
    HashMap<String, List<String>> underConstructionAdditionalSourceMap = new HashMap<>();
    HashMap<String, SingularityModule> underConstructionModuleMap = new HashMap<>();
    HashMap<String, SingularityModule> underConstructionPackageModuleMap = new HashMap<>();
    HashMap<String, List<SingularityModule>> underConstructionNonPackageModuleMap = new HashMap<>();
    SingularityIndex singularityIndex = null;
    Attributes mainAttributes = manifest.getMainAttributes();
    JarEntry jarEntry;
    String sealed;

    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
      if (!jarEntry.isDirectory()) {
        if (jarEntry.getName().equals("META-INF/singularity/index/singularity.idx")) {
          try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(jarInputStream.readAllBytes()))) {
            singularityIndex = (SingularityIndex)objectInputStream.readObject();
          }
          break;
        }
      }
    }

    if (singularityIndex == null) {
      throw new IOException("Missing singularity index");
    }

    parentJarUrlPart = jarURL.toExternalForm();

    for (String fileName : singularityIndex.getFileNameIterable()) {
      underConstructionSourceMap.put(fileName, OUTER_JAR_SOURCE);
    }
    for (SingularityIndex.SourceEntry sourceEntry : singularityIndex.getNestedJarSourceIterable()) {
      addClassPathSource(underConstructionSourceMap, underConstructionAdditionalSourceMap, sourceEntry.entryName(), sourceEntry.jarName());
    }
    for (SingularityIndex.ModuleEntry moduleEntry : singularityIndex.getModuleEntryIterable()) {

      SingularityModule singularityModule;

      try {
        singularityModule = new SingularityModule(moduleEntry, jarURL.toExternalForm());
      } catch (URISyntaxException uriSyntaxException) {
        throw new IOException("Unable to locate module(" + moduleEntry.moduleName() + ")", uriSyntaxException);
      }

      underConstructionModuleMap.put(singularityModule.getName(), singularityModule);
      for (String packageName : singularityModule.getModuleDescriptor().packages()) {
        underConstructionPackageModuleMap.put(packageName, singularityModule);
      }
      for (String entryName : singularityModule.listNonPackageEntryNames()) {

        List<SingularityModule> entryModuleList;

        if ((entryModuleList = underConstructionNonPackageModuleMap.get(entryName)) == null) {
          underConstructionNonPackageModuleMap.put(entryName, entryModuleList = new ArrayList<>(1));
        }

        entryModuleList.add(singularityModule);
      }
    }

    specificationTitle = mainAttributes.getValue(Attributes.Name.SPECIFICATION_TITLE);
    specificationVersion = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VERSION);
    specificationVendor = mainAttributes.getValue(Attributes.Name.SPECIFICATION_VENDOR);
    implementationTitle = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
    implementationVersion = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    implementationVendor = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VENDOR);

    if ((sealed = mainAttributes.getValue(Attributes.Name.SEALED)) != null) {
      if (Boolean.parseBoolean(sealed)) {
        sealBase = jarURL;
      } else {
        sealBase = null;
      }
    } else {
      sealBase = null;
    }

    sourceMap = Collections.unmodifiableMap(underConstructionSourceMap);
    additionalSourceMap = Collections.unmodifiableMap(underConstructionAdditionalSourceMap);
    moduleMap = Collections.unmodifiableMap(underConstructionModuleMap);
    packageModuleMap = Collections.unmodifiableMap(underConstructionPackageModuleMap);
    nonPackageModuleMap = Collections.unmodifiableMap(underConstructionNonPackageModuleMap);
  }

  /**
   * Records a class path source of an entry. The first source offered for a name is the one class and single-resource
   * lookups use; later sources are kept, in the order offered, only for {@link #findResources(String)}. Nearly every
   * name has exactly one source, so the common case costs one map entry.
   *
   * @param underConstructionSourceMap           first source of every name
   * @param underConstructionAdditionalSourceMap later sources of the names that have them
   * @param entryName                            the entry's name
   * @param source                               the library jar that supplies it
   */
  private static void addClassPathSource (HashMap<String, String> underConstructionSourceMap, HashMap<String, List<String>> underConstructionAdditionalSourceMap, String entryName, String source) {

    if (underConstructionSourceMap.putIfAbsent(entryName, source) != null) {

      List<String> sourceList;

      if ((sourceList = underConstructionAdditionalSourceMap.get(entryName)) == null) {
        underConstructionAdditionalSourceMap.put(entryName, sourceList = new ArrayList<>(1));
      }

      sourceList.add(source);
    }
  }

  /**
   * @param entryName resource-style entry name without a leading slash
   * @param source    {@link #OUTER_JAR_SOURCE} or the filename of a bundled library jar
   * @return the URL of the entry in that source
   */
  private URL createClassPathURL (String entryName, String source) {

    return OUTER_JAR_SOURCE.equals(source) ? SingularityIndex.createOuterJarURL(parentJarUrlPart, entryName) : SingularityIndex.createNestedJarURL(parentJarUrlPart, source, entryName);
  }

  /**
   * Returns the URL of an entry's first class path source, forming it on first request and keeping it for later ones.
   *
   * @param entryName resource-style entry name without a leading slash
   * @return the URL of the first source in class path order, or {@code null} if no class path source has the entry
   */
  private URL getClassPathURL (String entryName) {

    URL url;
    String source;

    if ((url = resourceURLMap.get(entryName)) == null) {
      if ((source = sourceMap.get(entryName)) == null) {

        return null;
      }

      resourceURLMap.putIfAbsent(entryName, url = createClassPathURL(entryName, source));
    }

    return url;
  }

  /**
   * Adds the URLs of an entry's later class path sources (every source after the first) to a list, in class path
   * order, without keeping them.
   *
   * @param entryName resource-style entry name without a leading slash
   * @param urlList   the list to add to
   */
  private void addAdditionalClassPathURLs (String entryName, List<URL> urlList) {

    List<String> additionalSourceList;

    if ((additionalSourceList = additionalSourceMap.get(entryName)) != null) {
      for (String additionalSource : additionalSourceList) {
        urlList.add(createClassPathURL(entryName, additionalSource));
      }
    }
  }

  /**
   * Finds the first visible copy of a resource among the bundle's modules, routing by package: a name in a module
   * package can only come from that module, and a name outside every module package comes from the modules indexed
   * under it.
   *
   * @param entryName resource-style entry name without a leading slash
   * @return the URL of the first visible module copy, or {@code null} if no module supplies a visible copy
   */
  private URL findModuleURL (String entryName) {

    String packageName = SingularityModule.toPackageName(entryName);
    SingularityModule packageModule;
    List<SingularityModule> entryModuleList;
    URL url;

    if ((packageModule = packageModuleMap.get(packageName)) != null) {

      return (((url = packageModule.getEntryURL(entryName)) != null) && packageModule.isVisible(entryName, packageName)) ? url : null;
    } else if ((entryModuleList = nonPackageModuleMap.get(entryName)) != null) {

      return entryModuleList.get(0).getEntryURL(entryName);
    }

    return null;
  }

  /**
   * Adds every visible copy of a resource held by the bundle's modules to a list, routed as in
   * {@link #findModuleURL(String)}.
   *
   * @param entryName resource-style entry name without a leading slash
   * @param urlList   the list to add to
   */
  private void addModuleURLs (String entryName, List<URL> urlList) {

    String packageName = SingularityModule.toPackageName(entryName);
    SingularityModule packageModule;
    List<SingularityModule> entryModuleList;
    URL url;

    if ((packageModule = packageModuleMap.get(packageName)) != null) {
      if (((url = packageModule.getEntryURL(entryName)) != null) && packageModule.isVisible(entryName, packageName)) {
        urlList.add(url);
      }
    } else if ((entryModuleList = nonPackageModuleMap.get(entryName)) != null) {
      for (SingularityModule singularityModule : entryModuleList) {
        urlList.add(singularityModule.getEntryURL(entryName));
      }
    }
  }

  /**
   * Reports whether the bundle was built in modular mode, in which case a module layer must be defined over
   * {@link #getModuleFinder()} before any class is loaded from it.
   *
   * @return {@code true} if the index records at least one module
   */
  public boolean isModular () {

    return !moduleMap.isEmpty();
  }

  /**
   * Returns a finder over the modules recorded in the bundle's index. Every module it finds must be mapped to this
   * loader when the layer is defined.
   *
   * @return a module finder over this bundle's module path, empty for a bundle built in class path mode
   */
  public ModuleFinder getModuleFinder () {

    return new SingularityModuleFinder(moduleMap.values());
  }

  /**
   * Resolves a class, preferring bundled definitions and falling back to the parent loader (or the system loader
   * when no parent is configured).
   *
   * @param name    fully qualified binary class name
   * @param resolve {@code true} to link the class after loading
   * @return the loaded {@link Class}
   * @throws ClassNotFoundException if no definition is available from this loader, its parent, or the system loader
   */
  @Override
  protected synchronized Class<?> loadClass (String name, boolean resolve)
    throws ClassNotFoundException {

    Class<?> singularityClass;

    if ((singularityClass = findLoadedClass(name)) == null) {
      try {
        singularityClass = findClass(name);
      } catch (ClassNotFoundException c) {
        if (getParent() != null) {
          singularityClass = getParent().loadClass(name);
        } else {
          singularityClass = findSystemClass(name);
        }
      }
    }

    if (resolve) {
      resolveClass(singularityClass);
    }

    return singularityClass;
  }

  /**
   * Reads the class bytes from the bundle and defines the class, taking care to construct a {@link CodeSource} whose
   * URL survives the quirks of {@code javax.crypto.JarVerifier} when the entry lives behind a {@code singularity:}
   * URL.
   *
   * @param name fully qualified binary class name
   * @return the newly defined class
   * @throws ClassNotFoundException if the class is not mapped by this loader, lives in a reserved namespace, or
   *                                cannot be read, deserialized, or defined for any reason
   */
  @Override
  protected Class<?> findClass (String name)
    throws ClassNotFoundException {

    SingularityModule packageModule;

    if ((!packageModuleMap.isEmpty()) && ((packageModule = packageModuleMap.get(getPackageName(name))) != null)) {

      Class<?> moduleClass;

      try {
        moduleClass = defineModuleClass(packageModule, name);
      } catch (Exception exception) {
        throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ") in module(" + packageModule.getName() + ")", exception);
      }

      if (moduleClass == null) {
        throw new ClassNotFoundException(name);
      }

      return moduleClass;
    }

    if (isOperableNamespace(name)) {

      String classEntryName = name.replace('.', '/') + ".class";
      String classSource;
      URL classURL;
      URL codeSourceUrl;

      if ((classSource = sourceMap.get(classEntryName)) != null) {
        try {

          String classURLExternalForm;

          classURL = createClassPathURL(classEntryName, classSource);
          classURLExternalForm = classURL.toExternalForm();

          switch (classURL.getProtocol()) {
            case "jar":

              int jarBangSlashIndex;

              if ((jarBangSlashIndex = classURLExternalForm.indexOf("!/")) < 0) {
                codeSourceUrl = URI.create(classURLExternalForm + "!/").toURL();
              } else {
                codeSourceUrl = URI.create(classURLExternalForm.substring(0, jarBangSlashIndex + 2)).toURL();
              }
              break;
            case "singularity":
              // javax.crypto.JarVerifier will encase this URL in 'jar:<code source url>!/ (it incorrectly assumes any protocol not 'jar:' is 'file:'),
              // which then gets stripped again by JarUrlConnection, which will let SingularityJarURLConnection respond correctly...
              codeSourceUrl = URI.create(classURLExternalForm.substring(0, classURLExternalForm.indexOf("!/"))).toURL();
              break;
            default:
              throw new MalformedURLException("Unknown class url protocol(" + classURL.getProtocol() + ")");
          }

          CodeSource codeSource = new CodeSource(codeSourceUrl, (Certificate[])null);
          ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, ALL_PERMISSION_COLLECTION, this, null);
          InputStream classInputStream;
          byte[] classData;

          classInputStream = classURL.openStream();
          classData = getClassData(classInputStream);
          classInputStream.close();

          definePackage(name);

          return defineClass(name, classData, 0, classData.length, protectionDomain);
        } catch (Exception exception) {
          throw new ClassNotFoundException("Exception encountered while attempting to define class (" + name + ")", exception);
        }
      }
    }

    throw new ClassNotFoundException(name);
  }

  /**
   * Finds a class in one of the bundle's modules, as used by {@code Class.forName(Module, String)} and by
   * {@code ServiceLoader} when it instantiates providers. A {@code null} module name searches the class path entries.
   *
   * @param moduleName the module's name, or {@code null} for the class path
   * @param name       fully qualified binary class name
   * @return the class, or {@code null} if the module does not contain it
   * @throws UncheckedIOException if the module contains the class but its bytes cannot be read
   */
  @Override
  protected synchronized Class<?> findClass (String moduleName, String name) {

    SingularityModule singularityModule;
    Class<?> moduleClass;

    if (moduleName == null) {
      try {
        return findClass(name);
      } catch (ClassNotFoundException classNotFoundException) {
        return null;
      }
    }

    if (((singularityModule = moduleMap.get(moduleName)) == null) || (!singularityModule.containsPackage(getPackageName(name)))) {

      return null;
    }

    if ((moduleClass = findLoadedClass(name)) != null) {

      return moduleClass;
    }

    try {
      return defineModuleClass(singularityModule, name);
    } catch (IOException ioException) {
      throw new UncheckedIOException("Unable to define class(" + name + ") in module(" + moduleName + ")", ioException);
    }
  }

  /**
   * Defines a class from one of the bundle's modules. Because the module layer maps the class's package to this
   * loader, the class becomes a member of that named module; its code source is the module's location.
   *
   * @param singularityModule the module that owns the class's package
   * @param name              fully qualified binary class name
   * @return the defined class, or {@code null} if the module has no such class file
   * @throws IOException if the class bytes cannot be read
   */
  private Class<?> defineModuleClass (SingularityModule singularityModule, String name)
    throws IOException {

    URL classURL;
    ProtectionDomain protectionDomain;
    byte[] classData;

    if ((classURL = singularityModule.getEntryURL(name.replace('.', '/') + ".class")) == null) {

      return null;
    }

    protectionDomain = new ProtectionDomain(new CodeSource(singularityModule.getLocation().toURL(), (Certificate[])null), ALL_PERMISSION_COLLECTION, this, null);

    try (InputStream classInputStream = classURL.openStream()) {
      classData = getClassData(classInputStream);
    }

    return defineClass(name, classData, 0, classData.length, protectionDomain);
  }

  /**
   * @param className fully qualified binary class name
   * @return the name of the class's package, or the empty string for the unnamed package
   */
  private String getPackageName (String className) {

    int lastDotPos;

    return ((lastDotPos = className.lastIndexOf('.')) < 0) ? "" : className.substring(0, lastDotPos);
  }

  /**
   * Decides whether a class name falls within the subset this loader is allowed to resolve.
   * <p>Names matching an entry in the allow list are admitted unconditionally; names matching an entry in the deny
   * list are refused so that the platform's own copy is used; everything else is admitted.
   *
   * @param name fully qualified binary class name
   * @return {@code true} if this loader is willing to define the class, {@code false} if it must defer
   */
  private boolean isOperableNamespace (String name) {

    for (String operableNamespace : OPERABLE_NAMESPACES) {
      if (name.startsWith(operableNamespace)) {

        return true;
      }
    }
    for (String inoperableNamespace : INOPERABLE_NAMESPACES) {
      if (name.startsWith(inoperableNamespace)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Defines the enclosing package the first time a class from that package is loaded, applying specification and
   * implementation metadata from the outer jar's manifest and the optional seal base.
   *
   * @param name fully qualified binary class name; its enclosing package is the text preceding the last dot
   */
  private synchronized void definePackage (String name) {

    String packageName;
    int lastDotPos = name.lastIndexOf('.');

    packageName = name.substring(0, lastDotPos);
    if (packageSet.add(packageName)) {
      definePackage(packageName, specificationTitle, specificationVersion, specificationVendor, implementationTitle, implementationVersion, implementationVendor, sealBase);
    }
  }

  /**
   * Slurps a stream of class bytes into a single byte array.
   *
   * @param classInputStream stream over the raw class file bytes
   * @return every byte read from the stream, ready to hand to {@link ClassLoader#defineClass}
   * @throws IOException if reading from the stream fails
   */
  private byte[] getClassData (InputStream classInputStream)
    throws IOException {

    return classInputStream.readAllBytes();
  }

  /**
   * Looks up a single resource, normalizing an optional leading slash. A resource in a package of one of the bundle's
   * modules is returned only if the module leaves it visible (see {@link SingularityModule#isVisible(String)}); a
   * resource outside every module package is searched in the modules first and then in the class path entries.
   *
   * @param name resource name, with or without a leading {@code '/'}
   * @return the URL of the resource, or {@code null} if the name is empty, unknown, or encapsulated
   */
  @Override
  protected URL findResource (String name) {

    if ((name == null) || name.isEmpty()) {

      return null;
    } else {

      String entryName = (name.charAt(0) == '/') ? name.substring(1) : name;
      URL moduleURL;

      if ((!moduleMap.isEmpty()) && ((moduleURL = findModuleURL(entryName)) != null)) {

        return moduleURL;
      }

      return getClassPathURL(entryName);
    }
  }

  /**
   * Looks up a resource inside a named module, as used by {@code Module.getResourceAsStream} once that method has
   * applied the module's encapsulation rules for the caller. A {@code null} module name searches the class path entries.
   *
   * @param moduleName the module's name, or {@code null} for the class path
   * @param name       resource name
   * @return the URL of the resource, or {@code null} if the module does not contain it
   */
  @Override
  protected URL findResource (String moduleName, String name) {

    SingularityModule singularityModule;

    if ((name == null) || name.isEmpty()) {

      return null;
    }

    if (moduleName == null) {

      return getClassPathURL((name.charAt(0) == '/') ? name.substring(1) : name);
    }

    if ((singularityModule = moduleMap.get(moduleName)) == null) {

      return null;
    }

    return singularityModule.getEntryURL((name.charAt(0) == '/') ? name.substring(1) : name);
  }

  /**
   * Enumerates matching resources: the visible copies held by the bundle's modules, then every class path source's
   * copy in class path order. For names ending in a slash this method behaves as a directory listing: every indexed
   * file that begins with the given prefix (excluding directory placeholders) is returned, once per source.
   *
   * @param name resource name or directory-style prefix
   * @return an {@link Enumeration} of matching URLs, possibly empty
   */
  @Override
  protected Enumeration<URL> findResources (String name) {

    if ((name == null) || name.isEmpty()) {

      return Collections.emptyEnumeration();
    } else if (!name.endsWith("/")) {

      String entryName = (name.charAt(0) == '/') ? name.substring(1) : name;
      ArrayList<URL> urlList;
      URL url;

      if (moduleMap.isEmpty() && (!additionalSourceMap.containsKey(entryName))) {

        return ((url = getClassPathURL(entryName)) == null) ? Collections.emptyEnumeration() : new ArrayEnumeration<>(new URL[] {url});
      }

      urlList = new ArrayList<>();
      addModuleURLs(entryName, urlList);
      if ((url = getClassPathURL(entryName)) != null) {
        urlList.add(url);
        addAdditionalClassPathURLs(entryName, urlList);
      }

      return asEnumeration(urlList);
    } else {

      ArrayList<URL> urlList = new ArrayList<>();

      for (SingularityModule singularityModule : moduleMap.values()) {
        singularityModule.streamEntryNames().filter((entryName) -> entryName.startsWith(name) && singularityModule.isVisible(entryName)).forEach((entryName) -> urlList.add(singularityModule.getEntryURL(entryName)));
      }

      // a listing can span the whole bundle, so its URLs are formed without being kept
      for (Map.Entry<String, String> sourceEntry : sourceMap.entrySet()) {
        if (sourceEntry.getKey().startsWith(name) && (!sourceEntry.getKey().endsWith("/"))) {
          urlList.add(createClassPathURL(sourceEntry.getKey(), sourceEntry.getValue()));
          addAdditionalClassPathURLs(sourceEntry.getKey(), urlList);
        }
      }

      return asEnumeration(urlList);
    }
  }

  /**
   * @param urlList the URLs to expose
   * @return an enumeration over the list, or the shared empty enumeration when the list is empty
   */
  private Enumeration<URL> asEnumeration (List<URL> urlList) {

    if (urlList.isEmpty()) {

      return Collections.emptyEnumeration();
    } else {

      URL[] urls = new URL[urlList.size()];

      urlList.toArray(urls);

      return new ArrayEnumeration<>(urls);
    }
  }

  /**
   * {@link Enumeration} view over a fixed-size array.
   *
   * @param <T> the element type
   */
  private static class ArrayEnumeration<T> implements Enumeration<T> {

    private final T[] values;
    private int index = 0;

    /**
     * Wraps the supplied array; the array is not copied, so callers must not mutate it afterward.
     *
     * @param values backing array whose elements will be returned in order
     */
    private ArrayEnumeration (T[] values) {

      this.values = values;
    }

    /**
     * Reports whether additional elements remain.
     *
     * @return {@code true} while the cursor has not yet reached the end of the array
     */
    @Override
    public boolean hasMoreElements () {

      return index < values.length;
    }

    /**
     * Advances the cursor and returns the next element.
     *
     * @return the next array element in order
     * @throws NoSuchElementException once every element has been returned
     */
    @Override
    public T nextElement () {

      if (index >= values.length) {
        throw new NoSuchElementException();
      }

      return values[index++];
    }
  }
}
