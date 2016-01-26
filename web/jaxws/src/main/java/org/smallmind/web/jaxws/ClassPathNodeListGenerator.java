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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

final class ClassPathNodeListGenerator extends PreorderNodeListGenerator {

  private final List<DependencyNode> endorsedNodes;
  private boolean endorsed;

  public ClassPathNodeListGenerator () {

    endorsedNodes = new ArrayList<DependencyNode>(8);
    endorsed = false;
  }

  public void setEndorsed (boolean endorsed) {

    this.endorsed = endorsed;
  }

  @Override
  public List<DependencyNode> getNodes () {

    List<DependencyNode> retVal = new ArrayList<DependencyNode>(super.getNodes());
    if (endorsed) {
      retVal.retainAll(endorsedNodes);
    }
    else {
      retVal.removeAll(endorsedNodes);
    }
    return retVal;
  }

  @Override
  public boolean visitEnter (DependencyNode node) {

    Artifact a = node.getDependency().getArtifact();
    if ("jaxws-api".equals(a.getArtifactId()) || "jaxb-api".equals(a.getArtifactId())
      || "saaj-api".equals(a.getArtifactId()) || "jsr181-api".equals(a.getArtifactId())
      || "javax.annotation".equals(a.getArtifactId())
      || "javax.annotation-api".equals(a.getArtifactId())
      || "webservices-api".equals(a.getArtifactId())) {
      endorsedNodes.add(node);
    }
    else if (a.getArtifactId().startsWith("javax.xml.ws")
      || a.getArtifactId().startsWith("javax.xml.bind")) {
      endorsedNodes.add(node);
    }
    return super.visitEnter(node);
  }
}