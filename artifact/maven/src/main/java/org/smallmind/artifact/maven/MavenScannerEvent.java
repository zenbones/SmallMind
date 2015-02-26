package org.smallmind.artifact.maven;

import java.util.EventObject;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;

public class MavenScannerEvent extends EventObject {

  private ClassLoader classLoader;
  private Map<Artifact, Artifact> artifactDeltaMap;
  private Artifact[] artifacts;

  public MavenScannerEvent (Object source, Map<Artifact, Artifact> artifactDeltaMap, Artifact[] artifacts, ClassLoader classLoader) {

    super(source);

    this.artifactDeltaMap = artifactDeltaMap;
    this.artifacts = artifacts;
    this.classLoader = classLoader;
  }

  public Map<Artifact, Artifact> getArtifactDeltaMap () {

    return artifactDeltaMap;
  }

  public Artifact[] getArtifacts () {

    return artifacts;
  }

  public ClassLoader getClassLoader () {

    return classLoader;
  }
}
