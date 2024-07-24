/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.forge.style;

import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DependencyWrapper implements Comparable<DependencyWrapper> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>();

  private final Node dependencyNode;
  private final String groupId;
  private final String artifactId;

  public DependencyWrapper (Node dependencyNode) {

    this.dependencyNode = dependencyNode;

    groupId = ((Element)dependencyNode).getElementsByTagName("groupId").item(0).getTextContent();
    artifactId = ((Element)dependencyNode).getElementsByTagName("artifactId").item(0).getTextContent();
  }

  public String getGroupId () {

    return groupId;
  }

  public String getArtifactId () {

    return artifactId;
  }

  public Node getDependencyNode () {

    return dependencyNode;
  }

  @Override
  public int compareTo (DependencyWrapper otherWrapper) {

    int comparison;

    if ((comparison = subCompare(groupId, otherWrapper.groupId, "\\.")) != 0) {

      return comparison;
    } else {

      return subCompare(artifactId, otherWrapper.artifactId, "-");
    }
  }

  private int subCompare (String first, String second, String separator) {

    String[] firstSegements = first.split(separator);
    String[] secondSegments = second.split(separator);
    int comparableSegments = Math.min(firstSegements.length, secondSegments.length);

    for (int segmentIndex = 0; segmentIndex < comparableSegments; segmentIndex++) {

      int comparison;

      if ((comparison = ALPHA_NUMERIC_COMPARATOR.compare(firstSegements[segmentIndex], secondSegments[segmentIndex])) != 0) {

        return comparison;
      }
    }

    return Integer.compare(firstSegements.length, secondSegments.length);
  }
}
