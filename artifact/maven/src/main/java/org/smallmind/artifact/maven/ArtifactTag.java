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

import java.io.File;
import org.eclipse.aether.artifact.Artifact;

/**
 * Lightweight wrapper around an {@link Artifact} that captures the artifact's last modification time on disk.
 * Instances are used as change tokens when polling a repository to detect when an artifact has been updated.
 */
public class ArtifactTag {

  private final Artifact artifact;
  private final long lastModTime;

  /**
   * Creates a tag for the supplied artifact and records the last modified timestamp of its backing file.
   *
   * @param artifact the resolved artifact to track; its file may be {@code null} for unresolved snapshots.
   */
  public ArtifactTag (Artifact artifact) {

    File artifactFile;

    this.artifact = artifact;

    lastModTime = ((artifactFile = artifact.getFile()) == null) ? 0 : artifactFile.lastModified();
  }

  /**
   * Returns the wrapped artifact.
   *
   * @return the artifact represented by this tag.
   */
  public Artifact getArtifact () {

    return artifact;
  }

  /**
   * Returns the last modification time (in epoch milliseconds) of the artifact file at tag creation.
   *
   * @return last modified time, or {@code 0} if the artifact had no associated file.
   */
  public long getLastModTime () {

    return lastModTime;
  }

  /**
   * Computes a hash based on the artifact identity and recorded modification time.
   *
   * @return combined hash code used for collection membership and change detection.
   */
  @Override
  public int hashCode () {

    return artifact.hashCode() ^ (int)lastModTime;
  }

  /**
   * Tags are equal when they wrap the same artifact and, for snapshots, the underlying file timestamp matches.
   *
   * @param obj comparison target.
   * @return {@code true} when the artifact identities (and snapshot timestamps) match.
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ArtifactTag) && artifact.equals(((ArtifactTag)obj).getArtifact()) && ((!artifact.isSnapshot()) || (lastModTime == ((ArtifactTag)obj).getLastModTime()));
  }
}
