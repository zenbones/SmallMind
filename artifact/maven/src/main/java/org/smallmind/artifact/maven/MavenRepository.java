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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * Encapsulates Maven/Aether repository setup driven by user settings.xml, providing helper utilities for
 * establishing repository sessions, resolving artifacts, and handling mirrors, proxies, and authentication.
 */
public class MavenRepository {

  private static final RepositorySystem REPOSITORY_SYSTEM;
  private final Settings settings;
  private final ProxySelector proxySelector;
  private final MirrorSelector mirrorSelector;
  private final AuthenticationSelector authenticationSelector;
  private final Map<Object, Object> configProps = new HashMap<>();
  private final List<Profile> profileList;
  private final List<RemoteRepository> remoteRepositoryList;
  private final boolean offline;

  static {

    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

    locator.setServices(ModelBuilder.class, new DefaultModelBuilderFactory().newInstance());
    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    REPOSITORY_SYSTEM = locator.getService(RepositorySystem.class);
  }

  /**
   * Builds a repository instance using the default {@code ~/.m2/settings.xml}.
   *
   * @param repositoryId identifier used in the generated User-Agent header.
   * @param offline whether resolution should avoid remote access.
   * @throws SettingsBuildingException if the settings file cannot be read or is invalid.
   */
  public MavenRepository (String repositoryId, boolean offline)
    throws SettingsBuildingException {

    this(System.getProperty("user.home") + "/.m2", repositoryId, offline);
  }

  /**
   * Builds a repository instance using the supplied settings directory.
   *
   * @param settingsDirectory directory that contains {@code settings.xml}; defaults to {@code ~/.m2} when {@code null} or empty.
   * @param repositoryId identifier used in the generated User-Agent header.
   * @param offline whether resolution should avoid remote access.
   * @throws SettingsBuildingException if the settings file cannot be read or is invalid.
   */
  public MavenRepository (String settingsDirectory, String repositoryId, boolean offline)
    throws SettingsBuildingException {

    DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();

    this.offline = offline;

    request.setGlobalSettingsFile(Paths.get((((settingsDirectory == null) || settingsDirectory.isEmpty()) ? System.getProperty("user.home") + "/.m2" : settingsDirectory) + "/settings.xml").toFile());
    settings = new DefaultSettingsBuilderFactory().newInstance().build(request).getEffectiveSettings();

    profileList = settings.getProfiles();

    configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent(repositoryId));

    proxySelector = getProxySelector(settings);
    mirrorSelector = getMirrorSelector(settings);
    authenticationSelector = getAuthenticationSelector(settings);

    remoteRepositoryList = initRepositories(authenticationSelector, mirrorSelector, settings);
  }

  /**
   * Creates a new repository session configured with caching, proxy/mirror/authentication selectors, and local repository management.
   *
   * @return configured session ready for artifact resolution.
   */
  public DefaultRepositorySystemSession generateSession () {

    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    session.setOffline(offline);
    session.setCache(new DefaultRepositoryCache());

    session.setConfigProperties(configProps);

    if ((profileList != null) && (!profileList.isEmpty())) {
      for (Profile profile : settings.getProfiles()) {
        session.setUserProperties(profile.getProperties());
      }
    }

    session.setProxySelector(proxySelector);
    session.setMirrorSelector(mirrorSelector);
    session.setAuthenticationSelector(authenticationSelector);

    session.setLocalRepositoryManager(getLocalRepoMan(settings, REPOSITORY_SYSTEM, session));

    return session;
  }

  /**
   * Resolves the artifact described by the provided coordinates.
   *
   * @param session a repository session created by {@link #generateSession()}.
   * @param mavenCoordinate coordinates describing the target artifact.
   * @return the resolved artifact with file reference.
   * @throws ArtifactResolutionException if the artifact cannot be resolved from configured repositories.
   */
  public Artifact acquireArtifact (DefaultRepositorySystemSession session, MavenCoordinate mavenCoordinate)
    throws ArtifactResolutionException {

    return acquireArtifact(session, new DefaultArtifact(mavenCoordinate.getGroupId(), mavenCoordinate.getArtifactId(), mavenCoordinate.getClassifier(), mavenCoordinate.getExtension(), mavenCoordinate.getVersion()));
  }

  /**
   * Resolves the supplied artifact descriptor against configured repositories.
   *
   * @param session a repository session created by {@link #generateSession()}.
   * @param artifact the artifact to resolve.
   * @return the resolved artifact including its downloaded file.
   * @throws ArtifactResolutionException if resolution fails or no repository provides the artifact.
   */
  public Artifact acquireArtifact (DefaultRepositorySystemSession session, Artifact artifact)
    throws ArtifactResolutionException {

    ArtifactRequest artifactRequest = new ArtifactRequest();

    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(remoteRepositoryList);

    return REPOSITORY_SYSTEM.resolveArtifact(session, artifactRequest).getArtifact();
  }

  /**
   * Resolves an artifact and its transitive compile-time dependencies.
   *
   * @param session a repository session created by {@link #generateSession()}.
   * @param artifact the root artifact to resolve (must include coordinates and optionally a file).
   * @return array of resolved artifacts including the root and dependencies.
   * @throws DependencyCollectionException if dependency metadata cannot be collected.
   * @throws DependencyResolutionException if any dependency fails to resolve.
   */
  public Artifact[] resolve (final DefaultRepositorySystemSession session, Artifact artifact)
    throws DependencyCollectionException, DependencyResolutionException {

    final HashSet<DependencyNode> visitedSet = new HashSet<>();
    final HashSet<Artifact> artifactSet = new HashSet<>();
    Artifact[] artifacts;
    DependencyNode dependencyNode;

    dependencyNode = REPOSITORY_SYSTEM.collectDependencies(session, new CollectRequest().setRoot(new Dependency(artifact, "compile"))).getRoot();
    dependencyNode.accept(new DependencyVisitor() {

      @Override
      public boolean visitEnter (DependencyNode node) {

        if (visitedSet.add(node)) {

          Dependency dependency;

          if (((dependency = node.getDependency()) != null) && (!dependency.isOptional())) {

            Artifact artifact;

            if ((artifact = dependency.getArtifact()).getFile() == null) {
              try {
                artifact = acquireArtifact(session, artifact);
              } catch (ArtifactResolutionException artifactResolutionException) {
                throw new RuntimeException(artifactResolutionException);
              }
            }

            artifactSet.add(artifact);
          }

          return true;
        }

        return false;
      }

      @Override
      public boolean visitLeave (DependencyNode node) {

        return true;
      }
    });

    REPOSITORY_SYSTEM.resolveDependencies(session, new DependencyRequest().setRoot(dependencyNode));

    artifacts = new Artifact[artifactSet.size()];
    artifactSet.toArray(artifacts);

    return artifacts;
  }

  /**
   * Composes a user agent string for outgoing repository requests.
   *
   * @param repositoryId identifier to include in the header.
   * @return formatted user agent string.
   */
  private String getUserAgent (String repositoryId) {

    StringBuilder buffer = new StringBuilder();

    buffer.append("Maven-Repository/").append(repositoryId);
    buffer.append(" (");
    buffer.append("Java ").append(System.getProperty("java.version"));
    buffer.append("; ");
    buffer.append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version"));
    buffer.append(")");
    buffer.append(" Aether");

    return buffer.toString();
  }

  /**
   * Builds the remote repository list from active profiles and applies mirrors/authentication.
   *
   * @param authenticationSelector selector that supplies credentials per repository.
   * @param mirrorSelector selector used to coerce repositories to configured mirrors.
   * @param settings effective Maven settings.
   * @return list of remote repositories used for resolution.
   */
  private List<RemoteRepository> initRepositories (AuthenticationSelector authenticationSelector, MirrorSelector mirrorSelector, Settings settings) {

    LinkedList<RemoteRepository> remoteRepositoryList = new LinkedList<>();

    for (Profile profile : settings.getProfiles()) {
      if (isProfileActive(settings, profile)) {
        for (Repository repository : profile.getRepositories()) {
          constructRemoteRepository(authenticationSelector, mirrorSelector, remoteRepositoryList, repository);
        }
        for (Repository repository : profile.getPluginRepositories()) {
          constructRemoteRepository(authenticationSelector, mirrorSelector, remoteRepositoryList, repository);
        }
      }
    }

    return remoteRepositoryList;
  }

  /**
   * Determines if a settings profile is active based on explicit activation or defaults.
   *
   * @param settings effective Maven settings.
   * @param profile profile under consideration.
   * @return {@code true} when the profile is active.
   */
  private boolean isProfileActive (Settings settings, Profile profile) {

    return settings.getActiveProfiles().contains(profile.getId()) ||
             (profile.getActivation() != null && profile.getActivation().isActiveByDefault());
  }

  /**
   * Creates a remote repository instance (or its mirror) and adds it to the supplied list with authentication applied.
   *
   * @param authenticationSelector selector that supplies credentials per repository.
   * @param mirrorSelector selector used to coerce repositories to configured mirrors.
   * @param remoteRepositoryList collection being populated.
   * @param repository repository settings entry to translate.
   */
  private void constructRemoteRepository (AuthenticationSelector authenticationSelector, MirrorSelector mirrorSelector, List<RemoteRepository> remoteRepositoryList, Repository repository) {

    RemoteRepository remoteRepository = new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl())
                                          .setReleasePolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                                          .setSnapshotPolicy(new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_ALWAYS, RepositoryPolicy.CHECKSUM_POLICY_WARN))
                                          .build();
    RemoteRepository mirrorRepository = mirrorSelector.getMirror(remoteRepository);
    RemoteRepository coercedRepository = (mirrorRepository != null) ? mirrorRepository : remoteRepository;

    remoteRepositoryList.add(new RemoteRepository.Builder(coercedRepository).setAuthentication(authenticationSelector.getAuthentication(coercedRepository)).build());
  }

  /**
   * Builds an Aether proxy selector from Maven settings.
   *
   * @param settings effective Maven settings.
   * @return proxy selector containing configured proxies.
   */
  private ProxySelector getProxySelector (Settings settings) {

    DefaultProxySelector selector = new DefaultProxySelector();

    for (Proxy proxy : settings.getProxies()) {
      AuthenticationBuilder auth = new AuthenticationBuilder();
      auth.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
      selector.add(new org.eclipse.aether.repository.Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), auth.build()), proxy.getNonProxyHosts());
    }

    return selector;
  }

  /**
   * Builds a mirror selector from Maven settings.
   *
   * @param settings effective Maven settings.
   * @return selector able to resolve mirrors for remote repositories.
   */
  private MirrorSelector getMirrorSelector (Settings settings) {

    DefaultMirrorSelector selector = new DefaultMirrorSelector();

    for (Mirror mirror : settings.getMirrors()) {
      selector.add(String.valueOf(mirror.getId()), mirror.getUrl(), mirror.getLayout(), false,
        mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
    }

    return selector;
  }

  /**
   * Builds an authentication selector from Maven server credentials, wrapped with conservative fallbacks.
   *
   * @param settings effective Maven settings.
   * @return selector that supplies authentication per server id.
   */
  private AuthenticationSelector getAuthenticationSelector (Settings settings) {

    DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();

    for (Server server : settings.getServers()) {
      AuthenticationBuilder auth = new AuthenticationBuilder();
      auth.addUsername(server.getUsername()).addPassword(server.getPassword());
      auth.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
      selector.add(server.getId(), auth.build());
    }

    return new ConservativeAuthenticationSelector(selector);
  }

  /**
   * Creates a local repository manager anchored at the configured local repository directory.
   *
   * @param settings effective Maven settings.
   * @param repositorySystem repository system used to create the manager.
   * @param repositorySystemSession the session associated with the manager.
   * @return local repository manager for caching artifacts.
   */
  private LocalRepositoryManager getLocalRepoMan (Settings settings, RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession) {

    LocalRepository repo = new LocalRepository(getDefaultLocalRepoDir(settings).toFile());

    return repositorySystem.newLocalRepositoryManager(repositorySystemSession, repo);
  }

  /**
   * Resolves the default local repository directory from settings, falling back to {@code ~/.m2/repository}.
   *
   * @param settings effective Maven settings.
   * @return path to the local repository.
   */
  private Path getDefaultLocalRepoDir (Settings settings) {

    if (settings.getLocalRepository() != null) {
      return Paths.get(settings.getLocalRepository());
    }

    return Paths.get(System.getProperty("user.home") + "/.m2/repository");
  }
}
