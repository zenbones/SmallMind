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
package org.smallmind.testbench.style;

import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Sortable wrapper around a {@code <dependency>} DOM node, pairing the node with its eagerly
 * extracted {@code groupId} and {@code artifactId} so a list of dependencies can be ordered without
 * re-walking the DOM. Ordering is the segment-aware, canonical order used by both
 * {@link DependencyOrganizer} and {@link DependencyReducer}: by {@code groupId} first, then by
 * {@code artifactId}.
 */
public class DependencyWrapper implements Comparable<DependencyWrapper> {

  private static final AlphaNumericComparator<String> ALPHA_NUMERIC_COMPARATOR = new AlphaNumericComparator<>();

  private final Node dependencyNode;
  private final String groupId;
  private final String artifactId;

  /**
   * Wraps a {@code <dependency>} node, eagerly reading its coordinates for later comparison.
   *
   * @param dependencyNode the DOM node to wrap; it must contain {@code <groupId>} and
   * {@code <artifactId>} child elements
   */
  public DependencyWrapper (Node dependencyNode) {

    this.dependencyNode = dependencyNode;

    groupId = ((Element)dependencyNode).getElementsByTagName("groupId").item(0).getTextContent();
    artifactId = ((Element)dependencyNode).getElementsByTagName("artifactId").item(0).getTextContent();
  }

  /**
   * Returns the {@code groupId} read from the wrapped node.
   *
   * @return the groupId text content
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Returns the {@code artifactId} read from the wrapped node.
   *
   * @return the artifactId text content
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * Returns the wrapped {@code <dependency>} DOM node.
   *
   * @return the underlying dependency node
   */
  public Node getDependencyNode () {

    return dependencyNode;
  }

  /**
   * Orders this wrapper against another by {@code groupId} first and, when those are equal, by
   * {@code artifactId}. The {@code groupId} is compared as dot-delimited segments and the
   * {@code artifactId} as dash-delimited segments, each segment via an alphanumeric comparator.
   *
   * @param otherWrapper the wrapper to compare against
   * @return a negative integer, zero, or a positive integer as this dependency sorts before, equal
   * to, or after {@code otherWrapper}
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
   * Compares two coordinate strings segment by segment, splitting each on {@code separator} and
   * comparing matching segments alphanumerically. When every shared segment is equal, the string
   * with fewer segments sorts first, so a shorter prefix precedes a longer one that extends it.
   *
   * @param first the left-hand string
   * @param second the right-hand string
   * @param separator the regular expression used to split each string into segments
   * @return a negative integer, zero, or a positive integer as {@code first} sorts before, equal to,
   * or after {@code second}
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
