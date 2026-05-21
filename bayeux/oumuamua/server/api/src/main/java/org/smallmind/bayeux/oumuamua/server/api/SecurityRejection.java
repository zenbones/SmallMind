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
package org.smallmind.bayeux.oumuamua.server.api;

/**
 * Value returned by {@link SecurityPolicy} to describe a denial. Any non-{@code null}
 * {@link SecurityRejection} signals deny; a {@link SecurityPolicy} method must return {@code null}
 * to permit the operation. {@link #noReason()} is the shared sentinel deny rejection that carries
 * no human-readable explanation, while {@link #reason(String)} carries an explanation propagated to
 * the client.
 */
public class SecurityRejection {

  private static final SecurityRejection NO_REASON = new SecurityRejection(null);
  private final String reason;

  /**
   * Constructs a rejection, optionally with a human-readable explanation.
   *
   * @param reason denial explanation, or {@code null} to represent the allow sentinel
   */
  private SecurityRejection (String reason) {

    this.reason = reason;
  }

  /**
   * Returns the shared sentinel deny rejection that carries no human-readable explanation.
   *
   * @return deny sentinel with no rejection reason
   */
  public static SecurityRejection noReason () {

    return NO_REASON;
  }

  /**
   * Creates a denial result carrying the given explanation.
   *
   * @param reason human-readable explanation to surface to the client
   * @return new rejection instance bearing the reason
   */
  public static SecurityRejection reason (String reason) {

    return new SecurityRejection(reason);
  }

  /**
   * Returns whether this rejection carries an explanation string.
   *
   * @return {@code true} if a reason string is present
   */
  public boolean hasReason () {

    return reason != null;
  }

  /**
   * Returns the denial explanation, or {@code null} when this is the allow sentinel.
   *
   * @return reason string, or {@code null}
   */
  public String getReason () {

    return reason;
  }
}
