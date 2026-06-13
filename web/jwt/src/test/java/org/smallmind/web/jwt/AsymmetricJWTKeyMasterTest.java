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
package org.smallmind.web.jwt;

import java.security.Key;
import java.security.KeyPairGenerator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies that {@link AsymmetricJWTKeyMaster} maps a key's algorithm to the correct JWT signing
 * algorithm and rejects unsupported key types.
 */
@Test(groups = "unit")
public class AsymmetricJWTKeyMasterTest {

  public void testRsaKeyMapsToRs256 ()
    throws Exception {

    AsymmetricJWTKeyMaster keyMaster = new AsymmetricJWTKeyMaster(KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate());

    Assert.assertEquals(keyMaster.getEncryptionAlgorithm(), JWTEncryptionAlgorithm.RS256);
  }

  public void testEd25519KeyMapsToEddsa ()
    throws Exception {

    // The standard JDK provider reports getAlgorithm() as "EdDSA" for an Ed25519 key.
    AsymmetricJWTKeyMaster keyMaster = new AsymmetricJWTKeyMaster(KeyPairGenerator.getInstance("Ed25519").generateKeyPair().getPrivate());

    Assert.assertEquals(keyMaster.getEncryptionAlgorithm(), JWTEncryptionAlgorithm.EDDSA);
  }

  public void testEd25519AlternateAlgorithmNameMapsToEddsa ()
    throws Exception {

    // Some providers report the algorithm as the literal "Ed25519" rather than "EdDSA"; both map to
    // the same signing algorithm. A stub key isolates that branch from provider naming.
    AsymmetricJWTKeyMaster keyMaster = new AsymmetricJWTKeyMaster(new NamedAlgorithmKey("Ed25519"));

    Assert.assertEquals(keyMaster.getEncryptionAlgorithm(), JWTEncryptionAlgorithm.EDDSA);
  }

  public void testKeyExposedUnchanged ()
    throws Exception {

    java.security.Key key = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();

    Assert.assertSame(new AsymmetricJWTKeyMaster(key).getKey(), key);
  }

  @Test(expectedExceptions = UnknownAlgorithmException.class)
  public void testUnsupportedAlgorithmRejected ()
    throws Exception {

    new AsymmetricJWTKeyMaster(KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate());
  }

  private static final class NamedAlgorithmKey implements Key {

    private final String algorithm;

    private NamedAlgorithmKey (String algorithm) {

      this.algorithm = algorithm;
    }

    @Override
    public String getAlgorithm () {

      return algorithm;
    }

    @Override
    public String getFormat () {

      return "RAW";
    }

    @Override
    public byte[] getEncoded () {

      return new byte[0];
    }
  }
}
