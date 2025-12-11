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

import java.util.EventObject;
import java.util.Map;
import org.eclipse.aether.artifact.Artifact;

/**
 * Event delivered to {@link MavenScannerListener}s describing detected artifact updates and providing
 * a class loader capable of loading the newly resolved artifacts.
 */
public class MavenScannerEvent extends EventObject {

  private final ClassLoader classLoader;
  private final Map<Artifact, Artifact> artifactDeltaMap;
  private final Artifact[] artifacts;

  /**
   * Constructs a new event with the changed artifacts and associated class loader.
   *
   * @param source scanner that produced the event.
   * @param artifactDeltaMap mapping of newly resolved artifacts to their previous versions (value is {@code null} on first discovery).
   * @param artifactTags current tags for each monitored coordinate.
   * @param classLoader class loader that can load the updated artifacts and their dependencies.
   */
  public MavenScannerEvent (Object source, Map<Artifact, Artifact> artifactDeltaMap, ArtifactTag[] artifactTags, ClassLoader classLoader) {

    super(source);

    this.artifactDeltaMap = artifactDeltaMap;
    this.classLoader = classLoader;

    artifacts = new Artifact[artifactTags.length];
    for (int index = 0; index < artifacts.length; index++) {
      artifacts[index] = (artifactTags[index] == null) ? null : artifactTags[index].getArtifact();
    }
  }

  /**
   * Provides a mapping of current artifacts to the prior artifacts they replace.
   *
   * @return map of artifact deltas; values may be {@code null} for newly observed artifacts.
   */
  public Map<Artifact, Artifact> getArtifactDeltaMap () {

    return artifactDeltaMap;
  }

  /**
   * Returns the current artifacts tracked by the scanner in coordinate order.
   *
   * @return array containing the latest artifact for each coordinate (entries may be {@code null} before first resolution).
   */
  public Artifact[] getArtifacts () {

    return artifacts;
  }

  /**
   * Returns a class loader that can load the updated artifacts and their dependencies.
   *
   * @return class loader tied to this event's resolved artifacts.
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }
}
