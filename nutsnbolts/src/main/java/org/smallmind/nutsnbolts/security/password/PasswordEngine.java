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
package org.smallmind.nutsnbolts.security.password;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.smallmind.nutsnbolts.security.InvalidPasswordException;

/**
 * Service interface for password hashing and verification, abstracting the underlying key-derivation algorithm.
 */
public interface PasswordEngine {

  /**
   * Hashes the plaintext password, typically with a secure random salt, and returns an encoded string
   * suitable for persistent storage.
   *
   * @param password the plaintext password to hash; must not be {@code null} or empty
   * @return an encoded string (commonly Base64) containing the salt concatenated with the derived key
   * @throws IOException              if encoding of the result fails
   * @throws NoSuchAlgorithmException if the required key-derivation algorithm is not available
   * @throws InvalidKeySpecException  if the key derivation parameters are invalid
   * @throws InvalidPasswordException if the password is {@code null} or empty
   */
  String encrypt (String password)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidPasswordException;

  /**
   * Verifies that the candidate password matches the encoded string previously produced by {@link #encrypt(String)}.
   *
   * @param password the candidate plaintext password to check
   * @param stored   the encoded salt+hash string previously returned by {@link #encrypt(String)}
   * @return {@code true} if the candidate password matches the stored value, {@code false} otherwise
   * @throws IOException              if decoding of the stored value fails
   * @throws NoSuchAlgorithmException if the required key-derivation algorithm is not available
   * @throws InvalidKeySpecException  if the key derivation parameters are invalid
   */
  boolean match (String password, String stored)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException;
}
