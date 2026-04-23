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
 * Mutable data-transfer object representing a fully qualified Maven artifact coordinate
 * (groupId, artifactId, version, optional classifier, and extension).
 *
 * <p>Coordinates are the primary input to {@link MavenRepository} artifact-resolution methods
 * and to {@link MavenScanner} for specifying which artifacts to monitor.  The {@code extension}
 * field defaults to {@code "jar"} and is overridden when targeting non-jar packaging such as
 * {@code pom} or {@code war}.
 *
 * <p>Instances produced by the no-argument constructor are expected to be fully populated via
 * setters before use; partial coordinates will cause resolution failures.
 */
public class MavenCoordinate {

  private String groupId;
  private String artifactId;
  private String classifier;
  private String extension = "jar";
  private String version;

  /**
   * Creates an empty coordinate whose fields must be supplied via setters before use.
   */
  public MavenCoordinate () {

  }

  /**
   * Creates a coordinate for the standard jar packaging of an artifact.
   *
   * <p>The extension is implicitly {@code "jar"} and the classifier is left {@code null}.
   *
   * @param groupId    Maven groupId (e.g. {@code "org.example"})
   * @param artifactId Maven artifactId (e.g. {@code "my-library"})
   * @param version    version string, including snapshot qualifiers (e.g. {@code "1.0.0-SNAPSHOT"})
   */
  public MavenCoordinate (String groupId, String artifactId, String version) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  /**
   * Creates a coordinate that includes a classifier, useful for selecting alternate artifacts
   * such as {@code "sources"} or {@code "tests"} jars.
   *
   * <p>The extension defaults to {@code "jar"}.
   *
   * @param groupId    Maven groupId
   * @param artifactId Maven artifactId
   * @param classifier artifact classifier; {@code null} is treated as absent
   * @param version    version string
   */
  public MavenCoordinate (String groupId, String artifactId, String classifier, String version) {

    this(groupId, artifactId, version);

    this.classifier = classifier;
  }

  /**
   * Creates a fully specified coordinate including classifier and packaging extension.
   *
   * @param groupId    Maven groupId
   * @param artifactId Maven artifactId
   * @param classifier artifact classifier; {@code null} is treated as absent
   * @param extension  packaging extension (e.g. {@code "jar"}, {@code "pom"}, {@code "war"})
   * @param version    version string
   */
  public MavenCoordinate (String groupId, String artifactId, String classifier, String extension, String version) {

    this(groupId, artifactId, classifier, version);

    this.extension = extension;
  }

  /**
   * Returns the Maven groupId.
   *
   * @return groupId, or {@code null} if this coordinate was created with the no-argument constructor
   * and {@link #setGroupId} has not yet been called
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Sets the Maven groupId.
   *
   * @param groupId groupId to assign
   */
  public void setGroupId (String groupId) {

    this.groupId = groupId;
  }

  /**
   * Returns the Maven artifactId.
   *
   * @return artifactId, or {@code null} if not yet set
   */
  public String getArtifactId () {

    return artifactId;
  }

  /**
   * Sets the Maven artifactId.
   *
   * @param artifactId artifactId to assign
   */
  public void setArtifactId (String artifactId) {

    this.artifactId = artifactId;
  }

  /**
   * Returns the artifact classifier.
   *
   * @return classifier, or {@code null} when the coordinate has no classifier
   */
  public String getClassifier () {

    return classifier;
  }

  /**
   * Sets the artifact classifier.
   *
   * @param classifier classifier to assign; pass {@code null} to indicate no classifier
   */
  public void setClassifier (String classifier) {

    this.classifier = classifier;
  }

  /**
   * Returns the packaging extension.
   *
   * @return extension; defaults to {@code "jar"} unless explicitly overridden
   */
  public String getExtension () {

    return extension;
  }

  /**
   * Sets the packaging extension.
   *
   * @param extension extension to assign (e.g. {@code "jar"}, {@code "pom"}, {@code "war"})
   */
  public void setExtension (String extension) {

    this.extension = extension;
  }

  /**
   * Returns the artifact version.
   *
   * @return version string, or {@code null} if not yet set
   */
  public String getVersion () {

    return version;
  }

  /**
   * Sets the artifact version.
   *
   * @param version version string to assign
   */
  public void setVersion (String version) {

    this.version = version;
  }

  /**
   * Returns a hash code computed from all five coordinate fields.
   *
   * <p>A {@code null} classifier or extension each contributes {@code 0} to the computation,
   * consistent with the equality check in {@link #equals(Object)}.
   *
   * @return hash code suitable for use in hash-based collections
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
   * Compares this coordinate to another for full equality across all five fields.
   *
   * <p>Both {@code null} classifiers and {@code null} extensions compare equal to each other,
   * and a {@code null} value is not equal to any non-{@code null} value for the same field.
   *
   * @param obj the object to compare against
   * @return {@code true} if {@code obj} is a {@code MavenCoordinate} whose groupId, artifactId,
   * classifier, extension, and version all equal the corresponding fields of this instance
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
