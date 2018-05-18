package org.smallmind.web.json.dto.maven;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

// Annotation processor for generating dto source
@Mojo(name = "generate-dtos", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DtoAptMojo extends AbstractMojo {

  private static final HashSet<String> outputDirectorySet = new HashSet<>();

  @Parameter(readonly = true, property = "project")
  private MavenProject project;

  @Override
  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    if (outputDirectorySet.add(project.getBuild().getOutputDirectory())) {

      JavaCompiler javaCompiler;
      VirtualClassLoader classLoader;

      if ((javaCompiler = ToolProvider.getSystemJavaCompiler()) == null) {
        throw new MojoFailureException("Unable to acquire the java compiler toolchain");
      } else {

        LinkedList<JavaFileObject> compilationUnitList = new LinkedList<>();
        LinkedList<URL> dependencyURLList = new LinkedList<>();
        LinkedList<String> compilerOptions = new LinkedList<>();
        Path sourcePath;
        URL[] dependencyURLs;

        compilerOptions.add("-implicit:none");
        compilerOptions.add("-proc:none");
        compilerOptions.add("-classpath");

        StringBuilder sb = new StringBuilder(Paths.get(project.getBuild().getOutputDirectory()).resolveSibling("generated-sources").resolve("java").toAbsolutePath().toString());
        for (Artifact artifact : project.getArtifacts()) {
          if (artifact.getArtifactHandler().isAddedToClasspath()) {
            sb.append(";");
            sb.append(artifact.getFile().getAbsolutePath());

            try {
              dependencyURLList.add(artifact.getFile().toURL());
            } catch (MalformedURLException malformedURLException) {
              throw new MojoFailureException("Unable to process dependency", malformedURLException);
            }
          }
        }
        compilerOptions.add(sb.toString());

        dependencyURLs = new URL[dependencyURLList.size()];
        dependencyURLList.toArray(dependencyURLs);
        classLoader = new VirtualClassLoader(dependencyURLs);

        try {
          Files.walkFileTree(sourcePath = Paths.get(project.getBuild().getSourceDirectory()), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {

              if (file.getFileName().toString().endsWith(".java")) {
                compilationUnitList.add(new SourceJavaFileObject(file));
              }

              return FileVisitResult.CONTINUE;
            }
          });
        } catch (IOException ioException) {
          throw new MojoExecutionException("Encountered a problem walking the source tree", ioException);
        }

        compile(javaCompiler, new VirtualJavaFileManager(javaCompiler.getStandardFileManager(null, null, null), classLoader), new DiagnosticCollector<>(), compilerOptions, compilationUnitList, sourcePath);
        for (Class<?> clazz : classLoader) {
          getLog().info("!!!!!!!!!!!!!!!!!:" + clazz.getName());
        }
      }
    }
  }

  private void compile (JavaCompiler javaCompiler, JavaFileManager fileManager, DiagnosticCollector<JavaFileObject> collector, LinkedList<String> compilerOptions, LinkedList<JavaFileObject> compilationUnitList, Path sourcePath)
    throws MojoExecutionException, MojoFailureException {

    JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, collector, compilerOptions, null, compilationUnitList);

    if ((!task.call()) || collector.getDiagnostics().size() > 0) {

      StringBuilder exceptionMsg = new StringBuilder();
      boolean hasWarnings = false;
      boolean hasErrors = false;

      for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
        switch (d.getKind()) {
          case NOTE:
            break;
          case MANDATORY_WARNING:
            break;
          case WARNING:
            hasWarnings = true;
            break;
          case OTHER:
            break;
          case ERROR:
            hasErrors = true;
            break;
          default:
            throw new MojoFailureException("Unknown diagnostic kind(" + d.getKind().name() + ")");
        }

        getLog().info("#####################:" + d.getMessage(Locale.US));
        exceptionMsg.append("\n").append("[kind=").append(d.getKind());
        exceptionMsg.append(", ").append("name=").append(d.getSource().getName().substring(sourcePath.toString().length() + 2, d.getSource().getName().length() - 5).replace("/", "."));
        exceptionMsg.append(", ").append("line=").append(d.getLineNumber());
        exceptionMsg.append(", ").append("message=").append(d.getMessage(Locale.US)).append("]");
      }

      if (hasWarnings || hasErrors) {
        throw new MojoExecutionException(exceptionMsg.toString());
      }
    }
  }
}
