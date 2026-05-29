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
import java.security.Key;
import java.security.KeyPair;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class EncryptionUtilityTest {

  public void testSha256HashMatchesKnownVector ()
    throws Exception {

    byte[] expected = new byte[] {
      (byte)0xb9, 0x4d, 0x27, (byte)0xb9, (byte)0x93, 0x4d, 0x3e, 0x08,
      (byte)0xa5, 0x2e, 0x52, (byte)0xd7, (byte)0xda, 0x7d, (byte)0xab, (byte)0xfa,
      (byte)0xc4, (byte)0x84, (byte)0xef, (byte)0xe3, 0x7a, 0x53, (byte)0x80, (byte)0xee,
      (byte)0x90, (byte)0x88, (byte)0xf7, (byte)0xac, (byte)0xe2, (byte)0xef, (byte)0xcd, (byte)0xe9
    };

    byte[] actual = EncryptionUtility.hash(HashAlgorithm.SHA_256, "hello world".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(actual, expected);
  }

  public void testMd5HashMatchesKnownVector ()
    throws Exception {

    byte[] expected = new byte[] {
      0x5e, (byte)0xb6, 0x3b, (byte)0xbb, (byte)0xe0, 0x1e, (byte)0xee, (byte)0xd0,
      (byte)0x93, (byte)0xcb, 0x22, (byte)0xbb, (byte)0x8f, 0x5a, (byte)0xcd, (byte)0xc3
    };

    byte[] actual = EncryptionUtility.hash(HashAlgorithm.MD5, "hello world".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(actual, expected);
  }

  public void testHmacRoundTripReturnsSameValue ()
    throws Exception {

    byte[] secret = "key-bytes-key-bytes-key-bytes-key".getBytes(StandardCharsets.UTF_8);
    Key key = HMACSigningAlgorithm.HMAC_SHA_256.generateKey(secret);
    byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);

    byte[] mac = EncryptionUtility.sign(HMACSigningAlgorithm.HMAC_SHA_256, key, payload);

    Assert.assertTrue(EncryptionUtility.verify(HMACSigningAlgorithm.HMAC_SHA_256, key, payload, mac));
  }

  public void testHmacRejectsTamperedPayload ()
    throws Exception {

    byte[] secret = "secret-key-secret-key-secret-key".getBytes(StandardCharsets.UTF_8);
    Key key = HMACSigningAlgorithm.HMAC_SHA_256.generateKey(secret);

    byte[] mac = EncryptionUtility.sign(HMACSigningAlgorithm.HMAC_SHA_256, key, "first".getBytes(StandardCharsets.UTF_8));

    Assert.assertFalse(EncryptionUtility.verify(HMACSigningAlgorithm.HMAC_SHA_256, key, "second".getBytes(StandardCharsets.UTF_8), mac));
  }

  public void testRsaSignAndVerifyRoundTrip ()
    throws Exception {

    KeyPair keyPair = EncryptionUtility.generateKeyPair(AsymmetricAlgorithm.RSA);
    byte[] message = "rsa-sign-test".getBytes(StandardCharsets.UTF_8);

    byte[] signature = EncryptionUtility.sign(RSASigningAlgorithm.SHA_256_WITH_RSA, keyPair.getPrivate(), message);

    Assert.assertTrue(EncryptionUtility.verify(RSASigningAlgorithm.SHA_256_WITH_RSA, keyPair.getPublic(), message, signature));
  }

  public void testSymmetricEncryptAndDecryptRoundTrip ()
    throws Exception {

    Key key = EncryptionUtility.generateKey(SymmetricAlgorithm.AES);
    byte[] payload = "secret content".getBytes(StandardCharsets.UTF_8);

    byte[] ciphertext = EncryptionUtility.encrypt(key, payload);
    byte[] plaintext = EncryptionUtility.decrypt(key, ciphertext);

    Assert.assertEquals(plaintext, payload);
  }

  public void testSerializeAndDeserializeSecretKey ()
    throws Exception {

    Key original = EncryptionUtility.generateKey(SymmetricAlgorithm.AES);

    byte[] bytes = EncryptionUtility.serializeKey(original);
    Key restored = EncryptionUtility.deserializeKey(bytes);

    Assert.assertEquals(restored.getAlgorithm(), original.getAlgorithm());
    Assert.assertEquals(restored.getEncoded(), original.getEncoded());
  }

  public void testConvertToBlockWrapsByWidth () {

    String wrapped = EncryptionUtility.convertToBlock("abcdefghij", 3);

    Assert.assertEquals(wrapped, "abc\ndef\nghi\nj");
  }

  public void testConvertToBlockHandlesZeroWidth () {

    String wrapped = EncryptionUtility.convertToBlock("abc", 0);

    Assert.assertEquals(wrapped, "a\nb\nc");
  }

  public void testHashAlgorithmExposesJcaNames () {

    Assert.assertEquals(HashAlgorithm.SHA_256.getAlgorithmName(), "SHA-256");
    Assert.assertEquals(HashAlgorithm.MD5.getAlgorithmName(), "MD5");
  }
}
