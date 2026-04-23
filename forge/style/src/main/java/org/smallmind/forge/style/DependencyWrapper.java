/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Adapter that pairs a Maven dependency DOM node with its pre-extracted {@code groupId} and
 * {@code artifactId}, enabling natural sort-order comparison without repeated DOM traversal.
 */
public class DependencyWrapper implements Comparable<DependencyWrapper> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>();

  private final Node dependencyNode;
  private final String groupId;
  private final String artifactId;

  /**
   * Wrap a {@code <dependency>} DOM node and eagerly extract its coordinates for comparison.
   *
   * @param dependencyNode the DOM element to wrap; must contain {@code <groupId>} and
   *                       {@code <artifactId>} child elements
   */
  public DependencyWrapper (Node dependencyNode) {

    this.dependencyNode = dependencyNode;

    groupId = ((Element)dependencyNode).getElementsByTagName("groupId").item(0).getTextContent();
    artifactId = ((Element)dependencyNode).getElementsByTagName("artifactId").item(0).getTextContent();
  }

  /**
   * Returns the {@code groupId} extracted from the wrapped dependency node.
   *
   * @return groupId text content
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Returns the {@code artifactId} extracted from the wrapped dependency node.
   *
   * @return artifactId text content
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * Returns the underlying DOM node representing the {@code <dependency>} element.
   *
   * @return the wrapped dependency node
   */
  public Node getDependencyNode () {

    return dependencyNode;
  }

  /**
   * Compare this wrapper to {@code otherWrapper} for sort-order purposes.
   *
   * <p>Ordering is first by {@code groupId} (dot-delimited segments, alphanumerically), then
   * by {@code artifactId} (dash-delimited segments, alphanumerically) when the groupIds are equal.
   *
   * @param otherWrapper the wrapper to compare against
   * @return a negative integer, zero, or a positive integer as this dependency orders before,
   * at the same position as, or after {@code otherWrapper}
   */
  @Override
  public int compareTo (DependencyWrapper otherWrapper) {

    int comparison;

    if ((comparison = subCompare(groupId, otherWrapper.groupId, "\\.")) != 0) {

      return comparison;
    } else {

      return subCompare(artifactId, otherWrapper.artifactId, "-");
    }
  }

  /**
   * Compare two strings by splitting each on {@code separator} and comparing corresponding
   * segments with an alphanumeric comparator.
   *
   * <p>When all shared segments are equal, the string with fewer segments sorts first.
   *
   * @param first     the left-hand string
   * @param second    the right-hand string
   * @param separator regex used to split each string into segments
   * @return a negative integer, zero, or a positive integer as {@code first} orders before,
   * at the same position as, or after {@code second}
   */
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
