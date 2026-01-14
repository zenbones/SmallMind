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
 * PBKDF2-based password hashing/verification utility.
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
   * Creates an engine with default settings (50,000 iterations, 32-byte salt, 256-bit key).
   */
  public PBKDF2PasswordEngine () {

    this(50000, 32, 256);
  }

  /**
   * Creates an engine with custom iterations and default salt/key lengths.
   *
   * @param iterations number of PBKDF2 iterations
   */
  public PBKDF2PasswordEngine (int iterations) {

    this(iterations, 32, 256);
  }

  /**
   * Creates an engine with custom parameters.
   *
   * @param iterations       number of PBKDF2 iterations
   * @param saltLength       bytes of random salt to prepend to stored hashes
   * @param desiredKeyLength desired derived key length in bits
   */
  public PBKDF2PasswordEngine (int iterations, int saltLength, int desiredKeyLength) {

    this.iterations = iterations;
    this.saltLength = saltLength;
    this.desiredKeyLength = desiredKeyLength;
  }

  /**
   * Hashes the password with a random salt and returns the salt+hash encoded as Base64.
   *
   * @param password the plain-text password
   * @return Base64 string containing salt concatenated with derived key
   * @throws IOException              if encoding fails
   * @throws NoSuchAlgorithmException if PBKDF2 algorithm is unavailable
   * @throws InvalidKeySpecException  if key derivation parameters are invalid
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
   * Checks whether the supplied password matches a stored salt+hash.
   *
   * @param password candidate password
   * @param stored   Base64 salt+hash string produced by {@link #encrypt(String)}
   * @return {@code true} if the password matches
   * @throws IOException              if decoding fails
   * @throws NoSuchAlgorithmException if PBKDF2 algorithm is unavailable
   * @throws InvalidKeySpecException  if key derivation parameters are invalid
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
   * Derives a key from the supplied salt and password.
   *
   * @param salt     random salt bytes
   * @param password the password
   * @return derived key bytes
   * @throws NoSuchAlgorithmException if PBKDF2 algorithm is unavailable
   * @throws InvalidKeySpecException  if key derivation parameters are invalid
   */
  private byte[] hash (byte[] salt, String password)
    throws NoSuchAlgorithmException, InvalidKeySpecException {

    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, desiredKeyLength));

    return key.getEncoded();
  }
}
