package org.smallmind.spark.wrapper.mojo;

import org.apache.maven.artifact.Artifact;

public class Dependency {

   private String groupId;
   private String artifactId;

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

   public boolean matchesArtifact (Artifact artifact) {

      return groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId());
   }
}
