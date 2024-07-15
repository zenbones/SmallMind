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
package org.smallmind.artifact.maven;

public class MavenCoordinate {

  private String groupId;
  private String artifactId;
  private String classifier;
  private String extension = "jar";
  private String version;

  public MavenCoordinate () {

  }

  public MavenCoordinate (String groupId, String artifactId, String version) {

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  public MavenCoordinate (String groupId, String artifactId, String classifier, String version) {

    this(groupId, artifactId, version);

    this.classifier = classifier;
  }

  public MavenCoordinate (String groupId, String artifactId, String classifier, String extension, String version) {

    this(groupId, artifactId, classifier, version);

    this.extension = extension;
  }

  public String getGroupId () {

    return groupId;
  }

  public void setGroupId (String groupId) {

    this.groupId = groupId;
  }

  public String getArtifactId () {

    return artifactId;
  }

  public void setArtifactId (String artifactId) {

    this.artifactId = artifactId;
  }

  public String getClassifier () {

    return classifier;
  }

  public void setClassifier (String classifier) {

    this.classifier = classifier;
  }

  public String getExtension () {

    return extension;
  }

  public void setExtension (String extension) {

    this.extension = extension;
  }

  public String getVersion () {

    return version;
  }

  public void setVersion (String version) {

    this.version = version;
  }

  @Override
  public int hashCode () {

    int result = groupId.hashCode();

    result = 31 * result + artifactId.hashCode();
    result = 31 * result + (classifier != null ? classifier.hashCode() : 0);
    result = 31 * result + (extension != null ? extension.hashCode() : 0);
    result = 31 * result + version.hashCode();

    return result;
  }

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
