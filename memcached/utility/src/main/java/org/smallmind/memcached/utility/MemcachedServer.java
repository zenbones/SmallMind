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
package org.smallmind.memcached.utility;

/**
 * Simple value object that describes a single memcached server endpoint.
 *
 * <p>Instances are produced by factory beans such as
 * {@link org.smallmind.memcached.utility.spring.MemcachedServerFactoryBean} and
 * {@link org.smallmind.memcached.utility.spring.MemcachedServerMapFactoryBean}, and are consumed
 * by client factory beans to open connections to the cluster.</p>
 */
public class MemcachedServer {

  private String host;
  private int port;

  /**
   * Default no-arg constructor for use by Spring or other IoC containers that set properties
   * via setters.
   */
  public MemcachedServer () {

  }

  /**
   * Constructs a server descriptor with the given host and port.
   *
   * @param host the hostname or IP address of the memcached server
   * @param port the TCP port on which the server is listening
   */
  public MemcachedServer (String host, int port) {

    this.host = host;
    this.port = port;
  }

  /**
   * Returns the hostname or IP address of this server.
   *
   * @return the server host
   */
  public String getHost () {

    return host;
  }

  /**
   * Sets the hostname or IP address of this server.
   *
   * @param host the server host
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Returns the TCP port of this server.
   *
   * @return the server port
   */
  public int getPort () {

    return port;
  }

  /**
   * Sets the TCP port of this server.
   *
   * @param port the server port
   */
  public void setPort (int port) {

    this.port = port;
  }
}
