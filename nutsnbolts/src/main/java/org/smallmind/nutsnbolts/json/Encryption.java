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
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

/**
 * Symmetric encryption algorithms available for securing {@link BinaryData} payloads.
 */
public enum Encryption {

  /**
   * AES encryption in CBC mode with PKCS5 padding using a fixed initialization vector.
   */
  AES {

    private final byte[] iv = new byte[] {0x47, 0x6C, 0x75, 0x20, 0x4D, 0x6F, 0x62, 0x69, 0x6C, 0x65, 0x20, 0x47, 0x61, 0x6D, 0x65, 0x73};

    /**
     * Encrypts the supplied bytes with AES/CBC/PKCS5Padding using the provided key.
     *
     * @param key           the AES key used for encryption
     * @param toBeEncrypted the clear-text bytes to encrypt
     * @return the ciphertext bytes
     * @throws NoSuchAlgorithmException           if the AES algorithm is unavailable
     * @throws InvalidAlgorithmParameterException if the IV parameter is invalid
     * @throws NoSuchPaddingException             if PKCS5Padding is unavailable
     * @throws InvalidKeyException                if the key is not valid for AES
     * @throws IllegalBlockSizeException          if the input length is invalid
     * @throws BadPaddingException                if a padding error occurs
     */
    @Override
    public byte[] encrypt (Key key, byte[] toBeEncrypted)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

      return EncryptionUtility.encrypt(key, "AES/CBC/PKCS5Padding", toBeEncrypted, new IvParameterSpec(iv));
    }

    /**
     * Decrypts bytes previously encrypted with {@link #encrypt(Key, byte[])} using AES/CBC/PKCS5Padding.
     *
     * @param key       the AES key used for decryption
     * @param encrypted the ciphertext bytes to decrypt
     * @return the decrypted clear-text bytes
     * @throws NoSuchAlgorithmException           if the AES algorithm is unavailable
     * @throws NoSuchPaddingException             if PKCS5Padding is unavailable
     * @throws InvalidKeyException                if the key is not valid for AES
     * @throws InvalidAlgorithmParameterException if the IV parameter is invalid
     * @throws IllegalBlockSizeException          if the input length is invalid
     * @throws BadPaddingException                if a padding error occurs during decryption
     */
    @Override
    public byte[] decrypt (Key key, byte[] encrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

      return EncryptionUtility.decrypt(key, "AES/CBC/PKCS5Padding", encrypted, new IvParameterSpec(iv));
    }
  };

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
