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
import java.security.NoSuchAlgorithmException;
import org.smallmind.nutsnbolts.lang.FormattedRuntimeException;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.HMACSigningAlgorithm;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

/**
 * A {@link JWTKeyMaster} that derives an HMAC-SHA-256 key from a shared secret string for symmetric JWT signing and verification.
 */
public class SymmetricJWTKeyMaster implements JWTKeyMaster {

  private final Key key;

  /**
   * Builds the HMAC-SHA-256 signing key from a shared secret. HS256 requires a key of at least 256
   * bits, so a secret whose UTF-8 encoding is already at least 32 bytes is used directly, while a
   * shorter secret is SHA-256 hashed into a 256-bit key. Secrets of at least 32 bytes therefore keep
   * the same key as before, and any secret length is accepted.
   *
   * @param secret the shared secret used as the HMAC key material
   */
  public SymmetricJWTKeyMaster (String secret) {

    byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);

    if (secretBytes.length < 32) {
      try {
        secretBytes = EncryptionUtility.hash(HashAlgorithm.SHA_256, secretBytes);
      } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
        throw new FormattedRuntimeException(noSuchAlgorithmException);
      }
    }

    key = HMACSigningAlgorithm.HMAC_SHA_256.generateKey(secretBytes);
  }

  /**
   * Returns the JWT signing algorithm associated with the key held by this master.
   *
   * @return the encryption/signing algorithm
   */
  @Override
  public JWTEncryptionAlgorithm getEncryptionAlgorithm () {

    return JWTEncryptionAlgorithm.HS256;
  }

  /**
   * Returns the cryptographic key used for signing or verification.
   *
   * @return the signing or verification key
   */
  @Override
  public Key getKey () {

    return key;
  }
}
