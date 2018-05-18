package org.smallmind.web.json.dto.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
@Mojo(name = "generate-dtos", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class DtoAptMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "project")
  private MavenProject project;

  @Override
  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    final JavaCompiler javaCompiler;

    if ((javaCompiler = ToolProvider.getSystemJavaCompiler()) == null) {
      throw new MojoFailureException("Unable to acquire the java compiler toolchain");
    } else {

      LinkedList<JavaFileObject> compilationUnitList = new LinkedList<>();
      LinkedList<String> compilerOptions = new LinkedList<>();
      Path sourcePath;

      compilerOptions.add("-classpath");

      StringBuilder sb = new StringBuilder();
      for (Artifact artifact : project.getArtifacts()) {
        if (artifact.getArtifactHandler().isAddedToClasspath()) {
          if (sb.length() > 0) {
            sb.append(";");
          }
          sb.append(artifact.getFile().getAbsolutePath());
        }
      }
      compilerOptions.add(sb.toString());

      try {
        Files.walkFileTree(sourcePath = Paths.get(project.getBuild().getSourceDirectory()), new SimpleFileVisitor<Path>() {

          @Override
          public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {

            if (file.getFileName().toString().endsWith(".java")) {
              compilationUnitList.add(new VirtualJavaFileObject(file));
            }

            return FileVisitResult.CONTINUE;
          }
        });
      } catch (IOException ioException) {
        throw new MojoExecutionException("Encountered a problem walking the source tree", ioException);
      }

      compile(javaCompiler, new VirtualJavaFileManager(javaCompiler.getStandardFileManager(null, null, null)), new DiagnosticCollector<>(), compilerOptions, compilationUnitList, sourcePath);
    }
  }

  private void compile (JavaCompiler javaCompiler, JavaFileManager fileManager, DiagnosticCollector<JavaFileObject> collector, LinkedList<String> compilerOptions, LinkedList<JavaFileObject> compilationUnitList, Path sourcePath)
    throws MojoExecutionException {

    JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, collector, compilerOptions, null, compilationUnitList);

    if ((!task.call()) || collector.getDiagnostics().size() > 0) {

      StringBuilder exceptionMsg = new StringBuilder();
      boolean hasWarnings = false;
      boolean hasErrors = false;

      for (Diagnostic<? extends JavaFileObject> d : collector.getDiagnostics()) {
        switch (d.getKind()) {
          case NOTE:
          case MANDATORY_WARNING:
          case WARNING:
            hasWarnings = true;
            break;
          case OTHER:
          case ERROR:
          default:
            hasErrors = true;
            break;
        }

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
