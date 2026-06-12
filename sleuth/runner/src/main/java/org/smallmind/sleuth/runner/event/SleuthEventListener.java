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
package org.smallmind.sleuth.runner.event;

import java.util.EventListener;

/**
 * Observer interface for Sleuth test-execution events.
 * <p>
 * Implementations are registered with
 * {@link org.smallmind.sleuth.runner.SleuthRunner#addListener(SleuthEventListener)} and receive
 * every {@link SleuthEvent} fired during a run — including the {@link CancelledSleuthEvent}
 * lifecycle event as well as per-test outcome events
 * ({@link SuccessSleuthEvent}, {@link FailureSleuthEvent}, {@link ErrorSleuthEvent},
 * {@link SkippedSleuthEvent}, {@link FatalSleuthEvent}).
 * <p>
 * {@link #handle(SleuthEvent)} is invoked on the runner thread that fired the event; implementations
 * must be thread-safe when tests execute concurrently.
 *
 * @see SleuthEvent
 * @see org.smallmind.sleuth.runner.SleuthRunner
 */
public interface SleuthEventListener extends EventListener {

  /**
   * Called when the Sleuth runner fires an event during test execution.
   * <p>
   * The supplied event can be narrowed to a concrete subtype using {@link SleuthEvent#getType()}
   * as a discriminator. Implementations should not throw unchecked exceptions; any exception
   * will propagate back to the calling runner thread.
   *
   * @param event the event that was fired; never {@code null}
   */
  void handle (SleuthEvent event);
}
