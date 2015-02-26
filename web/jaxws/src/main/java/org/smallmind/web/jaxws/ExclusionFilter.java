package org.smallmind.web.jaxws;

import java.util.List;
import org.apache.maven.model.Exclusion;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

final class ExclusionFilter implements DependencyFilter {

  private final List<Exclusion> toExclude;

  public ExclusionFilter (List<Exclusion> toExclude) {

    assert toExclude != null : "Null is not allowed";
    this.toExclude = toExclude;
  }

  @Override
  public boolean accept (DependencyNode node, List<DependencyNode> parents) {

    Artifact a = node.getDependency().getArtifact();
    for (Exclusion e : toExclude) {
      if (e.getGroupId().equals(a.getGroupId())
        && e.getArtifactId().equals(a.getArtifactId())) {
        return false;
      }
    }
    return true;
  }
}