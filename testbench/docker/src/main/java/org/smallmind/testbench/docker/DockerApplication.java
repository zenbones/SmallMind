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

/**
 * Catalog of ready-to-run Docker container fixtures for testbench integration tests. Each constant
 * bundles the complete recipe for one backing service: its image reference, container name, exposed
 * service ports, optional command override, environment variables, optional bind mounts, optional
 * volume binds, and optional tmpfs mount. {@link DockerApplications} consumes these constants to
 * create, start, and stop the corresponding containers.
 *
 * <p>The catalog covers:
 * <ul>
 *   <li>{@link #KAFKA} — Confluent Platform Kafka in KRaft mode (no ZooKeeper), external port 9094
 *   <li>{@link #MEMCACHED} — a Memcached cache daemon on port 11211
 *   <li>{@link #MINIO} — a MinIO object-storage server with its web console on ports 9000 and 9001
 *   <li>{@link #MONGODB} — MongoDB on port 27017 with a preconfigured {@code root} user
 *   <li>{@link #MYSQL} — MySQL on port 3306 with its data directory on a tmpfs for fast, ephemeral storage
 *   <li>{@link #RABBITMQ} — a RabbitMQ broker on ports 5671 and 5672
 *   <li>{@link #REGISTRY} — a Docker Distribution registry on port 5000 with OpenTelemetry tracing disabled
 * </ul>
 */
public enum DockerApplication {

  KAFKA("kafka", "confluentinc/cp-kafka:latest", new int[] {9094}, null, new String[] {"KAFKA_NODE_ID=1", "KAFKA_PROCESS_ROLES=broker,controller", "KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093", "KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094", "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092,EXTERNAL://localhost:9094", "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,PLAINTEXT:PLAINTEXT", "KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER", "KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT", "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"}, null, null, null),
  MEMCACHED("memcached", "memcached:latest", new int[] {11211}, null, null, null, null, null),
  MINIO("minio", "minio/minio:latest", new int[] {9000, 9001}, new String[] {"server", "/data", "--console-address", ":9001"}, new String[] {"MINIO_ROOT_USER=access_key", "MINIO_ROOT_PASSWORD=secret_key"}, null, null, null),
  MONGODB("mongodb", "mongo:latest", new int[] {27017}, null, new String[] {"MONGO_INITDB_ROOT_USERNAME=root", "MONGO_INITDB_ROOT_PASSWORD=secret"}, null, null, null),
  MYSQL("/mysql", "mysql:latest", new int[] {3306}, null, new String[] {"MYSQL_ROOT_PASSWORD=secret"}, null, null, DockerTmpFs.create("/var/lib/mysql", "rw,noexec,nosuid,size=1024m")),
  RABBITMQ("rabbitmq", "rabbitmq:latest", new int[] {5671, 5672}, null, null, null, null, null),
  REGISTRY("registry", "registry:latest", new int[] {5000}, null, new String[] {"OTEL_TRACES_EXPORTER=none"}, null, null, null);

  private final org.smallmind.testbench.docker.DockerTmpFs tmpFs;
  private final org.smallmind.testbench.docker.DockerMount[] mounts;
  private final org.smallmind.testbench.docker.DockerVolume[] volumes;
  private final String name;
  private final String image;
  private final int[] ports;
  private final String[] commands;
  private final String[] environment;

  /**
   * Defines a catalog constant from its full container recipe.
   *
   * @param name the default container name
   * @param image the Docker image reference, such as {@code "mysql:latest"}
   * @param ports the container-internal service ports to expose
   * @param commands an optional command override replacing the image's default {@code CMD}, or {@code null}
   * @param environment optional {@code KEY=VALUE} environment entries, or {@code null}
   * @param mounts optional bind-mount descriptors, or {@code null}
   * @param volumes optional volume-bind descriptors, or {@code null}
   * @param tmpFs an optional tmpfs mount, or {@code null}
   */
  DockerApplication (String name, String image, int[] ports, String[] commands, String[] environment, org.smallmind.testbench.docker.DockerMount[] mounts, org.smallmind.testbench.docker.DockerVolume[] volumes, org.smallmind.testbench.docker.DockerTmpFs tmpFs) {

    this.name = name;
    this.image = image;
    this.ports = ports;
    this.commands = commands;
    this.environment = environment;
    this.mounts = mounts;
    this.volumes = volumes;
    this.tmpFs = tmpFs;
  }

  /**
   * Returns this application's default container name.
   *
   * @return the container name; never {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the Docker image reference the container is created from.
   *
   * @return the image reference, such as {@code "mysql:latest"}; never {@code null}
   */
  public String getImage () {

    return image;
  }

  /**
   * Returns this application's service port numbers as raw {@code int}s, without wrapping them in
   * {@link DockerPort} descriptors.
   *
   * @return the container-internal service ports; never {@code null}
   */
  public int[] getRawPorts () {

    return ports;
  }

  /**
   * Returns this application's service ports as fresh {@link DockerPort} descriptors. Each descriptor
   * carries only the service port; no external host-port override is set, so by default the service
   * port is also used on the host. Callers wanting a different host binding must set it on the
   * returned descriptors.
   *
   * @return a new array of {@link DockerPort} descriptors; never {@code null}
   */
  public DockerPort[] getPorts () {

    org.smallmind.testbench.docker.DockerPort[] dockerPorts = new org.smallmind.testbench.docker.DockerPort[ports.length];
    int index = 0;

    for (int port : ports) {
      dockerPorts[index++] = new org.smallmind.testbench.docker.DockerPort(port);
    }

    return dockerPorts;
  }

  /**
   * Returns the command override applied to the container at startup, replacing the image's default
   * {@code CMD}.
   *
   * @return the command arguments, or {@code null} when the image default is used
   */
  public String[] getCommands () {

    return commands;
  }

  /**
   * Returns the container's environment, this application's configured {@code KEY=VALUE} entries plus
   * a trailing {@code Test=<test>} entry that lets a container identify the test that launched it.
   * A fresh array is returned on each call; the configured entries are not mutated.
   *
   * @param test the identifier of the owning test, embedded as the {@code Test} variable
   * @return the {@code KEY=VALUE} environment entries; never {@code null}
   */
  public String[] getEnvironment (String test) {

    String[] Environment = new String[(environment == null) ? 1 : environment.length + 1];

    if (environment != null) {
      System.arraycopy(environment, 0, Environment, 0, environment.length);
    }
    Environment[Environment.length - 1] = "Test=" + test;

    return Environment;
  }

  /**
   * Returns this application's bind-mount descriptors.
   *
   * @return the mount descriptors, or {@code null} when none were configured
   */
  public DockerMount[] getMounts () {

    return mounts;
  }

  /**
   * Returns this application's volume-bind descriptors.
   *
   * @return the volume descriptors, or {@code null} when none were configured
   */
  public DockerVolume[] getVolumes () {

    return volumes;
  }

  /**
   * Returns this application's tmpfs mount.
   *
   * @return the tmpfs descriptor, or {@code null} when none was configured
   */
  public DockerTmpFs getTmpFs () {

    return tmpFs;
  }
}
