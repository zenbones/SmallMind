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

/**
 * Immutable value object representing a Maven dependency reference parsed from text.
 */
public class DependencyReference {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String scope;

  /**
   * Parse a colon-delimited dependency reference of the form {@code groupId:artifactId:classifier:version:scope}.
   *
   * @param reference the textual representation of a dependency
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
   * @return the group id portion of the reference
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * @return the artifact id portion of the reference
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * @return the version portion of the reference
   */
  public String getVersion () {

    return version;
  }

  /**
   * @return the classifier portion of the reference
   */
  public String getClassifier () {

    return classifier;
  }

  /**
   * @return the dependency scope
   */
  public String getScope () {

    return scope;
  }
}
