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
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import org.smallmind.nutsnbolts.security.InvalidPasswordException;

/**
 * PBKDF2-based password hashing/verification utility.
 */
public class Argon2IDPasswordEngine implements PasswordEngine {

  private static final SecureRandom SECURE_RANDOM;

  // Changes to these values will alter the resulting encryption
  private final int iterations;
  private final int saltLength;
  private final int memLimit;
  private final int hashLength;
  private final int parallelism;

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
  public Argon2IDPasswordEngine () {

    this(2, 32, 66536, 32, 1);
  }

  /**
   * Creates an engine with custom iterations and default salt/key lengths.
   *
   * @param iterations number of PBKDF2 iterations
   */
  public Argon2IDPasswordEngine (int iterations) {

    this(iterations, 32, 66536, 32, 1);
  }

  /**
   * Creates an engine with custom parameters.
   *
   * @param iterations  number of Argon2_id iterations
   * @param saltLength  bytes of random salt to prepend to stored hashes
   * @param memLimit    limit to bytes of memory consumed (this relates directly to attack resistance with a recommended lower bound of 64k)
   * @param hashLength  length of the hash in bytes (also directly relates to attack resistance with recommended lower bound of 32 bytes)
   * @param parallelism number of threads used in the computation
   */
  public Argon2IDPasswordEngine (int iterations, int saltLength, int memLimit, int hashLength, int parallelism) {

    this.iterations = iterations;
    this.saltLength = saltLength;
    this.memLimit = memLimit;
    this.hashLength = hashLength;
    this.parallelism = parallelism;
  }

  /**
   * Hashes the password with a random salt and returns the salt+hash encoded as Base64.
   *
   * @param password the plain-text password
   * @return Base64 string containing salt concatenated with derived key
   * @throws IOException              if encoding fails
   * @throws InvalidPasswordException if the password is {@code null} or empty
   */
  @Override
  public String encrypt (String password)
    throws IOException, InvalidPasswordException {

    if ((password == null) || password.isEmpty()) {
      throw new InvalidPasswordException("Passwords must not be empty");
    } else {

      Argon2BytesGenerator generator = new Argon2BytesGenerator();
      byte[] result = new byte[saltLength + hashLength];
      byte[] salt = new byte[saltLength];
      byte[] hashed = new byte[hashLength];

      SECURE_RANDOM.nextBytes(salt);
      generator.init(new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13).withIterations(iterations).withMemoryAsKB(memLimit).withParallelism(parallelism).withSalt(salt).build());

      generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), hashed, 0, hashed.length);
      System.arraycopy(salt, 0, result, 0, salt.length);
      System.arraycopy(hashed, 0, result, salt.length, hashed.length);

      return Base64Codec.encode(result);
    }
  }

  /**
   * Checks whether the supplied password matches a stored salt+hash.
   *
   * @param password candidate password
   * @param stored   Base64 salt+hash string produced by {@link #encrypt(String)}
   * @return {@code true} if the password matches
   * @throws IOException if decoding fails
   */
  @Override
  public boolean match (String password, String stored)
    throws IOException {

    if ((password == null) || password.isEmpty() || (stored == null) || stored.isEmpty()) {

      return false;
    } else {

      Argon2BytesGenerator verifier = new Argon2BytesGenerator();
      byte[] compiledPasswordBytes = Base64Codec.decode(stored);
      byte[] saltBytes = new byte[saltLength];
      byte[] hashedPasswordBytes = new byte[compiledPasswordBytes.length - saltLength];
      byte[] result = new byte[hashLength];

      System.arraycopy(compiledPasswordBytes, 0, saltBytes, 0, saltLength);
      System.arraycopy(compiledPasswordBytes, saltLength, hashedPasswordBytes, 0, compiledPasswordBytes.length - saltLength);

      verifier.init(new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13).withIterations(iterations).withMemoryAsKB(memLimit).withParallelism(parallelism).withSalt(saltBytes).build());
      verifier.generateBytes(password.getBytes(StandardCharsets.UTF_8), result, 0, result.length);

      return Arrays.equals(hashedPasswordBytes, result);
    }
  }
}
