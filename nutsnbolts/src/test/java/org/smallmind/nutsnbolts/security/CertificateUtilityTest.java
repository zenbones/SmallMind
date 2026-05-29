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
package org.smallmind.nutsnbolts.security;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class CertificateUtilityTest {

  @BeforeClass
  public void registerBouncyCastleProvider () {

    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public void testConstructCertificateProducesParseableSelfSignedPEM ()
    throws Exception {

    KeyPair pair = generateRSAKeyPair();
    Date now = Date.from(Instant.now());
    Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

    String pem = CertificateUtility.constructCertificate(
      "server.example.com",
      "TestOU",
      "TestOrg",
      "US",
      "California",
      "San Francisco",
      now,
      now,
      notAfter,
      "server.example.com",
      RSASigningAlgorithm.SHA_256_WITH_RSA,
      pair);

    Assert.assertTrue(pem.contains("-----BEGIN CERTIFICATE-----"));
    Assert.assertTrue(pem.contains("-----END CERTIFICATE-----"));

    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    X509Certificate certificate = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));

    Assert.assertTrue(certificate.getSubjectX500Principal().getName().contains("CN=server.example.com"));
    Assert.assertTrue(certificate.getIssuerX500Principal().getName().contains("CN=server.example.com"));
  }

  private static KeyPair generateRSAKeyPair ()
    throws Exception {

    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

    generator.initialize(2048);

    return generator.generateKeyPair();
  }
}
