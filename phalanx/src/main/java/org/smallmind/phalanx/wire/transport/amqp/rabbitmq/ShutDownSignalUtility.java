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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import com.rabbitmq.client.ShutdownSignalException;

/**
 * Utility for locating the {@link ShutdownSignalException} buried within an exception chain.  The
 * RabbitMQ client frequently wraps the original shutdown signal inside another exception, so a
 * simple {@code instanceof} test against the thrown exception is not sufficient to determine
 * whether a connection or channel shutdown was the underlying cause of a failure.
 */
public class ShutDownSignalUtility {

  /**
   * Walks the cause chain of the given throwable and returns the first {@link ShutdownSignalException}
   * encountered, starting with the throwable itself.
   *
   * @param throwable the throwable to inspect, which may be {@code null}
   * @return the first shutdown signal found within the cause chain, or {@code null} if the chain
   * contains no shutdown signal.
   */
  public static ShutdownSignalException asShutdownSignal (Throwable throwable) {

    Throwable cursor = throwable;

    while (cursor != null) {
      if (cursor instanceof ShutdownSignalException) {

        return (ShutdownSignalException)cursor;
      }
      cursor = cursor.getCause();
    }

    return null;
  }
}
