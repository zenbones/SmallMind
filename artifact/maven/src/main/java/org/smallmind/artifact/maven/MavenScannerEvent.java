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
 * Event published by {@link MavenScanner} when one or more monitored artifacts have changed.
 *
 * <p>The event conveys three complementary views of the current scan result:
 * <ul>
 *   <li><b>Delta map</b> — only the artifacts that changed, keyed by the newly resolved artifact
 *       and mapped to the artifact it replaced.  The mapped value is {@code null} when the
 *       artifact is observed for the first time (i.e. on the initial scan after
 *       {@link MavenScanner#start()}).</li>
 *   <li><b>Artifacts array</b> — one entry per monitored coordinate in the order they were
 *       supplied to the scanner, reflecting the current artifact for each slot.  Entries are
 *       {@code null} for coordinates that have not yet been successfully resolved.</li>
 *   <li><b>Class loader</b> — a {@link ClassLoader} constructed over every artifact in the
 *       changed set and their transitive compile dependencies, enabling callers to load and
 *       instantiate classes from the updated artifacts without restarting the JVM.</li>
 * </ul>
 */
public class MavenScannerEvent extends EventObject {

  private final ClassLoader classLoader;
  private final Map<Artifact, Artifact> artifactDeltaMap;
  private final Artifact[] artifacts;

  /**
   * Constructs an event representing a completed scan that detected at least one change.
   *
   * @param source           the {@link MavenScanner} that fired this event; passed to
   *                         {@link EventObject#EventObject(Object)}
   * @param artifactDeltaMap map of each changed artifact (new version) to the artifact it
   *                         replaced (old version), with {@code null} values for first-time
   *                         observations; must not be {@code null}
   * @param artifactTags     current {@link ArtifactTag} array in coordinate order, one entry
   *                         per monitored coordinate; entries may be {@code null} for unresolved
   *                         coordinates; the artifacts array exposed by this event is derived
   *                         from these tags
   * @param classLoader      class loader capable of loading the changed artifacts and their
   *                         transitive compile dependencies; must not be {@code null}
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
   * Returns the delta map containing only the artifacts that changed in this scan cycle.
   *
   * <p>Each key is the newly resolved {@link Artifact}; the corresponding value is the
   * prior artifact it replaced, or {@code null} if the artifact was not present in the
   * previous scan (i.e. first observation).
   *
   * @return non-{@code null}, non-empty map of changed artifacts to their predecessors
   */
  public Map<Artifact, Artifact> getArtifactDeltaMap () {

    return artifactDeltaMap;
  }

  /**
   * Returns the current state of all monitored coordinates in the order they were registered.
   *
   * <p>Unlike {@link #getArtifactDeltaMap()}, this array covers every monitored coordinate,
   * not just those that changed.  An entry is {@code null} if the corresponding coordinate
   * has never been successfully resolved.
   *
   * @return array of current artifacts, one per monitored coordinate in registration order;
   * individual entries may be {@code null}
   */
  public Artifact[] getArtifacts () {

    return artifacts;
  }

  /**
   * Returns a class loader that can load classes from the changed artifacts and their
   * transitive compile-scope dependencies.
   *
   * <p>The loader delegates to the thread context class loader that was active when the
   * scanner worker invoked the scan, making existing JVM classes available as a fallback.
   *
   * @return class loader over the updated artifact set; never {@code null}
   */
  public ClassLoader getClassLoader () {

    return classLoader;
  }
}
