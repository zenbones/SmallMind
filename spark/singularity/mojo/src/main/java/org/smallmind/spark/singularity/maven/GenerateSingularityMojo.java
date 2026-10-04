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
package org.smallmind.spark.singularity.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.module.Configuration;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.lang.module.ResolvedModule;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.zip.CompressionType;
import org.smallmind.spark.singularity.boot.SingularityEntryPoint;
import org.smallmind.spark.singularity.boot.SingularityIndex;

/**
 * Mojo goal {@code generate-singularity} that assembles a single executable jar by staging boot classes, runtime
 * dependencies (as untouched nested jars), and the project's own classes beneath a build directory, then compressing
 * the whole tree into a jar with a manifest that declares {@link SingularityEntryPoint} as the {@code Main-Class}.
 * <p>A {@link SingularityIndex} written to {@code META-INF/singularity/index/singularity.idx} records every entry's
 * provenance so that {@code SingularityClassLoader} can resolve classes and resources at run time.
 * <p>With {@code modular} set, the project must be a named module. Its module graph is resolved here, at build time,
 * from the project's {@code module-info.class} over the runtime dependencies, exactly as {@code java} would resolve a
 * module path: explicit modules, the automatic modules reachable from them through {@code requires}, and any service
 * providers bound by {@code uses}. The resolved modules form the bundle's module path and every other dependency stays
 * on its class path. Resolution failures, duplicate module names, and split packages fail the build rather than the
 * launch.
 * <p>{@code useJarCache} (default {@code true}) is written to the manifest as {@code Singularity-Jar-Cache} and decides
 * how the bundle holds a nested jar in memory once something in it is needed: as the jar's own bytes, decompressing
 * only the entries read (faster), or, when {@code false}, drained through a {@code JarInputStream} with every entry
 * re-compressed (slower to build, slightly less memory).
 */
@Mojo(name = "generate-singularity", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateSingularityMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private Exclusion[] exclusions;
  @Parameter(defaultValue = "singularity")
  private String singularityBuildDir;
  @Parameter
  private String mainClass;
  @Parameter(defaultValue = "false")
  private boolean verbose;
  @Parameter(defaultValue = "false")
  private boolean skip;
  @Parameter(defaultValue = "false")
  private boolean modular;
  @Parameter(defaultValue = "true")
  private boolean useJarCache = true;
  @Component
  ArtifactFactory artifactFactory;
  @Parameter(readonly = true, property = "plugin.artifacts")
  protected List<Artifact> pluginArtifacts;

  /**
   * Builds the Singularity bundle end-to-end: extracts boot classes from this plugin's own dependency, stages every
   * non-excluded runtime dependency jar, walks the project's {@code classes} output directory, writes the index, and
   * finally compresses everything into the output jar. The resulting jar is attached to the project so subsequent
   * lifecycle phases see it.
   *
   * @throws MojoExecutionException if the build directory cannot be created, the boot classes cannot be located,
   *                                any dependency or class copy fails, the index cannot be serialized, or the final jar cannot be written
   */
  public void execute ()
    throws MojoExecutionException {

    if (!skip) {

      Artifact applicationArtifact;
      SingularityIndex singularityIndex = new SingularityIndex();
      Path buildPath;
      Path libraryPath;
      Path indexPath;
      Path classesPath;
      Path compressedFile;
      Set<String> applicationEntryNameSet = Set.of();
      String mainModule = null;
      boolean bootClassesFound = false;

      try {
        Files.createDirectories(buildPath = Paths.get(project.getBuild().getDirectory(), singularityBuildDir));
        Files.createDirectories(libraryPath = buildPath.resolve("META-INF").resolve("singularity").resolve("lib"));
        Files.createDirectories(indexPath = buildPath.resolve("META-INF").resolve("singularity").resolve("index"));
      } catch (IOException ioException) {
        throw new MojoExecutionException("Unable to create a build directory", ioException);
      }

      for (Artifact pluginArtifact : pluginArtifacts) {
        if (pluginArtifact.getGroupId().equals("org.smallmind") && pluginArtifact.getArtifactId().equals("spark-singularity-boot")) {
          try {
            copyBootClasses(singularityIndex, pluginArtifact.getFile(), buildPath);
          } catch (IOException ioException) {
            throw new MojoExecutionException("Problem in copying boot classes into the build directory", ioException);
          }

          bootClassesFound = true;
          break;
        }
      }
      if (!bootClassesFound) {
        throw new MojoExecutionException("Unable to locate the boot class dependencies");
      }

      LinkedList<Artifact> includedArtifactList = new LinkedList<>();

      for (Artifact artifact : project.getRuntimeArtifacts()) {

        boolean excluded = false;

        if ((exclusions != null) && (exclusions.length > 0)) {
          for (Exclusion exclusion : exclusions) {
            if (exclusion.matchesArtifact(artifact)) {
              excluded = true;
              break;
            }
          }
        }

        if (excluded) {
          if (verbose) {
            getLog().info(String.format("Excluded dependency(%s)...", artifact.getFile().getName()));
          }
        } else {
          if (verbose) {
            getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
          }

          try {
            if (!modular) {
              indexClassPathJar(singularityIndex, artifact);
            }

            copyToDestination(artifact.getFile(), libraryPath.resolve(artifact.getFile().getName()));
          } catch (IOException ioException) {
            throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the build directory", artifact), ioException);
          }

          includedArtifactList.add(artifact);
        }
      }

      classesPath = Paths.get(project.getBuild().getDirectory(), "classes");
      if (Files.exists(classesPath)) {
        if (verbose) {
          getLog().info("Copying classes directory...");
        }
        try {

          CopyFileVisitor copyFileVisitor = new CopyFileVisitor(modular ? null : singularityIndex, buildPath);

          Files.walkFileTree(classesPath, copyFileVisitor);
          applicationEntryNameSet = copyFileVisitor.getCopiedNameSet();
        } catch (IOException ioException) {
          throw new MojoExecutionException("Unable to copy the classes directory into the build path", ioException);
        }
      }

      if (modular) {
        mainModule = indexModulePath(singularityIndex, classesPath, applicationEntryNameSet, includedArtifactList);
      }

      if (verbose) {
        getLog().info("Creating singularity index...");
      }
      try {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(indexPath.resolve("singularity.idx").toFile()))) {
          objectOutputStream.writeObject(singularityIndex);
        }
      } catch (IOException ioException) {
        throw new MojoExecutionException("Unable to write the singularity index", ioException);
      }

      try {

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();

        attributes.put(Attributes.Name.MAIN_CLASS, SingularityEntryPoint.class.getName());
        attributes.put(new Attributes.Name("Singularity-Class"), mainClass);
        if (mainModule != null) {
          attributes.put(new Attributes.Name("Singularity-Module"), mainModule);
        }
        attributes.put(new Attributes.Name("Singularity-Jar-Cache"), Boolean.toString(useJarCache));

        attributes.put(Attributes.Name.SPECIFICATION_TITLE, System.getProperty("java.vm.specification.name"));
        attributes.put(Attributes.Name.SPECIFICATION_VERSION, System.getProperty("java.vm.specification.version"));
        attributes.put(Attributes.Name.SPECIFICATION_VENDOR, System.getProperty("java.vm.specification.vendor"));
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, System.getProperty("java.specification.name"));
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, System.getProperty("java.specification.version"));
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, System.getProperty("java.specification.vendor"));

        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        if (verbose) {
          getLog().info("Compressing output jar...");
        }

        CompressionType.JAR.compress(buildPath, compressedFile = Paths.get(project.getBuild().getDirectory(), constructArtifactName()), manifest);
      } catch (IOException ioException) {
        throw new MojoExecutionException("Problem constructing the executable jar", ioException);
      }

      applicationArtifact = artifactFactory.createArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(), "compile", "jar");
      applicationArtifact.setFile(compressedFile.toFile());

      project.addAttachedArtifact(applicationArtifact);
    }
  }

  /**
   * Records every entry of a dependency jar as a class path entry served from that nested jar.
   *
   * @param singularityIndex the index under construction
   * @param artifact         the dependency whose jar is being bundled
   * @throws IOException if the jar cannot be read
   */
  private void indexClassPathJar (SingularityIndex singularityIndex, Artifact artifact)
    throws IOException {

    try (JarFile jarFile = new JarFile(artifact.getFile())) {

      Enumeration<JarEntry> jarEntryEnum = jarFile.entries();
      // one shared instance per jar lets serialization write back-references instead of a copy per entry
      String jarName = artifact.getFile().getName();

      while (jarEntryEnum.hasMoreElements()) {
        singularityIndex.addInverseJarEntry(jarEntryEnum.nextElement().getName(), jarName);
      }
    }
  }

  /**
   * Resolves the bundle's module graph from the project's own module and records the result in the index: one module
   * entry per resolved module, and the remaining dependencies as class path jars.
   * <p>The candidates are the project module, every dependency with an explicit module descriptor, and the automatic
   * modules (dependencies without a descriptor) that an explicit candidate requires without {@code static}. Automatic
   * modules nobody requires are left out of the candidate set on purpose: resolving one automatic module resolves every
   * automatic module the finder can see, which would drag the whole class path onto the module path. The configuration
   * is resolved with service binding and then instantiated once in a throwaway layer so that split packages are caught
   * here.
   *
   * @param singularityIndex        the index under construction
   * @param classesPath             the project's compiled classes, which must contain {@code module-info.class}
   * @param applicationEntryNameSet the entries copied from the classes directory into the root of the bundle
   * @param artifactList            the non-excluded runtime dependencies, already copied into the bundle
   * @return the name of the project's module, which becomes the bundle's main module
   * @throws MojoExecutionException if the project is not a named module, two dependencies supply the same selected
   *                                module, the module graph cannot be resolved or instantiated, or a jar cannot be read
   */
  private String indexModulePath (SingularityIndex singularityIndex, Path classesPath, Set<String> applicationEntryNameSet, List<Artifact> artifactList)
    throws MojoExecutionException {

    HashMap<String, ModuleReference> referenceMap = new HashMap<>();
    HashMap<String, List<Artifact>> moduleArtifactMap = new HashMap<>();
    HashSet<String> candidateNameSet = new HashSet<>();
    HashSet<Artifact> modulePathArtifactSet = new HashSet<>();
    LinkedList<ModuleDescriptor> pendingDescriptorList = new LinkedList<>();
    LinkedList<Path> candidatePathList = new LinkedList<>();
    ModuleReference applicationReference;
    Configuration configuration;
    String applicationModuleName;

    if (!Files.isRegularFile(classesPath.resolve("module-info.class"))) {
      throw new MojoExecutionException(String.format("Modular packaging requires the project to be a named module, but no module descriptor was found(%s)", classesPath.resolve("module-info.class")));
    }

    applicationReference = ModuleFinder.of(classesPath).findAll().iterator().next();
    applicationModuleName = applicationReference.descriptor().name();
    pendingDescriptorList.add(applicationReference.descriptor());
    candidatePathList.add(classesPath);

    for (Artifact artifact : artifactList) {

      Set<ModuleReference> artifactReferenceSet;

      try {
        artifactReferenceSet = ModuleFinder.of(artifact.getFile().toPath()).findAll();
      } catch (FindException findException) {
        getLog().info(String.format("Dependency(%s) cannot be a module and stays on the class path: %s", artifact.getFile().getName(), findException.getMessage()));
        continue;
      }

      for (ModuleReference artifactReference : artifactReferenceSet) {

        String moduleName = artifactReference.descriptor().name();

        referenceMap.putIfAbsent(moduleName, artifactReference);
        moduleArtifactMap.computeIfAbsent(moduleName, (key) -> new LinkedList<>()).add(artifact);
        if (!artifactReference.descriptor().isAutomatic()) {
          if (candidateNameSet.add(moduleName)) {
            pendingDescriptorList.add(artifactReference.descriptor());
          }
        }
      }
    }

    while (!pendingDescriptorList.isEmpty()) {
      for (ModuleDescriptor.Requires requires : pendingDescriptorList.removeFirst().requires()) {
        if (!requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC)) {

          ModuleReference requiredReference;

          if (((requiredReference = referenceMap.get(requires.name())) != null) && requiredReference.descriptor().isAutomatic()) {
            candidateNameSet.add(requires.name());
          }
        }
      }
    }

    for (String candidateName : candidateNameSet) {
      if (candidateName.equals(applicationModuleName)) {
        throw new MojoExecutionException(String.format("Dependency(%s) declares the project's own module name(%s)", moduleArtifactMap.get(candidateName).get(0).getFile().getName(), candidateName));
      }
      if (moduleArtifactMap.get(candidateName).size() > 1) {
        throw new MojoExecutionException(String.format("Module(%s) is supplied by more than one dependency(%s, %s)", candidateName, moduleArtifactMap.get(candidateName).get(0).getFile().getName(), moduleArtifactMap.get(candidateName).get(1).getFile().getName()));
      }

      candidatePathList.add(moduleArtifactMap.get(candidateName).get(0).getFile().toPath());
    }

    try {
      configuration = ModuleLayer.boot().configuration().resolveAndBind(ModuleFinder.of(candidatePathList.toArray(new Path[0])), ModuleFinder.of(), Set.of(applicationModuleName));
      ModuleLayer.boot().defineModulesWithOneLoader(configuration, ClassLoader.getPlatformClassLoader());
    } catch (FindException | ResolutionException | LayerInstantiationException exception) {
      throw new MojoExecutionException(String.format("Unable to resolve the module path of module(%s): %s", applicationModuleName, exception.getMessage()), exception);
    }

    try {
      for (ResolvedModule resolvedModule : configuration.modules()) {
        if (resolvedModule.name().equals(applicationModuleName)) {
          singularityIndex.addModuleEntry(new SingularityIndex.ModuleEntry(applicationModuleName, null, Files.readAllBytes(classesPath.resolve("module-info.class")), false, new HashSet<>(applicationReference.descriptor().packages()), new HashMap<>(), new HashSet<>(applicationEntryNameSet)));
        } else {

          Artifact moduleArtifact = moduleArtifactMap.get(resolvedModule.name()).get(0);

          modulePathArtifactSet.add(moduleArtifact);
          singularityIndex.addModuleEntry(createModuleEntry(resolvedModule.reference(), moduleArtifact));
        }

        getLog().info(String.format("Module path: %s%s", resolvedModule.name(), resolvedModule.reference().descriptor().isAutomatic() ? " (automatic)" : ""));
      }

      for (Artifact artifact : artifactList) {
        if (!modulePathArtifactSet.contains(artifact)) {
          indexClassPathJar(singularityIndex, artifact);
          getLog().info(String.format("Class path: %s", artifact.getFile().getName()));
        }
      }
    } catch (IOException ioException) {
      throw new MojoExecutionException("Unable to index the module path", ioException);
    }

    return applicationModuleName;
  }

  /**
   * Describes a resolved dependency module for the index. An explicit module keeps the bytes of the descriptor the
   * JDK selected (for a multi-release jar, the versioned one); an automatic module keeps the service providers the JDK
   * derived from its {@code META-INF/services} files.
   *
   * @param moduleReference the resolved module
   * @param artifact        the dependency that supplies it
   * @return the module's index entry
   * @throws IOException if the jar or its descriptor cannot be read
   */
  private SingularityIndex.ModuleEntry createModuleEntry (ModuleReference moduleReference, Artifact artifact)
    throws IOException {

    ModuleDescriptor moduleDescriptor = moduleReference.descriptor();
    HashMap<String, List<String>> providerMap = new HashMap<>();
    HashSet<String> entryNameSet = new HashSet<>();
    byte[] descriptorBytes = null;
    boolean multiRelease;

    try (JarFile jarFile = new JarFile(artifact.getFile())) {

      Enumeration<JarEntry> jarEntryEnum = jarFile.entries();

      while (jarEntryEnum.hasMoreElements()) {

        JarEntry jarEntry = jarEntryEnum.nextElement();

        if (!jarEntry.isDirectory()) {
          entryNameSet.add(jarEntry.getName());
        }
      }

      multiRelease = (jarFile.getManifest() != null) && Boolean.parseBoolean(jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE));
    }

    if (moduleDescriptor.isAutomatic()) {
      for (ModuleDescriptor.Provides provides : moduleDescriptor.provides()) {
        providerMap.put(provides.service(), new ArrayList<>(provides.providers()));
      }
    } else {
      try (ModuleReader moduleReader = moduleReference.open()) {

        Optional<InputStream> descriptorStream = moduleReader.open("module-info.class");

        if (descriptorStream.isPresent()) {
          try (InputStream inputStream = descriptorStream.get()) {
            descriptorBytes = inputStream.readAllBytes();
          }
        }
      }

      if (descriptorBytes == null) {
        throw new IOException(String.format("Unable to read the module descriptor of module(%s) from dependency(%s)", moduleDescriptor.name(), artifact.getFile().getName()));
      }
    }

    return new SingularityIndex.ModuleEntry(moduleDescriptor.name(), artifact.getFile().getName(), descriptorBytes, multiRelease, new HashSet<>(moduleDescriptor.packages()), providerMap, entryNameSet);
  }

  /**
   * Constructs the canonical output jar filename for the current project, using the project's artifactId, version,
   * and optional classifier.
   *
   * @return filename in the form {@code artifactId-version[-classifier].jar}
   */
  private String constructArtifactName () {

    StringBuilder nameBuilder = new StringBuilder(project.getArtifactId()).append('-').append(project.getVersion());

    if (project.getArtifact().getClassifier() != null) {
      nameBuilder.append('-').append(project.getArtifact().getClassifier());
    }

    return nameBuilder.append(".jar").toString();
  }

  /**
   * Transfers a file to a destination path using NIO channels, looping until the entire source has been written.
   *
   * @param file            source file to read
   * @param destinationPath destination path to write; an existing file is overwritten
   * @throws IOException if either stream cannot be opened or bytes cannot be transferred
   */
  private void copyToDestination (File file, Path destinationPath)
    throws IOException {

    FileInputStream inputStream;
    FileOutputStream outputStream;
    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = (inputStream = new FileInputStream(file)).getChannel();
    writeChannel = (outputStream = new FileOutputStream(destinationPath.toFile())).getChannel();
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    outputStream.close();
    inputStream.close();
  }

  /**
   * Extracts every class under {@code org/smallmind/spark/singularity/boot/} from the plugin's boot jar into the
   * Singularity build tree so they become the public classes of the generated outer jar, recording each in the index.
   *
   * @param singularityIndex index that should be updated with every boot class extracted
   * @param jarFile          jar containing the precompiled boot classes (the {@code spark-singularity-boot} plugin dependency)
   * @param destinationPath  root of the Singularity build tree into which the classes are laid down
   * @throws IOException if the jar cannot be opened, a directory cannot be created, or bytes cannot be written
   */
  private void copyBootClasses (SingularityIndex singularityIndex, File jarFile, Path destinationPath)
    throws IOException {

    byte[] buffer = new byte[1024];

    try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {

      JarEntry jarEntry;

      while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
        if ((!jarEntry.isDirectory()) && jarEntry.getName().startsWith("org/smallmind/spark/singularity/boot/")) {
          if (verbose) {
            getLog().info(String.format("Copying boot class(%s)...", jarEntry.getName()));
          }

          Files.createDirectories(destinationPath.resolve(jarEntry.getName().substring(0, jarEntry.getName().lastIndexOf('/'))));

          try (OutputStream outputStream = Files.newOutputStream(destinationPath.resolve(jarEntry.getName()), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            int bytesRead;

            // the entry size is unknown (-1) when the jar was written with data descriptors, so read to the end of the entry
            while ((bytesRead = jarInputStream.read(buffer)) >= 0) {
              outputStream.write(buffer, 0, bytesRead);
            }
          }

          singularityIndex.addFileName(jarEntry.getName());
        }
      }
    }
  }
}
