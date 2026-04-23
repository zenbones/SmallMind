package org.smallmind.license;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Maven Mojo that copies license files into the project's build output directory so they are
 * included in distribution artifacts.
 *
 * <p>Bound to the {@code prepare-package} lifecycle phase under the goal name
 * {@code install-license-files}. Each entry in the {@code licenses} list is resolved relative to
 * the root project's base directory when the path is not absolute. Files that cannot be located
 * are skipped with a warning; files that cannot be copied cause the build to fail.
 */
@Mojo(name = "install-license-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TargetLicenseMojo extends AbstractMojo {

  /**
   * The current Maven project, supplied by Maven at execution time.
   */
  @Parameter(readonly = true, property = "project")
  private MavenProject project;

  /**
   * Optional root project identifier. When set, relative license file paths are resolved against
   * the identified ancestor module rather than the top-most Maven parent.
   */
  @Parameter
  private Root root;

  /**
   * Accepted so this goal can share a single plugin declaration with
   * {@code generate-notice-headers}. This goal does not use rule definitions.
   */
  @Parameter
  private Rule[] rules;

  /**
   * Paths of the license files to copy into the project's output directory. At least one entry
   * is required. Relative paths are resolved against the root project's base directory.
   */
  @Parameter(required = true)
  private String[] licenses;

  /**
   * When {@code true}, an informational log message is emitted for each license file copied.
   */
  @Parameter(defaultValue = "false")
  private boolean verbose;

  /**
   * Copies each configured license file into the project's build output directory. Files that
   * cannot be located are skipped with a warning. The goal is a no-op when the output directory
   * does not yet exist.
   *
   * @throws MojoExecutionException if a located license file cannot be copied to the output
   *                                directory
   */
  @Override
  public void execute ()
    throws MojoExecutionException {

    if (Files.isDirectory(Paths.get(project.getBuild().getOutputDirectory()))) {

      MavenProject rootProject = project;

      while ((root == null) ? !(rootProject.getParent() == null) : !(root.getGroupId().equals(rootProject.getGroupId()) && root.getArtifactId().equals(rootProject.getArtifactId()))) {
        rootProject = rootProject.getParent();
      }

      for (String license : licenses) {

        Path licenseFile;
        Path copyFile;

        if (!(licenseFile = Paths.get(license)).isAbsolute()) {
          licenseFile = rootProject.getBasedir().toPath().resolve(license);
        }

        if (!Files.isRegularFile(licenseFile)) {
          getLog().warn(String.format("Unable to acquire the license file(%s), skipping license copying...", licenseFile));
        } else {
          if (verbose) {
            getLog().info(String.format("Copying license(%s)...", licenseFile.getFileName()));
          }

          copyFile = Paths.get(project.getBuild().getOutputDirectory()).resolve(licenseFile.getFileName());

          try {
            Files.copy(licenseFile, copyFile, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException ioException) {
            try {
              Files.deleteIfExists(copyFile);
              throw new MojoExecutionException("Problem in copying output license file (" + copyFile + ")", ioException);
            } catch (IOException innerIoException) {
              throw new MojoExecutionException("Problem in copying output license file (" + copyFile + ")", innerIoException.initCause(ioException));
            }
          }
        }
      }
    }
  }
}
