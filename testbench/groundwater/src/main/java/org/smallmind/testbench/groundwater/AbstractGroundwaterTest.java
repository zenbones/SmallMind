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
package org.smallmind.testbench.groundwater;

import org.smallmind.testbench.condition.ContainerAbsentTestCondition;
import org.smallmind.testbench.condition.GreenmailAbsentTestCondition;
import org.smallmind.testbench.condition.TestConditions;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.docker.DockerApplications;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * TestNG base class that brings a set of {@link DockerApplication} fixtures up before a test class
 * runs and tears them down afterward. Subclasses pass the applications they need to the constructor;
 * the container lifecycle is then managed by the {@link BeforeClass} and {@link AfterClass} hooks.
 *
 * <p>Before starting anything, {@link #beforeClass()} waits — via
 * {@link org.smallmind.testbench.condition.TestConditions#parallel}, with a thirty-second budget —
 * for any leftover Kafka, MongoDB, MySQL, RabbitMQ, or Memcached containers from a prior run to be
 * gone and for the GreenMail port {@code 8025} to be free, so a stale environment fails fast rather
 * than colliding with the new fixtures.
 *
 * <p>The container lifecycle is owned by this class: containers it starts in {@link #beforeClass()}
 * are the ones it stops in {@link #afterClass()}. A {@code null} or empty application list is
 * permitted and turns both hooks into no-ops.
 */
public class AbstractGroundwaterTest {

  private final DockerApplication[] dockerApplications;
  private final String[] dockerContainerIds;

  /**
   * Records the applications this test class depends on and sizes the container-id tracking array
   * accordingly. No containers are started here; that happens in {@link #beforeClass()}.
   *
   * @param dockerApplications the application fixtures to start before the class and stop after it;
   * may be {@code null} or empty to manage no containers
   */
  public AbstractGroundwaterTest (DockerApplication... dockerApplications) {

    this.dockerApplications = dockerApplications;

    dockerContainerIds = new String[(dockerApplications == null) ? 0 : dockerApplications.length];
  }

  /**
   * TestNG hook that verifies a clean environment and then starts every configured application,
   * retaining each container id for teardown. Each container is started under its application's
   * default name, with the concrete test class's simple name recorded as the owning-test identifier
   * (the {@code test} label and {@code Test} environment variable). Does nothing when no applications
   * were configured.
   *
   * @throws Exception if the pre-start absence checks time out, or if any container fails to start
   * or become ready
   */
  @BeforeClass
  public void beforeClass ()
    throws Exception {

    if (dockerApplications != null) {

      TestConditions.parallel(30,
        new ContainerAbsentTestCondition(DockerApplication.KAFKA.getName(), DockerApplication.MONGODB.getName(), DockerApplication.MYSQL.getName(), DockerApplication.RABBITMQ.name(), DockerApplication.MEMCACHED.name()),
        new GreenmailAbsentTestCondition(8025)
      );

      int index = 0;

      for (DockerApplication dockerApplication : dockerApplications) {
        dockerContainerIds[index++] = DockerApplications.start(this.getClass().getSimpleName(), dockerApplication);
      }
    }
  }

  /**
   * TestNG hook that stops and removes every container started in {@link #beforeClass()}. Does
   * nothing when no applications were configured.
   *
   * @throws Exception if stopping or removing a container fails
   */
  @AfterClass
  public void afterClass ()
    throws Exception {

    if (dockerApplications != null) {
      for (String dockerContainerId : dockerContainerIds) {
        DockerApplications.stop(dockerContainerId);
      }
    }
  }
}
