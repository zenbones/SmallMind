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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.smallmind.nutsnbolts.resource.Resource;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class KeyStoreUtilityTest {

  private static final class BytesResource implements Resource {

    private final byte[] payload;

    BytesResource (byte[] payload) {

      this.payload = payload;
    }

    @Override
    public String getIdentifier () {

      return "bytes:in-memory.der";
    }

    @Override
    public String getScheme () {

      return "bytes";
    }

    @Override
    public String getPath () {

      return "in-memory.der";
    }

    @Override
    public InputStream getInputStream () {

      return new ByteArrayInputStream(payload);
    }
  }

  private static X509Certificate selfSign (KeyPair keyPair)
    throws Exception {

    X500Principal subject = new X500Principal("CN=KeyStoreUtility Test");
    long now = System.currentTimeMillis();
    Date notBefore = new Date(now);
    Date notAfter = new Date(now + 86_400_000L);

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject, BigInteger.valueOf(1), notBefore, notAfter, subject, keyPair.getPublic());
    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

    return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
  }

  public void testConstructWritesKeystoreAndReturnsDescriptor ()
    throws Exception {

    Path tempHome = Files.createTempDirectory("kstest-");
    String previousHome = System.getProperty("user.home");
    System.setProperty("user.home", tempHome.toString());
    try {

      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      X509Certificate certificate = selfSign(keyPair);

      Resource keyResource = new BytesResource(keyPair.getPrivate().getEncoded());
      Resource certResource = new BytesResource(certificate.getEncoded());

      KeyStoreInfo info = KeyStoreUtility.construct("teststore.jks", "alias", "secret", keyResource, certResource);

      Assert.assertEquals(info.getKeystoreName(), "teststore.jks");
      Assert.assertEquals(info.getKeystoreAlias(), "alias");
      Assert.assertEquals(info.getKeystorePassword(), "secret");
      Assert.assertNotNull(info.getKeystorePath());
      Assert.assertTrue(Files.exists(info.getKeystorePath()));

      KeyStore loaded = KeyStore.getInstance("JKS");
      try (InputStream in = Files.newInputStream(info.getKeystorePath())) {
        loaded.load(in, "secret".toCharArray());
      }

      Assert.assertTrue(loaded.isKeyEntry("alias"), "alias key entry missing");
      Assert.assertNotNull(loaded.getKey("alias", "secret".toCharArray()));
      Assert.assertNotNull(loaded.getCertificate("alias"));
    } finally {
      if (previousHome == null) {
        System.clearProperty("user.home");
      } else {
        System.setProperty("user.home", previousHome);
      }
      try (var paths = Files.walk(tempHome)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {

          }
        });
      }
    }
  }

  public void testConstructAppendsJksExtensionWhenMissing ()
    throws Exception {

    Path tempHome = Files.createTempDirectory("kstest-");
    String previousHome = System.getProperty("user.home");
    System.setProperty("user.home", tempHome.toString());
    try {

      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      X509Certificate certificate = selfSign(keyPair);

      Resource keyResource = new BytesResource(keyPair.getPrivate().getEncoded());
      Resource certResource = new BytesResource(certificate.getEncoded());

      KeyStoreInfo info = KeyStoreUtility.construct("teststore", "alias", "secret", keyResource, certResource);

      Assert.assertEquals(info.getKeystoreName(), "teststore.jks");
    } finally {
      if (previousHome == null) {
        System.clearProperty("user.home");
      } else {
        System.setProperty("user.home", previousHome);
      }
      try (var paths = Files.walk(tempHome)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {

          }
        });
      }
    }
  }

  public void testConstructAppliesDefaultsForNullArguments ()
    throws Exception {

    Path tempHome = Files.createTempDirectory("kstest-");
    String previousHome = System.getProperty("user.home");
    System.setProperty("user.home", tempHome.toString());
    try {

      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      KeyPair keyPair = generator.generateKeyPair();
      X509Certificate certificate = selfSign(keyPair);

      Resource keyResource = new BytesResource(keyPair.getPrivate().getEncoded());
      Resource certResource = new BytesResource(certificate.getEncoded());

      KeyStoreInfo info = KeyStoreUtility.construct(null, null, null, keyResource, certResource);

      Assert.assertEquals(info.getKeystoreName(), "keystore.jks");
      Assert.assertEquals(info.getKeystoreAlias(), "mykeystore");
      Assert.assertEquals(info.getKeystorePassword(), "changeit");
    } finally {
      if (previousHome == null) {
        System.clearProperty("user.home");
      } else {
        System.setProperty("user.home", previousHome);
      }
      try (var paths = Files.walk(tempHome)) {
        paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
          try {
            Files.deleteIfExists(p);
          } catch (Exception ignored) {

          }
        });
      }
    }
  }
}
