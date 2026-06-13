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
package org.smallmind.web.grizzly;

import org.smallmind.nutsnbolts.lang.SecureStore;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SSLInfoTest {

  public void testDefaults () {

    SSLInfo sslInfo = new SSLInfo();

    Assert.assertEquals(sslInfo.getPort(), 443);
    Assert.assertFalse(sslInfo.isRequireClientAuth());
    Assert.assertFalse(sslInfo.isProxyMode());
    Assert.assertNull(sslInfo.getKeySecureStore());
    Assert.assertNull(sslInfo.getTrustSecureStore());
  }

  public void testAccessorRoundTrip () {

    SSLInfo sslInfo = new SSLInfo();
    SecureStore keySecureStore = new SecureStore();
    SecureStore trustSecureStore = new SecureStore();

    sslInfo.setPort(8443);
    sslInfo.setRequireClientAuth(true);
    sslInfo.setProxyMode(true);
    sslInfo.setKeySecureStore(keySecureStore);
    sslInfo.setTrustSecureStore(trustSecureStore);

    Assert.assertEquals(sslInfo.getPort(), 8443);
    Assert.assertTrue(sslInfo.isRequireClientAuth());
    Assert.assertTrue(sslInfo.isProxyMode());
    Assert.assertSame(sslInfo.getKeySecureStore(), keySecureStore);
    Assert.assertSame(sslInfo.getTrustSecureStore(), trustSecureStore);
  }
}
