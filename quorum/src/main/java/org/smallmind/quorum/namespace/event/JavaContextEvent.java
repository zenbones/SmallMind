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
package org.smallmind.quorum.namespace.event;

import java.util.EventObject;
import javax.naming.CommunicationException;

/**
 * Event fired by a {@link org.smallmind.quorum.namespace.PooledJavaContext} when its
 * lifecycle changes — either because it was intentionally closed or because a backing-store
 * communication failure forced an abort.
 * <p>
 * The event optionally carries the {@link CommunicationException} that triggered an abort.
 * Use {@link #containsCommunicationException()} to distinguish a normal close (no exception)
 * from an aborted context (exception present) without a null check.
 */
public class JavaContextEvent extends EventObject {

  private final CommunicationException communicationException;

  /**
   * Creates an event representing a normal, intentional context close with no communication
   * error.
   *
   * @param source the {@link org.smallmind.quorum.namespace.PooledJavaContext} that fired
   *               the event
   */
  public JavaContextEvent (Object source) {

    this(source, null);
  }

  /**
   * Creates an event with an optional communication error.
   * <p>
   * Pass {@code null} for {@code communicationException} when constructing a normal-close
   * event; pass the actual exception when constructing an abort event.
   *
   * @param source                 the {@link org.smallmind.quorum.namespace.PooledJavaContext}
   *                               that fired the event
   * @param communicationException the exception that caused the context to abort, or
   *                               {@code null} for a normal close
   */
  public JavaContextEvent (Object source, CommunicationException communicationException) {

    super(source);

    this.communicationException = communicationException;
  }

  /**
   * Returns {@code true} when this event was triggered by a communication failure rather
   * than an intentional close.
   *
   * @return {@code true} if a {@link CommunicationException} is present
   */
  public boolean containsCommunicationException () {

    return communicationException != null;
  }

  /**
   * Returns the communication exception that caused the context abort, or {@code null} if
   * this is a normal-close event.
   *
   * @return the {@link CommunicationException}, or {@code null}
   */
  public CommunicationException getCommunicationException () {

    return communicationException;
  }
}
