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
package org.smallmind.quorum.juggler;

/**
 * Sink that accepts failed {@link JugglingPin}s so they are quarantined from active service.
 * <p>
 * A pin's own code can hold a reference to the {@link BlackList} it belongs to and call
 * {@link #addToBlackList(BlacklistEntry)} autonomously when it detects an unrecoverable
 * error, without waiting for the {@link Juggler} to discover the failure through a
 * subsequent {@link JugglingPin#obtain()} call.
 *
 * @param <R> the type of resource associated with the pins
 */
public interface BlackList<R> {

  /**
   * Moves the pin described by {@code blacklistEntry} out of active circulation.
   * <p>
   * After this call the pin will not be returned to callers until the optional recovery
   * worker re-validates it and restores it to service.
   *
   * @param blacklistEntry a record holding the failed pin and the throwable that caused the failure
   */
  void addToBlackList (BlacklistEntry<R> blacklistEntry);
}
