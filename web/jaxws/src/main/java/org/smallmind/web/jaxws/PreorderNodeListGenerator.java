package org.smallmind.web.jaxws;

import org.eclipse.aether.graph.DependencyNode;

public class PreorderNodeListGenerator
  extends AbstractDepthFirstNodeListGenerator {

  /**
   * Creates a new preorder list generator.
   */
  public PreorderNodeListGenerator () {

  }

  @Override
  public boolean visitEnter (DependencyNode node) {

    if (!setVisited(node)) {
      return false;
    }

    if (node.getDependency() != null) {
      nodes.add(node);
    }

    return true;
  }

  @Override
  public boolean visitLeave (DependencyNode node) {

    return true;
  }
}
