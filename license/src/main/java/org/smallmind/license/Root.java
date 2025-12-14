package org.smallmind.license;

/**
 * Identifies the root Maven project by group and artifact, allowing child modules to look up shared configuration.
 */
public class Root {

  private String groupId;
  private String artifactId;

  /**
   * Returns the Maven group id of the root project.
   *
   * @return the configured group id, or {@code null} if none has been set
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Returns the Maven artifact id of the root project.
   *
   * @return the configured artifact id, or {@code null} if none has been set
   */
  public String getArtifactId () {

    return artifactId;
  }
}
