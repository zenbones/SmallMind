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
 * Enum of pre-configured Docker application templates used by the science testbench
 * test environment. Each constant encapsulates everything needed to run the application
 * as a test fixture: the Docker image reference, container name, exposed ports, optional
 * command overrides, environment variables, optional volume binds, and optional tmpfs
 * configuration.
 *
 * <p>Defined applications:
 * <ul>
 *   <li>{@link #KAFKA} — Confluent Platform Kafka running in KRaft mode (no ZooKeeper)
 *   <li>{@link #MEMCACHED} — Memcached cache daemon
 *   <li>{@link #MINIO} — MinIO object storage server with web console
 *   <li>{@link #MONGODB} — MongoDB with a preconfigured root user
 *   <li>{@link #MYSQL} — MySQL with the data directory on a tmpfs for fast ephemeral storage
 *   <li>{@link #RABBITMQ} — RabbitMQ message broker
 *   <li>{@link #REGISTRY} — Docker Distribution registry with OTEL tracing disabled
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
   * Constructs a {@code DockerApplication} constant with its full configuration.
   *
   * @param name        the container name used when creating the container
   * @param image       the Docker image reference (e.g., {@code "mysql:latest"})
   * @param ports       the service ports exposed by the container
   * @param commands    optional command arguments that override the image's default CMD; may be {@code null}
   * @param environment optional environment variable strings in {@code KEY=VALUE} format; may be {@code null}
   * @param mounts      optional mount descriptors (e.g., bind mounts); may be {@code null}
   * @param volumes     optional volume bind mounts; may be {@code null}
   * @param tmpFs       optional tmpfs mount configuration; may be {@code null}
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
   * Returns the default container name for this application.
   *
   * @return the container name; never {@code null}
   */
  public String getName () {

    return name;
  }

  /**
   * Returns the Docker image reference used to create the container.
   *
   * @return the image reference (e.g., {@code "mysql:latest"}); never {@code null}
   */
  public String getImage () {

    return image;
  }

  /**
   * Returns the raw array of service port numbers as defined in the enum constant.
   *
   * @return the port numbers; never {@code null}
   */
  public int[] getRawPorts () {

    return ports;
  }

  /**
   * Wraps each raw port number in a {@link DockerPort} descriptor and returns the result.
   * External port overrides are not set; callers must configure them separately if needed.
   *
   * @return an array of {@link DockerPort} instances; never {@code null}
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
   * Returns the optional command override array passed to the container at startup.
   *
   * @return the command arguments, or {@code null} if no command override was specified
   */
  public String[] getCommands () {

    return commands;
  }

  /**
   * Returns the full environment variable array for the container, appending a
   * {@code Test=<test>} entry to allow containers to identify their owning test.
   *
   * @param test the test identifier to embed; must not be {@code null}
   * @return the environment variable strings in {@code KEY=VALUE} format; never {@code null}
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
   * Returns the optional mount descriptors for this application.
   *
   * @return the mount descriptors, or {@code null} if no mounts were configured
   */
  public DockerMount[] getMounts () {

    return mounts;
  }

  /**
   * Returns the optional volume bind mount descriptors for this application.
   *
   * @return the volume descriptors, or {@code null} if no volumes were configured
   */
  public DockerVolume[] getVolumes () {

    return volumes;
  }

  /**
   * Returns the optional tmpfs mount configuration for this application.
   *
   * @return the tmpfs descriptor, or {@code null} if no tmpfs was configured
   */
  public DockerTmpFs getTmpFs () {

    return tmpFs;
  }
}
