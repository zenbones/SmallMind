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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.ConflictException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Mount;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Static lifecycle operations for the container fixtures described by {@link DockerApplication}.
 * The image is pulled automatically when it is not already present locally, and the port bindings,
 * bind mounts, volume binds, and tmpfs mount declared by the application are applied when the
 * container is created.
 *
 * <p>{@link #start} blocks until the container is running <em>and</em> every service port is
 * accepting TCP connections, so callers receive a fixture that is ready to use without writing
 * their own readiness polling. {@link #stop} kills and removes a container, tolerating one that is
 * already stopped or already gone.
 */
public class DockerApplications {

  /**
   * Starts the application under its default container name, blocking until every service port
   * accepts connections. Equivalent to {@link #start(String, String, DockerApplication, DockerPort...)}
   * with the application's own name.
   *
   * @param test an identifier for the owning test, recorded as a container label and as the
   * {@code Test} environment variable
   * @param application the fixture to start
   * @param ports optional port overrides; when {@code null} or empty the application's default ports
   * are used
   * @return the id of the started container, for later passing to {@link #stop}
   * @throws IOException if image inspection or pull, container creation, or a port readiness probe fails
   * @throws InterruptedException if the thread is interrupted while pulling the image or awaiting readiness
   */
  public static String start (String test, DockerApplication application, DockerPort... ports)
    throws IOException, InterruptedException {

    return start(test, application.getName(), application, ports);
  }

  /**
   * Starts the application under an explicit container name, blocking until every service port
   * accepts connections. The image is pulled first when it is not already present locally.
   *
   * <p>The created container reflects the application's full recipe: bind mounts are applied with
   * their read-only flag, volume binds with their access mode, a tmpfs mount when one is declared,
   * and a command override when one is present. A {@code test=<test>} label is attached so the
   * container can be attributed to its owning test. Once running, each service port is probed with a
   * one-second TCP connect, and the method does not return until all of them succeed.
   *
   * @param test an identifier for the owning test, recorded as a container label and as the
   * {@code Test} environment variable
   * @param name the name to assign to the new container
   * @param application the fixture to start
   * @param ports optional port overrides; when {@code null} or empty the application's default ports
   * are used
   * @return the id of the started container, for later passing to {@link #stop}
   * @throws IOException if image inspection or pull, container creation, or a port readiness probe fails
   * @throws InterruptedException if the thread is interrupted while pulling the image or awaiting readiness
   */
  public static String start (String test, String name, DockerApplication application, DockerPort... ports)
    throws IOException, InterruptedException {

    try (DockerClient dockerClient = DockerClientUtility.createClient()) {

      HostConfig hostConfig;
      CreateContainerCmd createContainerCmd;
      CreateContainerResponse createContainerResponse;
      DockerMount[] mounts;
      DockerVolume[] volumes;
      DockerTmpFs tmpFs;
      LinkedList<ExposedPort> servicePortList = new LinkedList<>();
      LinkedList<PortBinding> portBindingList = new LinkedList<>();
      boolean ready = false;

      try {
        dockerClient.inspectImageCmd(application.getImage()).exec();
      } catch (NotFoundException notFoundException) {
        LoggerManager.getLogger(DockerApplications.class).info("Downloading image(%s)...", name);
        dockerClient.pullImageCmd(application.getImage()).start().awaitCompletion();
        LoggerManager.getLogger(DockerApplications.class).info("Downloaded image(%s)...", name);
      }

      for (DockerPort dockerPort : ((ports == null) || (ports.length == 0)) ? application.getPorts() : ports) {

        ExposedPort serviceExposedPort = ExposedPort.tcp(dockerPort.getServicePort());

        servicePortList.add(serviceExposedPort);
        portBindingList.add(new PortBinding(Ports.Binding.bindPort((dockerPort.getExternalPort() == null) ? dockerPort.getServicePort() : dockerPort.getExternalPort()), serviceExposedPort));
      }

      hostConfig = HostConfig.newHostConfig().withPortBindings(portBindingList);

      if (((mounts = application.getMounts()) != null) && (mounts.length > 0)) {

        LinkedList<Mount> mountList = new LinkedList<>();

        for (DockerMount mount : mounts) {
          mountList.add(new Mount().withType(mount.getMountType()).withSource(mount.getHostPath()).withTarget(mount.getLocalPath()).withReadOnly(mount.isReadOnly()));
        }

        hostConfig.withMounts(mountList);
      }

      if (((volumes = application.getVolumes()) != null) && (volumes.length > 0)) {

        Bind[] binds = new Bind[volumes.length];
        int index = 0;

        for (DockerVolume volume : volumes) {
          binds[index++] = new Bind(volume.getHostPath(), new Volume(volume.getLocalPath()), volume.getAccessMode());
        }

        hostConfig.withBinds(binds);
      }

      if ((tmpFs = application.getTmpFs()) != null) {
        hostConfig.withTmpFs(Map.of(tmpFs.getPath(), tmpFs.getParameters()));
      }

      createContainerCmd = dockerClient.createContainerCmd(application.getImage())
                             .withName(name)
                             .withHostConfig(hostConfig)
                             .withExposedPorts(servicePortList)
                             .withLabels(Map.of("test", test));

      if ((application.getCommands() != null) && (application.getCommands().length > 0)) {
        createContainerCmd.withCmd(application.getCommands());
      }

      createContainerCmd.withEnv(application.getEnvironment(test));

      createContainerResponse = createContainerCmd.exec();
      dockerClient.startContainerCmd(createContainerResponse.getId()).exec();

      do {
        if (Boolean.TRUE.equals(dockerClient.inspectContainerCmd(createContainerResponse.getId()).exec().getState().getRunning())) {

          ready = true;

          for (DockerPort dockerPort : ((ports == null) || (ports.length == 0)) ? application.getPorts() : ports) {
            try (Socket socket = new Socket()) {
              socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), dockerPort.getServicePort()), 1000);
            } catch (IOException ioException) {
              ready = false;
              break;
            }
          }
        }
      } while (!ready);

      return createContainerResponse.getId();
    }
  }

  /**
   * Kills and removes a container, along with the volumes created for it. The container is first
   * killed and polled until it is no longer running, then removed and polled until it is gone.
   *
   * <p>A container that has already stopped or already been removed is treated as success and simply
   * logged, so the method is safe to call in teardown regardless of the container's current state.
   *
   * @param containerId the id returned by {@link #start}
   * @throws IOException if a Docker client cannot be created or an unexpected Docker API error occurs
   */
  public static void stop (String containerId)
    throws IOException {

    try (DockerClient dockerClient = DockerClientUtility.createClient()) {

      try {

        dockerClient.killContainerCmd(containerId).exec();

        try {

          InspectContainerResponse inspectContainerResponse;

          do {
            inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
          } while ((inspectContainerResponse != null) && (Boolean.TRUE.equals(inspectContainerResponse.getState().getRunning())));
        } catch (NotFoundException notFoundException) {
          // nothing to do here
        }
      } catch (NotFoundException notFoundException) {
        LoggerManager.getLogger(DockerApplications.class).info("Container(%s) was killed...", containerId);
      } catch (ConflictException conflictException) {
        LoggerManager.getLogger(DockerApplications.class).info("Container(%s) was stopped...", containerId);
      }

      try {

        boolean done = false;

        dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).exec();

        do {
          try {
            dockerClient.inspectContainerCmd(containerId).exec();
          } catch (NotFoundException notFoundException) {
            done = true;
            LoggerManager.getLogger(DockerApplications.class).info("Container(%s) was removed...", containerId);
          }
        } while (!done);
      } catch (NotFoundException notFoundException) {
        LoggerManager.getLogger(DockerApplications.class).info("Container(%s) was removed...", containerId);
      }
    }
  }
}
