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

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.smallmind.nutsnbolts.security.HMACSigningAlgorithm;
import org.testng.annotations.Test;

/**
 * Exercises the wrong-key-type branches of {@link JWTEncryptionAlgorithm}. {@code RS256} and
 * {@code EdDSA} cast the supplied {@link Key} to {@code PrivateKey}/{@code PublicKey}, so feeding the
 * wrong key type must fail rather than silently misbehave. These complement the all-three-algorithm
 * sign/verify round trips in {@code JWTEncryptionAlgorithmTest}.
 */
@Test(groups = "unit")
public class JWTEncryptionAlgorithmNegativeTest {

  private Key symmetricKey () {

    return HMACSigningAlgorithm.HMAC_SHA_256.generateKey("a-shared-secret-of-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void testRs256SignRejectsSymmetricKey ()
    throws Exception {

    // RS256.sign casts to PrivateKey; an HMAC SecretKey is neither, so the cast fails.
    JWTEncryptionAlgorithm.RS256.sign(symmetricKey(), "header.payload");
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void testEdDsaSignRejectsSymmetricKey ()
    throws Exception {

    JWTEncryptionAlgorithm.EDDSA.sign(symmetricKey(), "header.payload");
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void testRs256VerifyRejectsSymmetricKey ()
    throws Exception {

    // RS256.verify casts to PublicKey.
    JWTEncryptionAlgorithm.RS256.verify(symmetricKey(), new String[] {"header", "payload", "AAAA"}, false);
  }

  public void testRs256RejectsEdDsaSigningKey ()
    throws Exception {

    // An Ed25519 private key is a PrivateKey, so the cast in RS256.sign succeeds, but the underlying
    // RSA signer must reject the foreign key. The failure type is provider-dependent, so any Exception
    // is acceptable here.
    KeyPair edPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

    try {
      JWTEncryptionAlgorithm.RS256.sign(edPair.getPrivate(), "header.payload");
      org.testng.Assert.fail("Expected signing with a non-RSA private key to fail");
    } catch (Exception exception) {
      // Expected: the RSA signer rejects an Edwards-curve key.
    }
  }
}
