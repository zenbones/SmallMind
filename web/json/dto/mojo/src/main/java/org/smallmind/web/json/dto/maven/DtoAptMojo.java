package org.smallmind.web.json.dto.maven;

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

  @Parameter(readonly = true, property = "project")
  private MavenProject project;

  @Override
  public void execute ()
    throws MojoExecutionException, MojoFailureException {

  }
}
