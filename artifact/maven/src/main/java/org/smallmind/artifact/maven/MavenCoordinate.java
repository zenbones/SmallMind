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
package org.smallmind.artifact.maven;

/**
 * Mutable representation of a Maven coordinate (groupId, artifactId, classifier, extension, version).
 * Coordinates are used when requesting artifacts from a {@link MavenRepository}.
 */
public class MavenCoordinate {

  private String groupId;
  private String artifactId;
  private String classifier;
  private String extension = "jar";
  private String version;

  /**
   * Creates an empty coordinate that can be populated via setters.
   */
  public MavenCoordinate () {

  }

  /**
   * Constructs coordinates for a standard jar artifact.
   *
   * @param groupId    the group identifier.
   * @param artifactId the artifact identifier.
   * @param version    the version string.
   */
  public MavenCoordinate (String groupId, String artifactId, String version) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  /**
   * Constructs coordinates including a classifier.
   *
   * @param groupId    the group identifier.
   * @param artifactId the artifact identifier.
   * @param classifier optional classifier for the artifact.
   * @param version    the version string.
   */
  public MavenCoordinate (String groupId, String artifactId, String classifier, String version) {

    this(groupId, artifactId, version);

    this.classifier = classifier;
  }

  /**
   * Constructs fully specified coordinates including classifier and extension.
   *
   * @param groupId    the group identifier.
   * @param artifactId the artifact identifier.
   * @param classifier optional classifier for the artifact.
   * @param extension  artifact packaging/extension (defaults to {@code jar}).
   * @param version    the version string.
   */
  public MavenCoordinate (String groupId, String artifactId, String classifier, String extension, String version) {

    this(groupId, artifactId, classifier, version);

    this.extension = extension;
  }

  /**
   * @return the group identifier.
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * @param groupId the group identifier.
   */
  public void setGroupId (String groupId) {

    this.groupId = groupId;
  }

  /**
   * @return the artifact identifier.
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * @param artifactId the artifact identifier.
   */
  public void setArtifactId (String artifactId) {

    this.artifactId = artifactId;
  }

  /**
   * @return classifier or {@code null} when not applicable.
   */
  public String getClassifier () {

    return classifier;
  }

  /**
   * @param classifier optional classifier for the artifact.
   */
  public void setClassifier (String classifier) {

    this.classifier = classifier;
  }

  /**
   * @return artifact packaging/extension (defaults to {@code jar}).
   */
  public String getExtension () {

    return extension;
  }

  /**
   * @param extension artifact packaging/extension.
   */
  public void setExtension (String extension) {

    this.extension = extension;
  }

  /**
   * @return version string.
   */
  public String getVersion () {

    return version;
  }

  /**
   * @param version version string.
   */
  public void setVersion (String version) {

    this.version = version;
  }

  /**
   * Calculates a hash code using all coordinate parts.
   *
   * @return hash suitable for map/set use.
   */
  @Override
  public int hashCode () {

    int result = groupId.hashCode();

    result = 31 * result + artifactId.hashCode();
    result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
    result = 31 * result + (extension != null ? extension.hashCode() : 0);
    result = 31 * result + version.hashCode();

    return result;
  }

  /**
   * Coordinates are equal when all parts (including optional classifier and extension) match.
   *
   * @param obj object to compare.
   * @return {@code true} when the coordinates describe the same artifact.
   */
  @Override
  public boolean equals (Object obj) {

    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MavenCoordinate)) {
      return false;
    }

    MavenCoordinate that = (MavenCoordinate)obj;

    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    if (classifier != null ? !classifier.equals(that.classifier) : that.classifier != null) {
      return false;
    }
    if (extension != null ? !extension.equals(that.extension) : that.extension != null) {
      return false;
    }
    if (!groupId.equals(that.groupId)) {
      return false;
    }

    return version.equals(that.version);
  }
}
