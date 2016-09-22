/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.smallmind.nutsnbolts.http.Base64Codec;

public class PasswordUtility {

  private static final int ITERATIONS = 20 * 1000;
  private static final int SALT_LENGTH = 32;
  private static final int DESIRED_KEY_LENGTH = 256;

  public static String encrypt (String password)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    byte[] saltBytes = SecureRandom.getInstance("SHA1PRNG").generateSeed(SALT_LENGTH);
    byte[] hashedPasswordBytes = hash(saltBytes, password);
    byte[] compiledPasswordBytes = new byte[saltBytes.length + hashedPasswordBytes.length];

    System.arraycopy(saltBytes, 0, compiledPasswordBytes, 0, saltBytes.length);
    System.arraycopy(hashedPasswordBytes, 0, compiledPasswordBytes, saltBytes.length, hashedPasswordBytes.length);

    return Base64Codec.encode(compiledPasswordBytes);
  }

  public static boolean match (String password, String stored)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    byte[] compiledPasswordBytes = Base64Codec.decode(stored);
    byte[] salBytes = new byte[SALT_LENGTH];
    byte[] hashedPasswordBytes = new byte[compiledPasswordBytes.length - SALT_LENGTH];

    System.arraycopy(compiledPasswordBytes, 0, salBytes, 0, SALT_LENGTH);
    System.arraycopy(compiledPasswordBytes, SALT_LENGTH, hashedPasswordBytes, 0, compiledPasswordBytes.length - SALT_LENGTH);

    return Arrays.equals(hash(salBytes, password), hashedPasswordBytes);
  }

  private static byte[] hash (byte[] salt, String password)
    throws NoSuchAlgorithmException, InvalidKeySpecException {

    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    SecretKey key = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, DESIRED_KEY_LENGTH));

    return key.getEncoded();
  }
}
