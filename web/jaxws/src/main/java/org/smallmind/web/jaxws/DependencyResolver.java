/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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