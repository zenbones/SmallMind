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
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import org.smallmind.nutsnbolts.security.InvalidPasswordException;

/**
 * PBKDF2-based password hashing and verification engine that prepends a random salt to each stored hash.
 */
public class PBKDF2PasswordEngine implements PasswordEngine {

  private static final SecureRandom SECURE_RANDOM;

  // Changes to these values will alter the resulting encryption
  private final int iterations;
  private final int saltLength;
  private final int desiredKeyLength;

  static {

    try {
      SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new StaticInitializationError(noSuchAlgorithmException);
    }

    // force the instance to seed itself
    SECURE_RANDOM.nextBytes(new byte[256]);
  }

  /**
   * Constructs an engine with default PBKDF2 parameters (50,000 iterations, 32-byte salt, 256-bit derived key).
   */
  public PBKDF2PasswordEngine () {

    this(50000, 32, 256);
  }

  /**
   * Constructs an engine with a custom iteration count and all other parameters set to their defaults.
   *
   * @param iterations the number of PBKDF2 iterations to perform
   */
  public PBKDF2PasswordEngine (int iterations) {

    this(iterations, 32, 256);
  }

  /**
   * Constructs an engine with fully customized PBKDF2 parameters.
   *
   * @param iterations       the number of PBKDF2 iterations; higher values increase computation cost
   * @param saltLength       the number of random salt bytes prepended to each stored hash
   * @param desiredKeyLength the desired length of the derived key in bits
   */
  public PBKDF2PasswordEngine (int iterations, int saltLength, int desiredKeyLength) {

    this.iterations = iterations;
    this.saltLength = saltLength;
    this.desiredKeyLength = desiredKeyLength;
  }

  /**
   * Hashes the password with a freshly generated random salt using PBKDF2WithHmacSHA1 and returns the concatenated salt and derived key encoded as Base64.
   *
   * @param password the plaintext password to hash; must not be {@code null} or empty
   * @return a Base64-encoded string containing the random salt followed by the derived key bytes
   * @throws IOException              if Base64 encoding fails
   * @throws NoSuchAlgorithmException if the PBKDF2WithHmacSHA1 algorithm is not available
   * @throws InvalidKeySpecException  if the key derivation parameters are invalid
   * @throws InvalidPasswordException if the password is {@code null} or empty
   */
  @Override
  public String encrypt (String password)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidPasswordException {

    if ((password == null) || password.isEmpty()) {
      throw new InvalidPasswordException("Passwords must not be empty");
    } else {

      byte[] saltBytes = new byte[saltLength];
      byte[] hashedPasswordBytes;
      byte[] compiledPasswordBytes;

      SECURE_RANDOM.nextBytes(saltBytes);
      hashedPasswordBytes = hash(saltBytes, password);
      compiledPasswordBytes = new byte[saltBytes.length + hashedPasswordBytes.length];

      System.arraycopy(saltBytes, 0, compiledPasswordBytes, 0, saltBytes.length);
      System.arraycopy(hashedPasswordBytes, 0, compiledPasswordBytes, saltBytes.length, hashedPasswordBytes.length);

      return Base64Codec.encode(compiledPasswordBytes);
    }
  }

  /**
   * Verifies a candidate password against a stored PBKDF2 salt+derived-key value produced by {@link #encrypt(String)}.
   * Returns {@code false} without throwing if either argument is {@code null} or empty.
   *
   * @param password the candidate plaintext password to check
   * @param stored   the Base64-encoded salt+derived-key string previously produced by {@link #encrypt(String)}
   * @return {@code true} if the candidate password produces the same derived key as the stored value
   * @throws IOException              if Base64 decoding of the stored value fails
   * @throws NoSuchAlgorithmException if the PBKDF2WithHmacSHA1 algorithm is not available
   * @throws InvalidKeySpecException  if the key derivation parameters are invalid
   */
  @Override
  public boolean match (String password, String stored)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    if ((password == null) || password.isEmpty() || (stored == null) || stored.isEmpty()) {

      return false;
    } else {

      byte[] compiledPasswordBytes = Base64Codec.decode(stored);
      byte[] saltBytes = new byte[saltLength];
      byte[] hashedPasswordBytes = new byte[compiledPasswordBytes.length - saltLength];

      System.arraycopy(compiledPasswordBytes, 0, saltBytes, 0, saltLength);
      System.arraycopy(compiledPasswordBytes, saltLength, hashedPasswordBytes, 0, compiledPasswordBytes.length - saltLength);

      return Arrays.equals(hash(saltBytes, password), hashedPasswordBytes);
    }
  }

  /**
   * Derives a key from the supplied salt and password using PBKDF2WithHmacSHA1.
   *
   * @param salt     the random salt bytes to use during key derivation
   * @param password the plaintext password
   * @return the raw derived key bytes
   * @throws NoSuchAlgorithmException if the PBKDF2WithHmacSHA1 algorithm is not available
   * @throws InvalidKeySpecException  if the key derivation parameters are invalid
   */
  private byte[] hash (byte[] salt, String password)
    throws NoSuchAlgorithmException, InvalidKeySpecException {

    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, desiredKeyLength));

    return key.getEncoded();
  }
}
