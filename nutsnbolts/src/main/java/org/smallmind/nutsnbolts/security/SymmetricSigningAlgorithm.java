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
 * Marker for symmetric signing algorithms that can generate and verify MACs.
 */
public interface SymmetricSigningAlgorithm extends SecurityAlgorithm {

  /**
   * Produces a MAC over the provided data using the given key.
   *
   * @param key  the secret key
   * @param data the data to sign
   * @return the MAC bytes
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid for the algorithm
   */
  default byte[] sign (Key key, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException {

    return EncryptionUtility.sign(this, key, data);
  }

  /**
   * Verifies a JWT-like three-part structure using this algorithm.
   *
   * @param key     the secret key
   * @param parts   the token sections (header, payload, signature)
   * @param urlSafe whether the signature is URL-safe Base64 encoded
   * @return {@code true} if the signature is valid
   * @throws IOException              if the signature cannot be decoded
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid for the algorithm
   * @throws SignatureException       if verification fails
   */
  default boolean verify (Key key, String[] parts, boolean urlSafe)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    return EncryptionUtility.verify(this, key, (parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8), urlSafe ? Base64Codec.urlSafeDecode(parts[2]) : Base64Codec.decode(parts[2]));
  }
}
