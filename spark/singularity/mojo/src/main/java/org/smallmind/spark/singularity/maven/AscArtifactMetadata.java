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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.AbstractArtifactMetadata;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataStoreException;

/**
 * Attaches a detached GPG signature ({@code .asc}) file to a Maven {@link Artifact} so that Maven's install and
 * deploy machinery will ship the signature alongside the artifact itself. The filename is derived from the
 * artifact's coordinates at construction time.
 */
public class AscArtifactMetadata extends AbstractArtifactMetadata {

  private final Artifact artifact;
  private final Path path;
  private final String fileName;

  /**
   * Binds the signature on disk to the artifact it was produced for and caches the canonical filename.
   *
   * @param artifact the artifact whose signature is being carried
   * @param path     filesystem location of the {@code .asc} signature file
   */
  public AscArtifactMetadata (Artifact artifact, Path path) {

    super(artifact);

    this.artifact = artifact;
    this.path = path;

    fileName = getFilename();
  }

  /**
   * Returns the on-disk location of the signature file supplied at construction.
   *
   * @return the {@link Path} to the {@code .asc} file
   */
  public Path getPath () {

    return path;
  }

  /**
   * Produces a metadata key unique per artifact type and classifier so that merges can detect collisions.
   *
   * @return a stable string key for this signature metadata
   */
  @Override
  public Object getKey () {

    return "gpg signature " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getType() + ":" + artifact.getClassifier();
  }

  /**
   * Exposes the artifact's base version so that Maven can place the signature in the correct version directory.
   *
   * @return the artifact's base version
   */
  @Override
  public String getBaseVersion () {

    return artifact.getBaseVersion();
  }

  /**
   * Supplies the filename used when the signature is written into the local repository.
   *
   * @param repository the local repository this metadata is being installed into (not consulted)
   * @return the precomputed {@code .asc} filename
   */
  @Override
  public String getLocalFilename (ArtifactRepository repository) {

    return fileName;
  }

  /**
   * Supplies the filename used when the signature is transferred to a remote repository.
   *
   * @return the precomputed {@code .asc} filename
   */
  @Override
  public String getRemoteFilename () {

    return fileName;
  }

  /**
   * Signals that the signature belongs next to a specific version of the artifact rather than at the groupId level.
   *
   * @return {@code true} always
   */
  @Override
  public boolean storedInArtifactVersionDirectory () {

    return true;
  }

  /**
   * Copies the signature file into the local repository location that Maven computes for this metadata.
   *
   * @param localRepository  repository to receive the signature
   * @param remoteRepository remote repository that is the nominal origin; used only to help Maven compute the path
   * @throws RepositoryMetadataStoreException if the signature file cannot be copied into the local repository
   */
  @Override
  public void storeInLocalRepository (ArtifactRepository localRepository, ArtifactRepository remoteRepository)
    throws RepositoryMetadataStoreException {

    Path destination = Paths.get(localRepository.getBasedir(), localRepository.pathOfLocalRepositoryMetadata(this, remoteRepository));

    try {
      Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ioException) {
      throw new RepositoryMetadataStoreException("Error copying ASC to the local repository", ioException);
    }
  }

  /**
   * Guards against two distinct signature files being registered under the same key. Merging is a no-op when both
   * instances point at the same file.
   *
   * @param metadata another {@link AscArtifactMetadata} claiming the same key
   * @throws IllegalStateException if {@code metadata} references a different file than this instance
   */
  @Override
  public void merge (ArtifactMetadata metadata) {

    if (!((AscArtifactMetadata)metadata).getPath().equals(path)) {
      throw new IllegalStateException("Cannot add two different pieces of metadata for key(" + getKey() + ")");
    }
  }

  /**
   * Legacy overload of {@link #merge(ArtifactMetadata)} that serves the same purpose for the older metadata API.
   *
   * @param metadata another {@link AscArtifactMetadata} claiming the same key
   * @throws IllegalStateException if {@code metadata} references a different file than this instance
   */
  @Override
  public void merge (org.apache.maven.repository.legacy.metadata.ArtifactMetadata metadata) {

    if (!((AscArtifactMetadata)metadata).getPath().equals(path)) {
      throw new IllegalStateException("Cannot add two different pieces of metadata for key(" + getKey() + ")");
    }
  }

  /**
   * Computes the canonical {@code artifactId-version[-classifier].extension.asc} filename for the wrapped artifact.
   *
   * @return the signature filename that Maven is expected to publish
   */
  private String getFilename () {

    StringBuilder nameBuilder = new StringBuilder(getArtifactId()).append("-").append(artifact.getVersion());

    if ((artifact.hasClassifier())) {
      nameBuilder.append("-").append(artifact.getClassifier());
    }

    return nameBuilder.append(".").append(artifact.getArtifactHandler().getExtension()).append(".asc").toString();
  }
}
