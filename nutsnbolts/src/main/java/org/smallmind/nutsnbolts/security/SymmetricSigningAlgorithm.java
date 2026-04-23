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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import org.smallmind.nutsnbolts.http.Base64Codec;

/**
 * Contract for symmetric signing algorithms that can produce and verify message authentication codes (MACs).
 */
public interface SymmetricSigningAlgorithm extends SecurityAlgorithm {

  /**
   * Produces a MAC over the provided data using the given secret key.
   *
   * @param key  the secret key used to compute the MAC
   * @param data the data to authenticate
   * @return the raw MAC bytes
   * @throws NoSuchAlgorithmException if the MAC algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the key is not valid for this algorithm
   */
  default byte[] sign (Key key, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException {

    return EncryptionUtility.sign(this, key, data);
  }

  /**
   * Verifies the MAC of a three-part dot-separated token (e.g., a JWT) using the given secret key.
   *
   * @param key     the secret key used to verify the MAC
   * @param parts   the three token sections where {@code parts[0]} and {@code parts[1]} are the signed material
   *                and {@code parts[2]} is the Base64-encoded MAC
   * @param urlSafe {@code true} if the MAC portion uses URL-safe Base64 encoding
   * @return {@code true} if the MAC is valid, {@code false} otherwise
   * @throws IOException              if the MAC cannot be decoded from its Base64 representation
   * @throws NoSuchAlgorithmException if the MAC algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the key is not valid for this algorithm
   * @throws SignatureException       if the verification operation fails
   */
  default boolean verify (Key key, String[] parts, boolean urlSafe)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    return EncryptionUtility.verify(this, key, (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8), urlSafe ? Base64Codec.urlSafeDecode(parts[2]) : Base64Codec.decode(parts[2]));
  }
}
