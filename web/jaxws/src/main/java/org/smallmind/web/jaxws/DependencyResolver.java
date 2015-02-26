package org.smallmind.web.jaxws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;

final class DependencyResolver {

  public static DependencyResult resolve (CollectRequest collectRequest, DependencyFilter filter, List<RemoteRepository> remoteRepos, RepositorySystem repoSystem, RepositorySystemSession repoSession) throws DependencyResolutionException {

    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, filter);

    return repoSystem.resolveDependencies(repoSession, dependencyRequest);
  }

  public static DependencyResult resolve (org.apache.maven.artifact.Artifact artifact, DependencyFilter filter, List<RemoteRepository> remoteRepos, RepositorySystem repoSystem, RepositorySystemSession repoSession)
    throws DependencyResolutionException {

    Artifact a = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType(), artifact.getVersion());
    Dependency dependency = new Dependency(a, null);
    CollectRequest collectRequest = new CollectRequest(dependency, remoteRepos);

    return resolve(collectRequest, filter, remoteRepos, repoSystem, repoSession);
  }

  public static DependencyResult resolve (org.apache.maven.model.Dependency dependency, DependencyFilter filter, List<RemoteRepository> remoteRepos, RepositorySystem repoSystem, RepositorySystemSession repoSession)
    throws DependencyResolutionException {

    CollectRequest collectRequest = new CollectRequest(createDependency(dependency), remoteRepos);

    return resolve(collectRequest, filter, remoteRepos, repoSystem, repoSession);
  }

  private static Dependency createDependency (org.apache.maven.model.Dependency d) {

    Collection<Exclusion> toExclude = new ArrayList<Exclusion>();

    for (org.apache.maven.model.Exclusion e : d.getExclusions()) {
      toExclude.add(new Exclusion(e.getGroupId(), e.getArtifactId(), null, "jar"));
    }

    Artifact artifact = new DefaultArtifact(d.getGroupId(), d.getArtifactId(), "jar", d.getVersion());

    return new Dependency(artifact, d.getScope(), d.isOptional(), toExclude);
  }
}