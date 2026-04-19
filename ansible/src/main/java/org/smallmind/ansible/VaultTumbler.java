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
 * Low-level cryptographic engine for Ansible vault AES256 payloads.
 *
 * <p>Key derivation follows Ansible's vault 1.1/1.2 specification exactly:
 * PBKDF2WithHmacSHA256 is run for 10&thinsp;000 iterations against a 32-byte salt to produce
 * 640 bits of key material.  The material is partitioned as follows:
 * <ul>
 *   <li>bytes 0–31: 256-bit AES key used for AES/CTR/PKCS7Padding encryption</li>
 *   <li>bytes 32–63: 256-bit HMAC-SHA256 key used to authenticate the ciphertext</li>
 *   <li>bytes 64–79: 128-bit AES-CTR initialization vector</li>
 * </ul>
 *
 * <p>The HMAC is computed over the raw ciphertext (encrypt-then-MAC).  On decryption the
 * computed HMAC is compared against the stored value; a mismatch raises
 * {@link VaultPasswordException}, indicating a wrong password rather than data corruption.
 *
 * <p>Instances are not thread-safe because {@link Mac} is stateful.  Create one
 * {@code VaultTumbler} per encryption or decryption operation.
 */
public class VaultTumbler {

  private static final String AES_ALGORITHM = "AES/CTR/PKCS7Padding";

  private final Mac mac;
  private final SecretKeySpec aesKey;
  private final byte[] iv = new byte[16];
  private final byte[] salt;

  /**
   * Constructs a tumbler for encryption, generating a fresh random salt.
   *
   * <p>The salt is produced by {@link ThreadLocalRandom} and is 32 bytes long.
   *
   * @param password vault password; converted to a {@code char[]} for {@link PBEKeySpec}
   * @throws VaultCodecException if the JCA provider does not support PBKDF2WithHmacSHA256
   *                             or HmacSHA256, or if key derivation fails for any other reason
   */
  public VaultTumbler (String password)
    throws VaultCodecException {

    this(password, generateSalt());
  }

  /**
   * Constructs a tumbler using a caller-supplied salt.
   *
   * <p>Use this constructor for decryption, passing the salt read from the vault file so that
   * key derivation reproduces the same AES key, HMAC key, and IV that were used during encryption.
   *
   * @param password vault password; converted to a {@code char[]} for {@link PBEKeySpec}
   * @param salt     32-byte PBKDF2 salt; must be the same value that was used at encryption time
   * @throws VaultCodecException if the JCA provider does not support the required algorithms
   *                             or if key derivation fails
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
   * Generates a 32-byte cryptographically random salt.
   *
   * @return freshly generated 32-byte salt array
   */
  private static byte[] generateSalt () {

    byte[] salt = new byte[32];

    ThreadLocalRandom.current().nextBytes(salt);

    return salt;
  }

  /**
   * Encrypts plaintext using AES-CTR and computes an authenticating HMAC over the ciphertext.
   *
   * @param original plaintext bytes to encrypt; may be empty but must not be {@code null}
   * @return a {@link VaultCake} containing the salt (for key re-derivation on decryption),
   *         the HMAC-SHA256 tag (for authentication), and the AES-CTR ciphertext
   * @throws VaultCodecException if the JCA provider rejects the key or algorithm parameters,
   *                             or if the cipher operation itself fails
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
   * Verifies the HMAC and, if valid, decrypts the ciphertext.
   *
   * <p>The HMAC computed from the derived HMAC key and {@code encrypted} is compared against
   * {@code hmac} using {@link Arrays#equals}.  A mismatch indicates a wrong password (since the
   * HMAC key is derived from the password) and raises {@link VaultPasswordException}.
   *
   * @param hmac      the 32-byte HMAC-SHA256 tag stored in the vault file
   * @param encrypted the ciphertext bytes stored in the vault file
   * @return the decrypted plaintext bytes
   * @throws VaultPasswordException if the computed HMAC does not match {@code hmac}, indicating
   *                                that the password used to construct this tumbler is incorrect
   * @throws VaultCodecException    if the JCA provider rejects the key or algorithm parameters,
   *                                or if the cipher operation fails for a non-password reason
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
