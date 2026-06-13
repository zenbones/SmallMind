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
package org.smallmind.spark.singularity.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;

/**
 * Minimal {@link ArtifactFactory} for driving {@link GenerateSingularityMojo} in tests. Only the single overload the
 * Mojo uses to create the attached application artifact is implemented; the rest are intentionally unsupported so
 * that any new dependency on this collaborator surfaces immediately rather than silently returning {@code null}.
 */
public class StubArtifactFactory implements ArtifactFactory {

  @Override
  public Artifact createArtifact (String groupId, String artifactId, String version, String scope, String type) {

    return new DefaultArtifact(groupId, artifactId, version, scope, type, null, new DefaultArtifactHandler(type));
  }

  @Override
  public Artifact createArtifactWithClassifier (String groupId, String artifactId, String version, String type, String classifier) {

    return new DefaultArtifact(groupId, artifactId, version, "compile", type, classifier, new DefaultArtifactHandler(type));
  }

  @Override
  public Artifact createDependencyArtifact (String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createDependencyArtifact (String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, boolean optional) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createDependencyArtifact (String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createDependencyArtifact (String groupId, String artifactId, VersionRange versionRange, String type, String classifier, String scope, String inheritedScope, boolean optional) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createBuildArtifact (String groupId, String artifactId, String version, String packaging) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createProjectArtifact (String groupId, String artifactId, String version) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createProjectArtifact (String groupId, String artifactId, String version, String scope) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createParentArtifact (String groupId, String artifactId, String version) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createPluginArtifact (String groupId, String artifactId, VersionRange versionRange) {

    throw new UnsupportedOperationException();
  }

  @Override
  public Artifact createExtensionArtifact (String groupId, String artifactId, VersionRange versionRange) {

    throw new UnsupportedOperationException();
  }
}
