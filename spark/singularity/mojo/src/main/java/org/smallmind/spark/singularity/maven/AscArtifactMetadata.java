/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.io.File;
import java.io.IOException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;
import org.codehaus.plexus.util.FileUtils;

public class AscArtifactMetadata extends AbstractArtifactMetadata {

  private final Artifact artifact;
  private final File file;
  private final String fileName;

  public AscArtifactMetadata (Artifact artifact, File file) {

    super(artifact);

    this.artifact = artifact;
    this.file = file;

    fileName = getFilename();
  }

  public File getFile () {

    return file;
  }

  @Override
  public Object getKey () {

    return "gpg signature " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getClassifier();
  }

  @Override
  public String getBaseVersion () {

    return artifact.getBaseVersion();
  }

  @Override
  public String getLocalFilename (ArtifactRepository repository) {

    return fileName;
  }

  @Override
  public String getRemoteFilename () {

    return fileName;
  }

  @Override
  public boolean storedInArtifactVersionDirectory () {

    return true;
  }

  @Override
  public void storeInLocalRepository (ArtifactRepository localRepository, ArtifactRepository remoteRepository)
    throws RepositoryMetadataStoreException {

    File destination = new File(localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(this, remoteRepository));

    try {
      FileUtils.copyFile(file, destination);
    } catch (IOException ioException) {
      throw new RepositoryMetadataStoreException("Error copying ASC to the local repository", ioException);
    }
  }

  @Override
  public void merge (ArtifactMetadata metadata) {

    if (!((AscArtifactMetadata)metadata).getFile().equals(file)) {
      throw new IllegalStateException("Cannot add two different pieces of metadata for key(" + getKey() + ")");
    }
  }

  @Override
  public void merge (org.apache.maven.repository.legacy.metadata.ArtifactMetadata metadata) {

    if (!((AscArtifactMetadata)metadata).getFile().equals(file)) {
      throw new IllegalStateException("Cannot add two different pieces of metadata for key(" + getKey() + ")");
    }
  }

  private String getFilename () {

    StringBuilder nameBuilder = new StringBuilder(getArtifactId()).append("-").append(artifact.getVersion());

    if ((artifact.hasClassifier())) {
      nameBuilder.append("-").append(artifact.getClassifier());
    }

    return nameBuilder.append(".").append(artifact.getArtifactHandler().getExtension()).append(".asc").toString();
  }
}
