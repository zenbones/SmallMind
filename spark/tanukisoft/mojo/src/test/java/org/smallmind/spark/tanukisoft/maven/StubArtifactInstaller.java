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
package org.smallmind.spark.tanukisoft.maven;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * Capturing {@link ArtifactInstaller} double that records the file and artifact handed to it, and can be told to
 * fail so the Mojo's exception-translation path can be exercised.
 */
public class StubArtifactInstaller implements ArtifactInstaller {

  private final boolean fail;
  private File installedFile;
  private Artifact installedArtifact;

  public StubArtifactInstaller () {

    this(false);
  }

  public StubArtifactInstaller (boolean fail) {

    this.fail = fail;
  }

  public File installedFile () {

    return installedFile;
  }

  public Artifact installedArtifact () {

    return installedArtifact;
  }

  @Override
  public void install (String basedir, String finalName, Artifact artifact, ArtifactRepository localRepository) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void install (File source, Artifact artifact, ArtifactRepository localRepository)
    throws ArtifactInstallationException {

    if (fail) {
      throw new ArtifactInstallationException("forced installation failure");
    }

    installedFile = source;
    installedArtifact = artifact;
  }
}
