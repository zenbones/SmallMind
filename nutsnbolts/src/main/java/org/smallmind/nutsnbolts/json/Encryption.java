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
package org.smallmind.nutsnbolts.json;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

/**
 * Symmetric encryption algorithms available for securing {@link BinaryData} payloads.
 */
public enum Encryption {

  /**
   * Authenticated AES encryption using AES/GCM/NoPadding with a 128-bit tag. Every call generates a fresh random 96-bit initialization vector, and the
   * payload is laid out as the 12-byte IV followed by the ciphertext and tag.
   */
  AES {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    /**
     * Encrypts the supplied bytes with AES/GCM/NoPadding using the provided key and a freshly generated random IV.
     *
     * @param key           the AES key used for encryption
     * @param toBeEncrypted the clear-text bytes to encrypt
     * @return the IV followed by the ciphertext and authentication tag
     * @throws NoSuchAlgorithmException           if AES/GCM is unavailable
     * @throws NoSuchPaddingException             if the transformation is rejected by the provider
     * @throws InvalidKeyException                if the key is not valid for AES
     * @throws InvalidAlgorithmParameterException if the GCM parameters are rejected by the provider
     * @throws IllegalBlockSizeException          if the cipher rejects the input
     * @throws BadPaddingException                if the cipher rejects the input
     */
    @Override
    public byte[] encrypt (Key key, byte[] toBeEncrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

      byte[] iv = generateIv();
      byte[] encrypted = EncryptionUtility.encrypt(key, TRANSFORMATION, toBeEncrypted, new GCMParameterSpec(TAG_BITS, iv));
      byte[] payload = new byte[iv.length + encrypted.length];

      System.arraycopy(iv, 0, payload, 0, iv.length);
      System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

      return payload;
    }

    /**
     * Decrypts bytes previously produced by {@link #encrypt(Key, byte[])}, verifying the authentication tag.
     *
     * @param key       the AES key used for decryption
     * @param encrypted the IV followed by the ciphertext and authentication tag
     * @return the decrypted clear-text bytes
     * @throws NoSuchAlgorithmException           if AES/GCM is unavailable
     * @throws NoSuchPaddingException             if the transformation is rejected by the provider
     * @throws InvalidKeyException                if the key is not valid for AES
     * @throws InvalidAlgorithmParameterException if the GCM parameters are invalid
     * @throws IllegalBlockSizeException          if the payload is too short to hold an IV and a tag
     * @throws BadPaddingException                if authentication fails (wrong key, tampering, or a payload in the old fixed-IV format)
     */
    @Override
    public byte[] decrypt (Key key, byte[] encrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

      if (encrypted.length < IV_LENGTH + (TAG_BITS / 8)) {
        throw new IllegalBlockSizeException("The encrypted payload is too short to contain an IV and an authentication tag");
      } else {

        byte[] body = new byte[encrypted.length - IV_LENGTH];

        System.arraycopy(encrypted, IV_LENGTH, body, 0, body.length);

        return EncryptionUtility.decrypt(key, TRANSFORMATION, body, new GCMParameterSpec(TAG_BITS, encrypted, 0, IV_LENGTH));
      }
    }

    // The single place the IV originates; a provider-generated IV (for FIPS approved mode) would replace this and the spec passed above
    private byte[] generateIv () {

      byte[] iv = new byte[IV_LENGTH];

      SECURE_RANDOM.nextBytes(iv);

      return iv;
    }
  };

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * Encrypts the provided bytes using this algorithm and the supplied key.
   *
   * @param key           the key required by the algorithm
   * @param toBeEncrypted the clear-text bytes to encrypt
   * @return the resulting ciphertext bytes
   * @throws Exception if the algorithm, padding, key, or input are invalid
   */
  public abstract byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws Exception;

  /**
   * Decrypts bytes that were produced by the matching {@link #encrypt(Key, byte[])} call.
   *
   * @param key       the key used for decryption
   * @param encrypted the ciphertext bytes to decrypt
   * @return the decrypted clear-text bytes
   * @throws Exception if decryption fails or the key or algorithm are invalid
   */
  public abstract byte[] decrypt (Key key, byte[] encrypted)
    throws Exception;
}
