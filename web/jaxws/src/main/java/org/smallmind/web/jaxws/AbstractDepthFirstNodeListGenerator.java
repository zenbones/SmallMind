/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.web.jaxws;

import java.io.File;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;

abstract class AbstractDepthFirstNodeListGenerator
  implements DependencyVisitor {

  protected final List<DependencyNode> nodes;
  private final Map<DependencyNode, Object> visitedNodes;

  public AbstractDepthFirstNodeListGenerator () {

    nodes = new ArrayList<DependencyNode>(128);
    visitedNodes = new IdentityHashMap<DependencyNode, Object>(512);
  }

  /**
   * Gets the list of dependency nodes that was generated during the graph traversal.
   *
   * @return The list of dependency nodes, never {@code null}.
   */
  public List<DependencyNode> getNodes () {

    return nodes;
  }

  /**
   * Gets the dependencies seen during the graph traversal.
   *
   * @param includeUnresolved Whether unresolved dependencies shall be included in the result or not.
   * @return The list of dependencies, never {@code null}.
   */
  public List<Dependency> getDependencies (boolean includeUnresolved) {

    List<Dependency> dependencies = new ArrayList<Dependency>(getNodes().size());

    for (DependencyNode node : getNodes()) {
      Dependency dependency = node.getDependency();
      if (dependency != null) {
        if (includeUnresolved || dependency.getArtifact().getFile() != null) {
          dependencies.add(dependency);
        }
      }
    }

    return dependencies;
  }

  /**
   * Gets the artifacts associated with the list of dependency nodes generated during the graph traversal.
   *
   * @param includeUnresolved Whether unresolved artifacts shall be included in the result or not.
   * @return The list of artifacts, never {@code null}.
   */
  public List<Artifact> getArtifacts (boolean includeUnresolved) {

    List<Artifact> artifacts = new ArrayList<Artifact>(getNodes().size());

    for (DependencyNode node : getNodes()) {
      if (node.getDependency() != null) {
        Artifact artifact = node.getDependency().getArtifact();
        if (includeUnresolved || artifact.getFile() != null) {
          artifacts.add(artifact);
        }
      }
    }

    return artifacts;
  }

  /**
   * Gets the files of resolved artifacts seen during the graph traversal.
   *
   * @return The list of artifact files, never {@code null}.
   */
  public List<File> getFiles () {

    List<File> files = new ArrayList<File>(getNodes().size());

    for (DependencyNode node : getNodes()) {
      if (node.getDependency() != null) {
        File file = node.getDependency().getArtifact().getFile();
        if (file != null) {
          files.add(file);
        }
      }
    }

    return files;
  }

  /**
   * Gets a class path by concatenating the artifact files of the visited dependency nodes. Nodes with unresolved
   * artifacts are automatically skipped.
   *
   * @return The class path, using the platform-specific path separator, never {@code null}.
   */
  public String getClassPath () {

    StringBuilder buffer = new StringBuilder(1024);

    for (Iterator<DependencyNode> it = getNodes().iterator(); it.hasNext(); ) {
      DependencyNode node = it.next();
      if (node.getDependency() != null) {
        Artifact artifact = node.getDependency().getArtifact();
        if (artifact.getFile() != null) {
          buffer.append(artifact.getFile().getAbsolutePath());
          if (it.hasNext()) {
            buffer.append(File.pathSeparatorChar);
          }
        }
      }
    }

    return buffer.toString();
  }

  /**
   * Marks the specified node as being visited and determines whether the node has been visited before.
   *
   * @param node The node being visited, must not be {@code null}.
   * @return {@code true} if the node has not been visited before, {@code false} if the node was already visited.
   */
  protected boolean setVisited (DependencyNode node) {

    return visitedNodes.put(node, Boolean.TRUE) == null;
  }

  public abstract boolean visitEnter (DependencyNode node);

  public abstract boolean visitLeave (DependencyNode node);

}