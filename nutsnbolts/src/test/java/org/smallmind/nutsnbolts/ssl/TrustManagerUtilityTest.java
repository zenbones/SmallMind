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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.net.ssl.TrustManager;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class TrustManagerUtilityTest {

  private static byte[] generateSelfSignedCertificateBytes ()
    throws Exception {

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

    keyPairGenerator.initialize(2048);

    KeyPair pair = keyPairGenerator.generateKeyPair();
    X500Principal subject = new X500Principal("CN=trustmanager-test, OU=nutsnbolts, O=SmallMind, C=US");
    Instant now = Instant.now();
    X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
      subject,
      BigInteger.valueOf(now.toEpochMilli()),
      Date.from(now),
      Date.from(now.plus(1, ChronoUnit.DAYS)),
      subject,
      pair.getPublic());
    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate());
    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(signer));

    return certificate.getEncoded();
  }

  @BeforeClass
  public void registerBouncyCastleProvider () {

    if (Security.getProvider("BC") == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public void testLoadProducesTrustManagersFromInMemoryCertificate ()
    throws Exception {

    byte[] certBytes = generateSelfSignedCertificateBytes();
    TrustManager[] managers = TrustManagerUtility.load("test-alias", new InMemoryCertResource(certBytes));

    Assert.assertNotNull(managers);
    Assert.assertTrue(managers.length > 0);
  }

  private static class InMemoryCertResource implements Resource {

    private final byte[] bytes;

    InMemoryCertResource (byte[] bytes) {

      this.bytes = bytes;
    }

    @Override
    public String getIdentifier () {

      return "memory:cert";
    }

    @Override
    public String getScheme () {

      return "memory";
    }

    @Override
    public String getPath () {

      return "cert";
    }

    @Override
    public InputStream getInputStream ()
      throws ResourceException {

      return new ByteArrayInputStream(bytes);
    }
  }
}
