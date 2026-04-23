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
package org.smallmind.phalanx.wire.transport.jms;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Queue;
import jakarta.jms.Topic;

/**
 * Lifecycle-aware service locator for JMS resources.
 *
 * <p>Implementations provide lookup operations for connection factories, queues, and topics,
 * and expose {@link #start()} / {@link #stop()} hooks for managing any underlying broker
 * or JNDI context lifecycle.
 */
public interface MessageBroker {

  /**
   * Resolves a {@link ConnectionFactory} by the given identifier or JNDI path.
   *
   * @param path broker-specific identifier or JNDI lookup path for the connection factory
   * @return the resolved {@link ConnectionFactory}
   * @throws Exception if the lookup fails
   */
  ConnectionFactory lookupConnectionFactory (String path)
    throws Exception;

  /**
   * Resolves a JMS {@link Queue} by the given identifier or JNDI path.
   *
   * @param path broker-specific identifier or JNDI lookup path for the queue
   * @return the resolved {@link Queue}
   * @throws Exception if the lookup fails
   */
  Queue lookupQueue (String path)
    throws Exception;

  /**
   * Resolves a JMS {@link Topic} by the given identifier or JNDI path.
   *
   * @param path broker-specific identifier or JNDI lookup path for the topic
   * @return the resolved {@link Topic}
   * @throws Exception if the lookup fails
   */
  Topic lookupTopic (String path)
    throws Exception;

  /**
   * Starts the broker or its underlying resources.
   *
   * @throws Exception if startup fails
   */
  void start ()
    throws Exception;

  /**
   * Stops the broker or its underlying resources.
   *
   * @throws Exception if shutdown fails
   */
  void stop ()
    throws Exception;
}
