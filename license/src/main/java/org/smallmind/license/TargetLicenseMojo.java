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

// Installs license files for inclusion in distribution artifacts
@Mojo(name = "install-license-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class TargetLicenseMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private Root root;
  @Parameter
  private Rule[] rules;
  @Parameter
  private String[] licenses;
  @Parameter(defaultValue = "false")
  private boolean verbose;

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
