package org.smallmind.license;

/**
 * Maven plugin configuration bean that identifies the root project by group and artifact
 * coordinates.
 *
 * <p>Mojos use this to stop reactor-hierarchy traversal at a specific parent module rather than
 * always walking all the way to the top-most parent. Configure it as a nested {@code <root>}
 * element inside the plugin's {@code <configuration>} block.
 */
public class Root {

  private String groupId;
  private String artifactId;

  /**
   * Returns the Maven {@code groupId} of the designated root project.
   *
   * @return the configured group id, or {@code null} if not set
   */
  public String getGroupId () {

    return groupId;
  }

  /**
   * Returns the Maven {@code artifactId} of the designated root project.
   *
   * @return the configured artifact id, or {@code null} if not set
   */
  public String getArtifactId () {

    return artifactId;
  }
}
