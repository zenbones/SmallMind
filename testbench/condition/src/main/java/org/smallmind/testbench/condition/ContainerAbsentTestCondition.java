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
package org.smallmind.testbench.condition;

import java.util.Arrays;
import java.util.List;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import org.smallmind.nutsnbolts.util.MutationUtility;
import org.smallmind.testbench.docker.DockerClientUtility;

/**
 * A {@link TestCondition} satisfied only when no Docker container matching any of the given names is
 * present on the daemon. It is typically polled before a test starts its own containers, to ensure a
 * previous run left nothing behind that would collide on container name or bound port.
 */
public class ContainerAbsentTestCondition implements TestCondition {

  private final String[] names;

  /**
   * Creates a condition that waits for the named containers to be gone.
   *
   * @param names the container names to watch for; the condition is satisfied only when none of
   * them are present
   */
  public ContainerAbsentTestCondition (String... names) {

    this.names = names;
  }

  /**
   * Queries the Docker daemon for any container matching the configured names.
   *
   * @return {@code null} when no matching container is present, or a
   * {@link MessageTestConditionFailure} naming the container that is still around
   * @throws Exception if a Docker client cannot be created or the container listing fails
   */
  @Override
  public TestConditionFailure test ()
    throws Exception {

    try (DockerClient dockerClient = DockerClientUtility.createClient()) {

      List<Container> remainingContainerList;

      if ((remainingContainerList = dockerClient.listContainersCmd().withNameFilter(Arrays.asList(names)).withLimit(1).exec()).isEmpty()) {

        return null;
      } else {

        return new MessageTestConditionFailure(Arrays.toString(MutationUtility.toArray(remainingContainerList, String.class, container -> container.getNames()[0])));
      }
    }
  }
}
