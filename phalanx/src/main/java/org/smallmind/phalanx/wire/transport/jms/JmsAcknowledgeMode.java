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

import jakarta.jms.Session;

/**
 * Standard JMS acknowledge modes drawn directly from {@link Session} constants.
 *
 * <p>Implements {@link AcknowledgeMode} so that a {@link MessagePolicy} can reference
 * any of these values when creating sessions via {@link ConnectionManager}.
 */
public enum JmsAcknowledgeMode implements AcknowledgeMode {

  /**
   * Session automatically acknowledges each received message; maps to {@link Session#AUTO_ACKNOWLEDGE}.
   */
  AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),

  /**
   * Application must explicitly call {@code Message.acknowledge()}; maps to {@link Session#CLIENT_ACKNOWLEDGE}.
   */
  CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE),

  /**
   * Lazy acknowledgement allowing duplicate delivery; maps to {@link Session#DUPS_OK_ACKNOWLEDGE}.
   */
  DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE);

  private final int jmsValue;

  /**
   * Creates a constant that wraps the given JMS session acknowledge-mode integer.
   *
   * @param jmsValue integer acknowledge-mode constant from {@link Session}
   */
  JmsAcknowledgeMode (int jmsValue) {

    this.jmsValue = jmsValue;
  }

  /**
   * Returns the JMS acknowledge-mode integer for use with
   * {@link jakarta.jms.Connection#createSession(boolean, int)}.
   *
   * @return JMS acknowledge-mode constant
   */
  public int getJmsValue () {

    return jmsValue;
  }
}
