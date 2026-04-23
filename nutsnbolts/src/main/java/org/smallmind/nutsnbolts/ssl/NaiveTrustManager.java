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
package org.smallmind.nutsnbolts.ssl;

import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

/**
 * {@link X509TrustManager} that accepts all certificates without validation, intended only for development
 * and testing where certificate checking must be suppressed.
 */
public class NaiveTrustManager implements X509TrustManager {

  /**
   * Performs no validation, unconditionally accepting any client certificate chain.
   *
   * @param cert     the client certificate chain to check
   * @param authType the key exchange algorithm used
   */
  public void checkClientTrusted (X509Certificate[] cert, String authType) {

  }

  /**
   * Performs no validation, unconditionally accepting any server certificate chain.
   *
   * @param cert     the server certificate chain to check
   * @param authType the key exchange algorithm used
   */
  public void checkServerTrusted (X509Certificate[] cert, String authType) {

  }

  /**
   * Returns an empty array to indicate that no certificate authorities are specifically trusted.
   *
   * @return an empty {@link X509Certificate} array
   */
  public X509Certificate[] getAcceptedIssuers () {

    return new X509Certificate[0];
  }
}
