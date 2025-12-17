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
 * Event describing the closure or abort of a {@link org.smallmind.quorum.namespace.JavaContext}.
 */
public class JavaContextEvent extends EventObject {

  private final CommunicationException communicationException;

  /**
   * Creates an event without an associated communication exception.
   *
   * @param source context source
   */
  public JavaContextEvent (Object source) {

    this(source, null);
  }

  /**
   * Creates an event with an optional communication exception.
   *
   * @param source                 context source
   * @param communicationException underlying communication error, or {@code null}
   */
  public JavaContextEvent (Object source, CommunicationException communicationException) {

    super(source);

    this.communicationException = communicationException;
  }

  /**
   * Indicates whether the event carries a communication exception.
   *
   * @return {@code true} if a communication error is present
   */
  public boolean containsCommunicationException () {

    return communicationException != null;
  }

  /**
   * Returns the associated communication exception, if any.
   *
   * @return communication exception or {@code null}
   */
  public CommunicationException getCommunicationException () {

    return communicationException;
  }
}
