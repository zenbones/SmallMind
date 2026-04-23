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
package org.smallmind.nutsnbolts.email;

import jakarta.mail.Authenticator;

/**
 * Value object that pairs an {@link AuthType} with the credentials required by that strategy, and can produce the corresponding Jakarta Mail {@link Authenticator}.
 */
public class Authentication {

  /**
   * Shared singleton representing the absence of authentication.
   */
  public static final Authentication NONE = new Authentication(AuthType.NONE);

  private AuthType type;
  private String[] data;

  /**
   * Creates an unconfigured authentication descriptor; use {@link #setType} and {@link #setData} before calling {@link #getAuthenticator}.
   */
  public Authentication () {

  }

  private Authentication (AuthType type, String... data) {

    this.type = type;
    this.data = data;
  }

  /**
   * Sets the authentication mechanism to use when connecting to the mail server.
   *
   * @param type the desired authentication strategy
   */
  public void setType (AuthType type) {

    this.type = type;
  }

  /**
   * Sets the credential values passed to the selected {@link AuthType} (for example, {@code [username, password]} for {@link AuthType#LOGIN}).
   *
   * @param data ordered credential strings expected by the authentication type
   */
  public void setData (String[] data) {

    this.data = data;
  }

  /**
   * Builds a Jakarta Mail {@link Authenticator} for the configured authentication type and credentials.
   *
   * @return a configured {@link Authenticator}, or {@code null} when the type is {@link AuthType#NONE}
   */
  public Authenticator getAuthenticator () {

    return type.getAuthenticator(data);
  }
}
