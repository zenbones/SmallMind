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
 * Change-detection token that pairs a resolved {@link Artifact} with the last-modified timestamp
 * of its backing file at the moment the tag was created.
 *
 * <p>{@link MavenScanner} maintains one tag per monitored coordinate and compares a freshly
 * resolved tag against the stored one after each polling cycle.  The comparison semantics
 * intentionally differ between release and snapshot artifacts:
 * <ul>
 *   <li><b>Releases</b> — identity alone determines equality; the file timestamp is ignored
 *       because a release coordinate always maps to the same immutable artifact.</li>
 *   <li><b>Snapshots</b> — both identity and file timestamp must match.  A newer timestamp
 *       signals that the remote repository published a new snapshot build even though the
 *       version string has not changed.</li>
 * </ul>
 */
public class ArtifactTag {

  private final Artifact artifact;
  private final long lastModTime;

  /**
   * Creates a tag for the given artifact, capturing its file's last-modified time immediately.
   *
   * <p>If the artifact has not yet been resolved to a local file ({@link Artifact#getFile()}
   * returns {@code null}), the recorded modification time is {@code 0}.
   *
   * @param artifact the resolved artifact to wrap; must not be {@code null}
   */
  public ArtifactTag (Artifact artifact) {

    File artifactFile;

    this.artifact = artifact;

    lastModTime = ((artifactFile = artifact.getFile()) == null) ? 0 : artifactFile.lastModified();
  }

  /**
   * Returns the wrapped artifact.
   *
   * @return the artifact passed to the constructor; never {@code null}
   */
  public Artifact getArtifact () {

    return artifact;
  }

  /**
   * Returns the last-modified time of the artifact file recorded when this tag was constructed.
   *
   * @return epoch-millisecond timestamp as returned by {@link File#lastModified()}, or {@code 0}
   * if the artifact had no associated local file at construction time
   */
  public long getLastModTime () {

    return lastModTime;
  }

  /**
   * Returns a hash code derived from the artifact identity and the recorded file timestamp.
   *
   * <p>The XOR combination ensures that two tags for the same snapshot coordinate but different
   * file timestamps produce different hash codes, supporting correct behavior in hash-based
   * collections used by {@link MavenScanner}.
   *
   * @return hash code of the artifact, additionally XOR'd with the modification time for snapshots
   */
  @Override
  public int hashCode () {

    return artifact.isSnapshot() ? artifact.hashCode() ^ (int)lastModTime : artifact.hashCode();
  }

  /**
   * Determines whether this tag represents the same artifact state as another.
   *
   * <p>For release artifacts, only the artifact identity (groupId, artifactId, version, etc.)
   * is compared.  For snapshot artifacts, the file modification timestamps must also be equal,
   * so that a re-deployed snapshot with unchanged version metadata is still detected as changed.
   *
   * @param obj the object to compare against
   * @return {@code true} if {@code obj} is an {@code ArtifactTag} wrapping an equal artifact and,
   * for snapshots, recording the same file modification time; {@code false} otherwise
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ArtifactTag) && artifact.equals(((ArtifactTag)obj).getArtifact()) && ((!artifact.isSnapshot()) || (lastModTime == ((ArtifactTag)obj).getLastModTime()));
  }
}
