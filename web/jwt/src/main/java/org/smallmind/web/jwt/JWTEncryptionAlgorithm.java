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
import java.security.PrivateKey;
import java.security.PublicKey;
import org.smallmind.nutsnbolts.security.ECDSASigningAlgorithm;
import org.smallmind.nutsnbolts.security.HMACSigningAlgorithm;
import org.smallmind.nutsnbolts.security.RSASigningAlgorithm;

/**
 * Enumeration of supported JWT signing algorithms, each delegating to the corresponding cryptographic implementation.
 */
public enum JWTEncryptionAlgorithm {

  HS256("HS256") {
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign (PrivateKey privateKey, String prologue)
      throws Exception {

      return HMACSigningAlgorithm.HMAC_SHA_256.sign(privateKey, prologue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verify (PublicKey publicKey, String[] pieces, boolean urlSafe)
      throws Exception {

      return HMACSigningAlgorithm.HMAC_SHA_256.verify(publicKey, pieces, urlSafe);
    }
  },
  RS256("RS256") {
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign (PrivateKey privateKey, String prologue)
      throws Exception {

      return RSASigningAlgorithm.SHA_256_WITH_RSA.sign(privateKey, prologue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verify (PublicKey publicKey, String[] pieces, boolean urlSafe)
      throws Exception {

      return RSASigningAlgorithm.SHA_256_WITH_RSA.verify(publicKey, pieces, urlSafe);
    }
  },
  EDDSA("EdDSA") {
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] sign (PrivateKey privateKey, String prologue)
      throws Exception {

      return ECDSASigningAlgorithm.ECDSA_USING_SHA_ALGORITHM.sign(privateKey, prologue.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean verify (PublicKey publicKey, String[] pieces, boolean urlSafe)
      throws Exception {

      return ECDSASigningAlgorithm.ECDSA_USING_SHA_ALGORITHM.verify(publicKey, pieces, urlSafe);
    }
  };

  private final String code;

  JWTEncryptionAlgorithm (String code) {

    this.code = code;
  }

  /**
   * Returns the JWT header algorithm identifier string for this algorithm.
   *
   * @return the algorithm code (e.g. {@code "HS256"}, {@code "RS256"}, {@code "EdDSA"})
   */
  public String getCode () {

    return code;
  }

  /**
   * Produces a signature over the given prologue bytes using the supplied private key.
   *
   * @param privateKey the key used to generate the signature
   * @param prologue   the UTF-8 string to be signed (typically the JWT header and payload segments)
   * @return the raw signature bytes
   * @throws Exception if the signing operation fails
   */
  public abstract byte[] sign (PrivateKey privateKey, String prologue)
    throws Exception;

  /**
   * Verifies a JWT signature against the supplied public key and token segments.
   *
   * @param publicKey the key used to verify the signature
   * @param pieces    the JWT segments required by the underlying verifier
   * @param urlSafe   {@code true} if the token uses URL-safe Base64 encoding
   * @return {@code true} if the signature is valid
   * @throws Exception if the verification operation fails
   */
  public abstract boolean verify (PublicKey publicKey, String[] pieces, boolean urlSafe)
    throws Exception;
}
