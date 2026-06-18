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
package org.smallmind.testbench.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

/**
 * Factory for the {@link DockerClient} used by the rest of this package to talk to the Docker
 * daemon. The daemon location is taken from the {@code DOCKER_HOST} environment variable, falling
 * back to {@code tcp://localhost:2375} when that variable is unset.
 */
public class DockerClientUtility {

  /**
   * Creates a new {@link DockerClient} over an Apache HTTP/5 transport, configured from
   * {@code DOCKER_HOST} (or the {@code tcp://localhost:2375} default). The client is
   * {@link AutoCloseable}; ownership transfers to the caller, who must close it when finished —
   * typically with a try-with-resources block.
   *
   * @return a ready-to-use {@link DockerClient}
   */
  public static DockerClient createClient () {

    String dockerHost = System.getenv("DOCKER_HOST");
    DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost((dockerHost == null) ? "tcp://localhost:2375" : dockerHost).build();

    return DockerClientImpl.getInstance(dockerClientConfig, new ApacheDockerHttpClient.Builder()
                                                              .dockerHost(dockerClientConfig.getDockerHost())
                                                              .sslConfig(dockerClientConfig.getSSLConfig())
                                                              .build());
  }
}
