/*
 * Copyright (c) 2007 through 2024 David Berkman
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
 * Describes a decision to reject an operation for security reasons.
 */
public class SecurityRejection {

  private static final SecurityRejection NO_REASON = new SecurityRejection(null);
  private final String reason;

  /**
   * Creates a rejection with a textual reason.
   *
   * @param reason explanation for the rejection; may be {@code null} to indicate no rejection
   */
  private SecurityRejection (String reason) {

    this.reason = reason;
  }

  /**
   * Produces a value indicating the action is permitted.
   *
   * @return an instance without a rejection reason
   */
  public static SecurityRejection noReason () {

    return NO_REASON;
  }

  /**
   * Produces a rejection describing why the action is denied.
   *
   * @param reason explanation for the denial
   * @return a new rejection instance
   */
  public static SecurityRejection reason (String reason) {

    return new SecurityRejection(reason);
  }

  /**
   * Indicates whether a reason exists.
   *
   * @return {@code true} if a rejection reason has been set
   */
  public boolean hasReason () {

    return reason != null;
  }

  /**
   * @return the rejection reason, or {@code null} if none
   */
  public String getReason () {

    return reason;
  }
}
