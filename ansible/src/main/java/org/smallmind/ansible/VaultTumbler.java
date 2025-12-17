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
package org.smallmind.ansible;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

/**
 * Performs encryption/decryption and integrity validation for Ansible vault payloads.
 * Uses PBKDF2WithHmacSHA256 to derive AES-CTR and HMAC keys, mirroring Ansible's vault 1.1/1.2 format.
 */
public class VaultTumbler {

  private static final String AES_ALGORITHM = "AES/CTR/PKCS7Padding";

  private final Mac mac;
  private final SecretKeySpec aesKey;
  private final byte[] iv = new byte[16];
  private final byte[] salt;

  /**
   * Constructs a tumbler with a newly generated salt.
   *
   * @param password vault password
   * @throws VaultCodecException if key derivation fails
   */
  public VaultTumbler (String password)
    throws VaultCodecException {

    this(password, generateSalt());
  }

  /**
   * Constructs a tumbler using the provided salt.
   *
   * @param password vault password
   * @param salt     32-byte salt used for PBKDF2 key derivation
   * @throws VaultCodecException if key derivation fails
   */
  public VaultTumbler (String password, byte[] salt)
    throws VaultCodecException {

    try {

      SecretKey pbkdf2Key;
      SecretKeyFactory pbkdf2KeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      SecretKeySpec hmacKey;
      byte[] pbkdf2KeyBytes;
      byte[] aesKeyBytes = new byte[32];
      byte[] hmacKeyBytes = new byte[32];

      this.salt = salt;

      pbkdf2Key = pbkdf2KeyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, 10000, 640));
      pbkdf2KeyBytes = pbkdf2Key.getEncoded();

      System.arraycopy(pbkdf2KeyBytes, 0, aesKeyBytes, 0, 32);
      System.arraycopy(pbkdf2KeyBytes, 32, hmacKeyBytes, 0, 32);
      System.arraycopy(pbkdf2KeyBytes, 64, iv, 0, 16);

      aesKey = new SecretKeySpec(aesKeyBytes, "AES");
      hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");

      mac = Mac.getInstance("HmacSHA256");
      mac.init(hmacKey);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException exception) {
      throw new VaultCodecException(exception);
    }
  }

  /**
   * Generates a random 32-byte salt using {@link ThreadLocalRandom}.
   *
   * @return new random salt
   */
  private static byte[] generateSalt () {

    byte[] salt = new byte[32];

    ThreadLocalRandom.current().nextBytes(salt);

    return salt;
  }

  /**
   * Encrypts plaintext bytes, returning the vault payload components.
   *
   * @param original plaintext bytes to encrypt
   * @return container holding the salt, HMAC, and encrypted bytes
   * @throws VaultCodecException if encryption or MAC calculation fails
   */
  public VaultCake encrypt (byte[] original)
    throws VaultCodecException {

    try {

      byte[] encrypted = EncryptionUtility.encrypt(aesKey, AES_ALGORITHM, original, new IvParameterSpec(iv));

      return new VaultCake(salt, mac.doFinal(encrypted), encrypted);
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException exception) {
      throw new VaultCodecException(exception);
    }
  }

  /**
   * Decrypts ciphertext after verifying the provided HMAC.
   *
   * @param hmac      expected HMAC for the encrypted payload
   * @param encrypted encrypted payload bytes
   * @return decrypted plaintext bytes
   * @throws VaultPasswordException if the supplied password does not validate against the HMAC
   * @throws VaultCodecException    if decryption fails for other reasons
   */
  public byte[] decrypt (byte[] hmac, byte[] encrypted)
    throws VaultCodecException {

    if (!Arrays.equals(hmac, mac.doFinal(encrypted))) {
      throw new VaultPasswordException("Wrong password");
    } else {
      try {

        return EncryptionUtility.decrypt(aesKey, AES_ALGORITHM, encrypted, new IvParameterSpec(iv));
      } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException exception) {
        throw new VaultCodecException(exception);
      }
    }
  }
}
