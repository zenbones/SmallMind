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
package org.smallmind.javafx.extras.dialog;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * JavaFX event fired when an error dialog is dismissed. The event carries the logical source object
 * associated with the error and the throwable that was displayed. Handlers may register against
 * {@link #ANY} to receive all error events, or against {@link #OCCURRED} for the specific error-occurred
 * sub-type.
 */
public class ErrorEvent extends Event {

  /**
   * Super-type event type that acts as a wildcard for all {@code ErrorEvent} variants. Handlers
   * registered against this type receive every {@code ErrorEvent} regardless of sub-type.
   */
  public static final EventType<ErrorEvent> ANY = new EventType<>(Event.ANY, "ERROR_ANY");

  /**
   * Specific event type fired when an error has occurred and the dialog has been shown. This is a
   * child of {@link #ANY}.
   */
  public static final EventType<ErrorEvent> OCCURRED = new EventType<>(ErrorEvent.ANY, "OCCURRED");

  private final Object exceptionSource;
  private final Throwable throwable;

  /**
   * Creates an error event of the given type carrying the originating source and throwable.
   *
   * @param eventType       the specific error event type; typically {@link #OCCURRED}
   * @param exceptionSource the object associated with or responsible for the error;
   *                        may be {@code null}
   * @param throwable       the exception that was raised and displayed; must not be {@code null}
   */
  protected ErrorEvent (EventType<ErrorEvent> eventType, Object exceptionSource, Throwable throwable) {

    super(eventType);

    this.exceptionSource = exceptionSource;
    this.throwable = throwable;
  }

  /**
   * Returns the object that was associated with the error at the time the dialog was created.
   *
   * @return the exception source; may be {@code null}
   */
  public Object getExceptionSource () {

    return exceptionSource;
  }

  /**
   * Returns the throwable that was displayed in the error dialog.
   *
   * @return the throwable; never {@code null}
   */
  public Throwable getThrowable () {

    return throwable;
  }
}
