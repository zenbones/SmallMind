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

/**
 * Immutable Maven dependency coordinate parsed from one line of {@code mvn dependency:analyze}
 * output, where the coordinate is colon-delimited as
 * {@code groupId:artifactId:classifier:version:scope}. Consumed by {@link DependencyReducer} to
 * decide which dependencies to add, remove, or re-scope.
 */
public class DependencyReference {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String scope;

  /**
   * Parses a colon-delimited dependency coordinate, expecting the five fields
   * {@code groupId:artifactId:classifier:version:scope} in that order.
   *
   * @param reference the raw coordinate string as produced by {@code mvn dependency:analyze}
   * @throws ArrayIndexOutOfBoundsException if {@code reference} contains fewer than five
   * colon-separated fields
   */
  public DependencyReference (String reference) {

    String[] split = reference.split(":");

    groupId = split[0];
    artifactId = split[1];
    classifier = split[2];
    version = split[3];
    scope = split[4];
  }

  /**
   * Returns the Maven {@code groupId}.
   *
   * @return the groupId, such as {@code org.smallmind}
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Returns the Maven {@code artifactId}.
   *
   * @return the artifactId, such as {@code testbench-docker}
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * Returns the Maven version.
   *
   * @return the version as it appeared in the analyze output
   */
  public String getVersion () {

    return version;
  }

  /**
   * Returns the Maven classifier.
   *
   * @return the classifier field from the coordinate
   */
  public String getClassifier () {

    return classifier;
  }

  /**
   * Returns the Maven scope.
   *
   * @return the scope, such as {@code compile}, {@code test}, or {@code provided}
   */
  public String getScope () {

    return scope;
  }
}
