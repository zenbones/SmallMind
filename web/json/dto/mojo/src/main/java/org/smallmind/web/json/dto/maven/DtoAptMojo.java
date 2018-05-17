package org.smallmind.web.json.dto.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

// Annotation processor for generating dto source
@Mojo(name = "generate-dto", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class DtoAptMojo extends AbstractMojo {

  @Override
  public void execute () {

  }
}
