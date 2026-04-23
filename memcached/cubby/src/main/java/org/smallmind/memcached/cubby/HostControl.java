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
package org.smallmind.memcached.cubby;

/**
 * Associates a {@link MemcachedHost} with its current health state within a {@link ServerPool}.
 *
 * <p>{@code HostControl} is an internal bookkeeping object managed by the pool. The active flag
 * is toggled by the {@link ConnectionCoordinator} when a host goes offline or is successfully
 * reconnected. The {@link ServerDefibrillator} consults this flag to determine which hosts
 * need reconnection attempts, and the configured {@link org.smallmind.memcached.cubby.locator.KeyLocator}
 * uses it to exclude unhealthy hosts from routing decisions.</p>
 */
public class HostControl {

  private final MemcachedHost memcachedHost;
  private boolean active = true;

  /**
   * Wraps the supplied host definition with an initially active state.
   *
   * @param memcachedHost the host whose health this control object tracks
   */
  public HostControl (MemcachedHost memcachedHost) {

    this.memcachedHost = memcachedHost;
  }

  /**
   * Returns the underlying host definition managed by this control object.
   *
   * @return the {@link MemcachedHost}, never {@code null}
   */
  public MemcachedHost getMemcachedHost () {

    return memcachedHost;
  }

  /**
   * Indicates whether the host is currently considered healthy and eligible for routing.
   *
   * @return {@code true} if the host is reachable; {@code false} if it has been marked offline
   */
  public boolean isActive () {

    return active;
  }

  /**
   * Updates the health state of the host.
   *
   * @param active {@code true} to mark the host as healthy and available for routing;
   *               {@code false} to remove it from routing consideration
   */
  public void setActive (boolean active) {

    this.active = active;
  }
}
