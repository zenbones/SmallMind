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
 * Utility class for starting and stopping Docker containers in the science workbench
 * test environment. Missing images are pulled automatically. Port bindings, volume mounts,
 * and tmpfs configuration are applied from the {@link DockerApplication} descriptor.
 *
 * <p>{@link #start} blocks until the container is running <em>and</em> all service ports
 * are accepting TCP connections, ensuring downstream test code does not need its own
 * readiness polling.
 */
public class DockerApplications {

  /**
   * Starts a container for the given application using the application's default container name,
   * and waits until all service ports accept connections.
   *
   * @param test        a test identifier embedded as a container label and environment variable
   * @param application the application template describing the image, ports, and configuration
   * @param ports       optional port overrides; when empty or {@code null} the application's
   *                    default ports are used
   * @return the Docker container ID of the newly started container
   * @throws IOException          if a port liveness check or Docker client operation fails
   * @throws InterruptedException if the calling thread is interrupted while waiting for readiness
   */
  public static String start (String test, DockerApplication application, DockerPort... ports)
    throws IOException, InterruptedException {

    return start(test, application.getName(), application, ports);
  }

  /**
   * Starts a container for the given application under an explicit container name, and waits
   * until all service ports accept connections. If the image is not present locally it is
   * pulled before the container is created.
   *
   * <p>Mount descriptors are applied with their read-only flag when the application specifies
   * them. Volume binds are applied when the application specifies them. A tmpfs mount is applied
   * when the application specifies one. Command overrides are applied when non-empty.
   * A {@code test=<test>} label is added to the container for later identification.
   *
   * @param test        a test identifier embedded as a container label and environment variable
   * @param name        the name to assign to the new container
   * @param application the application template describing the image, ports, and configuration
   * @param ports       optional port overrides; when empty or {@code null} the application's
   *                    default ports are used
   * @return the Docker container ID of the newly started container
   * @throws IOException          if a port liveness check or Docker client operation fails
   * @throws InterruptedException if the calling thread is interrupted while pulling the image
   *                              or waiting for readiness
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
   * Kills and removes the container with the given ID. Waits for the container to reach a
   * stopped state before issuing the remove command, then polls until the remove completes.
   * Volumes associated with the container are removed along with it.
   *
   * <p>This method tolerates containers that are already stopped or already removed —
   * both cases are logged and treated as success.
   *
   * @param containerId the Docker container ID returned by {@link #start}
   * @throws IOException if the Docker client cannot be created or an unexpected API error occurs
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
