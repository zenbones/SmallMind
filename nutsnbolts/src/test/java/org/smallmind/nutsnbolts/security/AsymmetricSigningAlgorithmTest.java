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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class AsymmetricSigningAlgorithmTest {

  public void testRSASigningAlgorithmEnumNamesMapToJcaIdentifiers () {

    Assert.assertEquals(RSASigningAlgorithm.SHA_256_WITH_RSA.getAlgorithmName(), "SHA256withRSA");
  }

  public void testECDSASigningAlgorithmEnumNamesMapToJcaIdentifiers () {

    Assert.assertEquals(ECDSASigningAlgorithm.ECDSA_USING_SHA_ALGORITHM.getAlgorithmName(), "EcdsaUsingShaAlgorithm");
  }

  public void testEdDSASigningAlgorithmEnumNamesMapToJcaIdentifiers () {

    Assert.assertEquals(EdDSASigningAlgorithm.ED_DSA.getAlgorithmName(), "EdDSA");
  }

  public void testRSASignAndVerifyRoundTripSucceedsOnUnmodifiedPayload ()
    throws Exception {

    byte[] payload = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
    KeyPair pair = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.RSA);
    byte[] signature = EncryptionUtility.sign(RSASigningAlgorithm.SHA_256_WITH_RSA, pair.getPrivate(), payload);

    Assert.assertTrue(EncryptionUtility.verify(RSASigningAlgorithm.SHA_256_WITH_RSA, pair.getPublic(), payload, signature));
  }

  public void testRSAVerifyFailsOnTamperedPayload ()
    throws Exception {

    byte[] payload = "original".getBytes(StandardCharsets.UTF_8);
    byte[] tampered = "originaX".getBytes(StandardCharsets.UTF_8);
    KeyPair pair = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.RSA);
    byte[] signature = EncryptionUtility.sign(RSASigningAlgorithm.SHA_256_WITH_RSA, pair.getPrivate(), payload);

    Assert.assertFalse(EncryptionUtility.verify(RSASigningAlgorithm.SHA_256_WITH_RSA, pair.getPublic(), tampered, signature));
  }

  public void testRSAVerifyFailsWithDifferentKeyPair ()
    throws Exception {

    byte[] payload = "x".getBytes(StandardCharsets.UTF_8);
    KeyPair signer = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.RSA);
    KeyPair other = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.RSA);
    byte[] signature = EncryptionUtility.sign(RSASigningAlgorithm.SHA_256_WITH_RSA, signer.getPrivate(), payload);

    Assert.assertFalse(EncryptionUtility.verify(RSASigningAlgorithm.SHA_256_WITH_RSA, other.getPublic(), payload, signature));
  }

  public void testEdDSASignAndVerifyRoundTripSucceedsOnUnmodifiedPayload ()
    throws Exception {

    byte[] payload = "the quick brown fox".getBytes(StandardCharsets.UTF_8);
    KeyPair pair = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.ED25519);
    byte[] signature = EncryptionUtility.sign(EdDSASigningAlgorithm.ED_DSA, pair.getPrivate(), payload);

    Assert.assertTrue(EncryptionUtility.verify(EdDSASigningAlgorithm.ED_DSA, pair.getPublic(), payload, signature));
  }

  public void testEdDSAVerifyFailsOnTamperedPayload ()
    throws Exception {

    byte[] payload = "original".getBytes(StandardCharsets.UTF_8);
    byte[] tampered = "originaX".getBytes(StandardCharsets.UTF_8);
    KeyPair pair = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.ED25519);
    byte[] signature = EncryptionUtility.sign(EdDSASigningAlgorithm.ED_DSA, pair.getPrivate(), payload);

    Assert.assertFalse(EncryptionUtility.verify(EdDSASigningAlgorithm.ED_DSA, pair.getPublic(), tampered, signature));
  }
}
