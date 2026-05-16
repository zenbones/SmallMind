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
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
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
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;

/**
 * Facade over the Eclipse Aether repository system that initialises itself from a Maven
 * {@code settings.xml} file and exposes artifact resolution operations.
 *
 * <p>Construction reads and processes the settings file once, deriving proxy, mirror, and
 * authentication selectors and the list of remote repositories from active profiles.  The
 * resolved selectors are shared across all sessions produced by {@link #generateSession()},
 * so a single {@code MavenRepository} instance can be reused for multiple resolution cycles
 * without re-parsing settings.
 *
 * <p>Repository and snapshot policies are set to {@code UPDATE_POLICY_ALWAYS} and
 * {@code CHECKSUM_POLICY_WARN} for all remote repositories, ensuring that snapshot
 * re-deployments are picked up on every resolution request.
 *
 * <p>Instances of this class are thread-safe once constructed; {@link #generateSession()}
 * returns a fresh session per call and is not synchronized.  The {@link #resolve} method
 * is likewise independent across calls because it creates its own local state.
 */
public class MavenRepository {

  private final RepositorySystem repositorySystem;
  private final Settings settings;
  private final ProxySelector proxySelector;
  private final MirrorSelector mirrorSelector;
  private final AuthenticationSelector authenticationSelector;
  private final Map<Object, Object> configProps = new HashMap<>();
  private final List<Profile> profileList;
  private final List<RemoteRepository> remoteRepositoryList;
  private final boolean offline;

  /**
   * Builds a repository using the Maven settings file found at {@code ~/.m2/settings.xml}.
   *
   * @param repositoryId short identifier embedded in the {@code User-Agent} header sent with
   *                     remote repository requests (e.g. the application name)
   * @param offline      {@code true} to disable all remote repository access and restrict
   *                     resolution to locally cached artifacts
   * @throws SettingsBuildingException if {@code ~/.m2/settings.xml} cannot be read, parsed,
   *                                   or merged with any global settings
   */
  public MavenRepository (String repositoryId, boolean offline)
    throws SettingsBuildingException {

    this(System.getProperty("user.home") + "/.m2", repositoryId, offline);
  }

  /**
   * Builds a repository using the Maven settings file found in the given directory.
   *
   * <p>If {@code settingsDirectory} is {@code null} or empty the constructor falls back to
   * {@code ~/.m2}.
   *
   * @param settingsDirectory directory that contains {@code settings.xml}; {@code null} or
   *                          empty string both fall back to {@code ~/.m2}
   * @param repositoryId      short identifier embedded in the {@code User-Agent} header
   * @param offline           {@code true} to restrict resolution to locally cached artifacts
   * @throws SettingsBuildingException if {@code settings.xml} cannot be read or is syntactically
   *                                   invalid
   */
  public MavenRepository (String settingsDirectory, String repositoryId, boolean offline)
    throws SettingsBuildingException {

    DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();

    this.offline = offline;

    repositorySystem = new RepositorySystemSupplier().get();

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
   * Creates a new Aether {@link DefaultRepositorySystemSession} ready for artifact resolution.
   *
   * <p>Each call returns an independent session with:
   * <ul>
   *   <li>offline flag set from the constructor argument</li>
   *   <li>a fresh {@link DefaultRepositoryCache} scoped to this session</li>
   *   <li>user properties merged from all active profile {@code properties} blocks</li>
   *   <li>proxy, mirror, and authentication selectors derived from settings</li>
   *   <li>a local repository manager anchored at the directory declared in settings
   *       (or {@code ~/.m2/repository} as fallback)</li>
   * </ul>
   *
   * @return a fully configured session; callers must not share a session across concurrent threads
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

    session.setLocalRepositoryManager(getLocalRepoMan(settings, repositorySystem, session));

    return session;
  }

  /**
   * Resolves the artifact described by a {@link MavenCoordinate} against all configured remote
   * repositories, downloading it to the local repository cache if necessary.
   *
   * <p>Delegates to {@link #acquireArtifact(DefaultRepositorySystemSession, Artifact)} after
   * constructing a {@link DefaultArtifact} from the coordinate fields.
   *
   * @param session         session produced by {@link #generateSession()}; must not be shared
   *                        concurrently
   * @param mavenCoordinate coordinate describing the artifact to fetch; all fields must be
   *                        non-{@code null} except classifier
   * @return the resolved artifact with its local file reference populated
   * @throws ArtifactResolutionException if the artifact cannot be found in any configured
   *                                     repository, or if a network or authentication error
   *                                     occurs during download
   */
  public Artifact acquireArtifact (DefaultRepositorySystemSession session, MavenCoordinate mavenCoordinate)
    throws ArtifactResolutionException {

    return acquireArtifact(session, new DefaultArtifact(mavenCoordinate.getGroupId(), mavenCoordinate.getArtifactId(), mavenCoordinate.getClassifier(), mavenCoordinate.getExtension(), mavenCoordinate.getVersion()));
  }

  /**
   * Resolves a pre-built {@link Artifact} descriptor against all configured remote repositories,
   * downloading it to the local repository cache if necessary.
   *
   * @param session  session produced by {@link #generateSession()}; must not be shared
   *                 concurrently
   * @param artifact artifact descriptor to resolve; need not have a local file reference
   * @return the resolved artifact with its local file reference populated
   * @throws ArtifactResolutionException if no configured repository provides the artifact, or if
   *                                     a network, checksum, or authentication error occurs
   */
  public Artifact acquireArtifact (DefaultRepositorySystemSession session, Artifact artifact)
    throws ArtifactResolutionException {

    ArtifactRequest artifactRequest = new ArtifactRequest();

    artifactRequest.setArtifact(artifact);
    artifactRequest.setRepositories(remoteRepositoryList);

    return repositorySystem.resolveArtifact(session, artifactRequest).getArtifact();
  }

  /**
   * Resolves an artifact and all of its non-optional transitive compile-scope dependencies.
   *
   * <p>The dependency graph is walked depth-first using a {@link DependencyVisitor}.  Any node
   * whose dependency is marked optional is silently skipped.  Nodes already visited (by reference)
   * are also skipped to avoid processing duplicates in diamond dependency graphs.  For each
   * non-optional dependency that lacks a local file, {@link #acquireArtifact} is called to
   * download it.
   *
   * <p>The resulting array always includes the root artifact.  Duplicate artifacts that appear
   * across different branches of the graph are deduplicated via an intermediate {@link HashSet}.
   *
   * @param session  session produced by {@link #generateSession()}
   * @param artifact root artifact for which to collect and resolve the full dependency closure;
   *                 must have its groupId, artifactId, version, and extension populated
   * @return array of resolved artifacts constituting the full compile-scope dependency closure;
   * order is not guaranteed
   * @throws DependencyCollectionException if the dependency metadata (POMs) for any reachable
   *                                       node cannot be retrieved or parsed
   * @throws DependencyResolutionException if Aether's final resolution pass fails for any node
   * @throws RuntimeException              wrapping an {@link ArtifactResolutionException} if an
   *                                       individual artifact download fails during graph traversal
   */
  public Artifact[] resolve (final DefaultRepositorySystemSession session, Artifact artifact)
    throws DependencyCollectionException, DependencyResolutionException {

    final HashSet<DependencyNode> visitedSet = new HashSet<>();
    final HashSet<Artifact> artifactSet = new HashSet<>();
    Artifact[] artifacts;
    DependencyNode dependencyNode;

    dependencyNode = repositorySystem.collectDependencies(session, new CollectRequest().setRoot(new Dependency(artifact, "compile")).setRepositories(remoteRepositoryList)).getRoot();
    dependencyNode.accept(new DependencyVisitor() {

      @Override
      public boolean visitEnter (DependencyNode node) {

        if (visitedSet.add(node)) {

          Dependency dependency;

          if (((dependency = node.getDependency()) != null) && (!dependency.isOptional())) {

            Artifact artifact;

            if ((artifact = dependency.getArtifact()).getFile() == null) {
//            if ((artifact = dependency.getArtifact()).getPath() == null) {
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

    repositorySystem.resolveDependencies(session, new DependencyRequest().setRoot(dependencyNode));

    artifacts = new Artifact[artifactSet.size()];
    artifactSet.toArray(artifacts);

    return artifacts;
  }

  /**
   * Composes the HTTP {@code User-Agent} header value sent with remote repository requests.
   *
   * <p>The header includes the repository identifier, JVM version, and OS name and version,
   * matching the format used by standard Maven tooling.
   *
   * @param repositoryId identifier to embed as the agent name
   * @return formatted User-Agent string
   */
  private String getUserAgent (String repositoryId) {

    String buffer = "Maven-Repository/" + repositoryId +
                      " (" +
                      "Java " + System.getProperty("java.version") +
                      "; " +
                      System.getProperty("os.name") + " " + System.getProperty("os.version") +
                      ")" +
                      " Aether";

    return buffer;
  }

  /**
   * Enumerates the remote repositories declared across all active profiles in the given settings,
   * applying mirror redirection and authentication before adding each repository to the list.
   *
   * <p>Both {@code repositories} and {@code pluginRepositories} sections of each active profile
   * are included.
   *
   * @param authenticationSelector selector that supplies {@link org.eclipse.aether.repository.Authentication}
   *                               for a given repository id
   * @param mirrorSelector         selector that maps a repository to its configured mirror,
   *                               or returns {@code null} when no mirror applies
   * @param settings               effective Maven settings from which profile and mirror
   *                               declarations are read
   * @return list of remote repositories ready for use in artifact requests
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
   * Determines whether a profile should contribute repositories to the active set.
   *
   * <p>A profile is considered active if its id appears in {@link Settings#getActiveProfiles()},
   * or if its activation block exists and has {@code activeByDefault} set to {@code true}.
   *
   * @param settings effective Maven settings supplying the active-profile list
   * @param profile  profile to evaluate
   * @return {@code true} if the profile is active by explicit selection or by default activation
   */
  private boolean isProfileActive (Settings settings, Profile profile) {

    return settings.getActiveProfiles().contains(profile.getId()) ||
             (profile.getActivation() != null && profile.getActivation().isActiveByDefault());
  }

  /**
   * Translates a settings {@link Repository} into an Aether {@link RemoteRepository}, applies
   * mirror redirection if a matching mirror is configured, attaches authentication, and appends
   * the result to the supplied list.
   *
   * <p>Both release and snapshot policies are set to {@code UPDATE_POLICY_ALWAYS} /
   * {@code CHECKSUM_POLICY_WARN} so that re-deployed snapshots are always refetched.
   *
   * @param authenticationSelector selector that returns authentication for the effective
   *                               repository id (which may be the mirror's id after redirection)
   * @param mirrorSelector         selector that returns a mirror for the original repository,
   *                               or {@code null} when no mirror applies
   * @param remoteRepositoryList   mutable list to which the constructed repository is appended
   * @param repository             settings repository entry to translate
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
   * Builds an Aether {@link ProxySelector} populated from the {@code proxies} block of Maven
   * settings.  Each proxy entry contributes optional username/password authentication and a
   * non-proxy-hosts exclusion pattern.
   *
   * @param settings effective Maven settings from which proxy declarations are read
   * @return selector that returns the appropriate proxy (or {@code null}) for a given repository URL
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
   * Builds an Aether {@link MirrorSelector} populated from the {@code mirrors} block of Maven
   * settings.
   *
   * @param settings effective Maven settings from which mirror declarations are read
   * @return selector that maps an original repository to its configured mirror, or returns
   * {@code null} when no mirror matches
   */
  private MirrorSelector getMirrorSelector (Settings settings) {

    DefaultMirrorSelector selector = new DefaultMirrorSelector();

    for (Mirror mirror : settings.getMirrors()) {
      selector.add(String.valueOf(mirror.getId()), mirror.getUrl(), mirror.getLayout(), false, false, mirror.getMirrorOf(), mirror.getMirrorOfLayouts());
    }

    return selector;
  }

  /**
   * Builds an Aether {@link AuthenticationSelector} from the {@code servers} block of Maven
   * settings, wrapping the result in a {@link ConservativeAuthenticationSelector} to avoid
   * forwarding credentials to repositories that do not require them.
   *
   * <p>Each server entry may supply a username/password pair, an SSH private key path and
   * passphrase, or both.
   *
   * @param settings effective Maven settings from which server credential declarations are read
   * @return selector that returns appropriate authentication for a given repository id
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
   * Creates a {@link LocalRepositoryManager} anchored at the local repository directory
   * declared in settings, falling back to {@code ~/.m2/repository} when the setting is absent.
   *
   * @param settings                effective Maven settings queried for {@code localRepository}
   * @param repositorySystem        system used to instantiate the manager
   * @param repositorySystemSession session to which the manager will be bound
   * @return manager responsible for reading from and writing to the local artifact cache
   */
  private LocalRepositoryManager getLocalRepoMan (Settings settings, RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession) {

    LocalRepository repo = new LocalRepository(getDefaultLocalRepoDir(settings).toFile());
//    LocalRepository repo = new LocalRepository(getDefaultLocalRepoDir(settings));

    return repositorySystem.newLocalRepositoryManager(repositorySystemSession, repo);
  }

  /**
   * Resolves the path to the local Maven repository directory.
   *
   * <p>Uses the {@code localRepository} setting when present; otherwise falls back to
   * {@code ~/.m2/repository}.
   *
   * @param settings effective Maven settings to inspect
   * @return absolute path to the local repository directory
   */
  private Path getDefaultLocalRepoDir (Settings settings) {

    if (settings.getLocalRepository() != null) {
      return Paths.get(settings.getLocalRepository());
    }

    return Paths.get(System.getProperty("user.home") + "/.m2/repository");
  }
}
