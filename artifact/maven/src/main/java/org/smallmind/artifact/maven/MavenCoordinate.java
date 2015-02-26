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
    if (!version.equals(that.version)) {
      return false;
    }

    return true;
  }
}
