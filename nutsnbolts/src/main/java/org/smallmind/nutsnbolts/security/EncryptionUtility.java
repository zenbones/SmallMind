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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyRep;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;

/**
 * Convenience methods for message digesting, symmetric and asymmetric signing/verification, cipher-based encryption and decryption, and key serialization.
 */
public class EncryptionUtility {

  /**
   * Reformats a single-line string into a block of fixed-width lines separated by newline characters.
   *
   * @param singleLine the input string to reformat (whitespace is removed before splitting)
   * @param blockWidth the maximum number of characters per output line; values less than 1 are treated as 1
   * @return a multi-line string with at most {@code blockWidth} characters per line
   */
  public static String convertToBlock (String singleLine, int blockWidth) {

    StringBuilder blockBuilder = new StringBuilder();
    String compressedLine = singleLine.replaceAll("\\s*", "");
    int index = 0;

    if (blockWidth < 1) {
      blockWidth = 1;
    }

    while (index < compressedLine.length()) {
      if (!blockBuilder.isEmpty()) {
        blockBuilder.append("\n");
      }

      blockBuilder.append(compressedLine, index, Math.min(index + blockWidth, compressedLine.length()));
      index += blockWidth;
    }

    return blockBuilder.toString();
  }

  /**
   * Computes a message digest over the supplied bytes using the specified hash algorithm.
   *
   * @param algorithm  the hash algorithm to use
   * @param toBeHashed the data to digest
   * @return the raw digest bytes
   * @throws NoSuchAlgorithmException if the digest algorithm is not available in the current security environment
   */
  public static byte[] hash (HashAlgorithm algorithm, byte[] toBeHashed)
    throws NoSuchAlgorithmException {

    return MessageDigest.getInstance(algorithm.getAlgorithmName()).digest(toBeHashed);
  }

  /**
   * Computes a message authentication code (MAC) over the provided data using the given symmetric algorithm and key.
   *
   * @param algorithm the MAC algorithm to use
   * @param secretKey the secret key to initialize the MAC with
   * @param data      the data to authenticate
   * @return the raw MAC bytes
   * @throws NoSuchAlgorithmException if the MAC algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the key is not valid for this algorithm
   */
  public static byte[] sign (SymmetricSigningAlgorithm algorithm, Key secretKey, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException {

    Mac mac = Mac.getInstance(algorithm.getAlgorithmName());

    mac.init(secretKey);

    return mac.doFinal(data);
  }

  /**
   * Verifies a MAC produced by {@link #sign(SymmetricSigningAlgorithm, Key, byte[])} by recomputing it and performing a constant-time comparison.
   *
   * @param algorithm  the MAC algorithm to use
   * @param secretKey  the secret key to initialize the MAC with
   * @param data       the original data that was authenticated
   * @param signedData the expected MAC bytes to compare against
   * @return {@code true} if the recomputed MAC matches {@code signedData}
   * @throws NoSuchAlgorithmException if the MAC algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the key is not valid for this algorithm
   */
  public static boolean verify (SymmetricSigningAlgorithm algorithm, Key secretKey, byte[] data, byte[] signedData)
    throws NoSuchAlgorithmException, InvalidKeyException {

    Mac mac = Mac.getInstance(algorithm.getAlgorithmName());

    mac.init(secretKey);

    return Arrays.equals(mac.doFinal(data), signedData);
  }

  /**
   * Signs data using an asymmetric algorithm and the given private key.
   *
   * @param algorithm  the signature algorithm to use
   * @param privateKey the private key used to generate the signature
   * @param data       the data to sign
   * @return the raw signature bytes
   * @throws NoSuchAlgorithmException if the signature algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the private key is not valid for this algorithm
   * @throws SignatureException       if the signing operation fails
   */
  public static byte[] sign (AsymmetricSigningAlgorithm algorithm, PrivateKey privateKey, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName());

    signature.initSign(privateKey);
    signature.update(data);

    return signature.sign();
  }

  /**
   * Verifies a signature produced by {@link #sign(AsymmetricSigningAlgorithm, PrivateKey, byte[])} using the corresponding public key.
   *
   * @param algorithm  the signature algorithm to use
   * @param publicKey  the public key used to verify the signature
   * @param data       the original data that was signed
   * @param signedData the signature bytes to verify
   * @return {@code true} if the signature is valid
   * @throws NoSuchAlgorithmException if the signature algorithm is not available in the current security environment
   * @throws InvalidKeyException      if the public key is not valid for this algorithm
   * @throws SignatureException       if the verification operation fails
   */
  public static boolean verify (AsymmetricSigningAlgorithm algorithm, PublicKey publicKey, byte[] data, byte[] signedData)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName());

    signature.initVerify(publicKey);
    signature.update(data);

    return signature.verify(signedData);
  }

  /**
   * Generates a new symmetric secret key for the given algorithm using the default key size.
   *
   * @param algorithm the symmetric algorithm for which to generate a key
   * @return a freshly generated {@link javax.crypto.SecretKey}
   * @throws NoSuchAlgorithmException if the algorithm is not available in the current security environment
   */
  public static Key generateKey (SymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyGenerator keyGenerator;

    keyGenerator = KeyGenerator.getInstance(algorithm.name());
    return keyGenerator.generateKey();
  }

  /**
   * Generates a new asymmetric key pair for the given algorithm using the default key size.
   *
   * @param algorithm the asymmetric algorithm for which to generate a key pair
   * @return a freshly generated {@link KeyPair}
   * @throws NoSuchAlgorithmException if the algorithm is not available in the current security environment
   */
  public static KeyPair generateKeyPair (AsymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyPairGenerator keyPairGenerator;

    keyPairGenerator = KeyPairGenerator.getInstance(algorithm.name());
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Serializes a {@link Key} to a byte array using {@link java.security.KeyRep} so that it can be reconstructed later.
   *
   * @param key the key to serialize (public, private, or secret)
   * @return a byte array containing the serialized key representation
   * @throws IOException if object serialization fails
   */
  public static byte[] serializeKey (Key key)
    throws IOException {

    KeyRep keyRep;
    ObjectOutputStream keyOutputStream;
    ByteArrayOutputStream byteOutputStream;

    keyRep = new KeyRep((key instanceof PublicKey) ? KeyRep.Type.PUBLIC : (key instanceof PrivateKey) ? KeyRep.Type.PRIVATE : KeyRep.Type.SECRET, key.getAlgorithm(), key.getFormat(), key.getEncoded());
    byteOutputStream = new ByteArrayOutputStream();
    keyOutputStream = new ObjectOutputStream(byteOutputStream);
    keyOutputStream.writeObject(keyRep);

    try {
      return byteOutputStream.toByteArray();
    } finally {
      keyOutputStream.close();
    }
  }

  /**
   * Reconstructs a {@link Key} from the byte array produced by {@link #serializeKey(Key)}.
   *
   * @param keyBytes the serialized key bytes previously returned by {@link #serializeKey(Key)}
   * @return the deserialized key
   * @throws IOException            if object deserialization fails
   * @throws ClassNotFoundException if the class of the serialized key cannot be found on the classpath
   */
  public static Key deserializeKey (byte[] keyBytes)
    throws IOException, ClassNotFoundException {

    ObjectInputStream keyInputStream;
    ByteArrayInputStream byteInputStream;

    byteInputStream = new ByteArrayInputStream(keyBytes);
    keyInputStream = new ObjectInputStream(byteInputStream);

    try {
      return (Key)keyInputStream.readObject();
    } finally {
      keyInputStream.close();
    }
  }

  /**
   * Encrypts plaintext using the key's own algorithm name to select the cipher.
   *
   * @param key           the key used to encrypt
   * @param toBeEncrypted the plaintext bytes to encrypt
   * @return the ciphertext bytes
   * @throws NoSuchAlgorithmException  if the cipher algorithm is not available in the current security environment
   * @throws NoSuchPaddingException    if the padding scheme is not available
   * @throws InvalidKeyException       if the key is not valid for the cipher
   * @throws IllegalBlockSizeException if the plaintext length is not valid for the cipher's block size
   * @throws BadPaddingException       if padding removal fails during encryption
   */
  public static byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted);
  }

  /**
   * Encrypts plaintext using an explicitly named cipher algorithm, falling back to the key's own algorithm when the name is {@code null}.
   *
   * @param key               the key used to encrypt
   * @param specificAlgorithm the cipher algorithm name to use, or {@code null} to use {@link Key#getAlgorithm()}
   * @param toBeEncrypted     the plaintext bytes to encrypt
   * @return the ciphertext bytes
   * @throws NoSuchAlgorithmException  if the cipher algorithm is not available in the current security environment
   * @throws NoSuchPaddingException    if the padding scheme is not available
   * @throws InvalidKeyException       if the key is not valid for the cipher
   * @throws IllegalBlockSizeException if the plaintext length is not valid for the cipher's block size
   * @throws BadPaddingException       if padding removal fails during encryption
   */
  public static byte[] encrypt (Key key, String specificAlgorithm, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;
    byte[] data;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key);

    data = toBeEncrypted;

    return cipher.doFinal(data);
  }

  /**
   * Encrypts plaintext using algorithm parameters (e.g., an IV for CBC mode) and the key's own algorithm name.
   *
   * @param key                    the key used to encrypt
   * @param toBeEncrypted          the plaintext bytes to encrypt
   * @param algorithmParameterSpec additional parameters such as an initialization vector
   * @return the ciphertext bytes
   * @throws NoSuchAlgorithmException           if the cipher algorithm is not available in the current security environment
   * @throws InvalidAlgorithmParameterException if the algorithm parameters are not valid for the cipher
   * @throws NoSuchPaddingException             if the padding scheme is not available
   * @throws InvalidKeyException                if the key is not valid for the cipher
   * @throws IllegalBlockSizeException          if the plaintext length is not valid for the cipher's block size
   * @throws BadPaddingException                if padding removal fails during encryption
   */
  public static byte[] encrypt (Key key, byte[] toBeEncrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted, algorithmParameterSpec);
  }

  /**
   * Encrypts plaintext using algorithm parameters and an explicitly named cipher, falling back to the key's own algorithm when the name is {@code null}.
   *
   * @param key                    the key used to encrypt
   * @param specificAlgorithm      the cipher algorithm name to use, or {@code null} to use {@link Key#getAlgorithm()}
   * @param toBeEncrypted          the plaintext bytes to encrypt
   * @param algorithmParameterSpec additional parameters such as an initialization vector
   * @return the ciphertext bytes
   * @throws NoSuchAlgorithmException           if the cipher algorithm is not available in the current security environment
   * @throws InvalidAlgorithmParameterException if the algorithm parameters are not valid for the cipher
   * @throws NoSuchPaddingException             if the padding scheme is not available
   * @throws InvalidKeyException                if the key is not valid for the cipher
   * @throws IllegalBlockSizeException          if the plaintext length is not valid for the cipher's block size
   * @throws BadPaddingException                if padding removal fails during encryption
   */
  public static byte[] encrypt (Key key, String specificAlgorithm, byte[] toBeEncrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;
    byte[] data;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key, algorithmParameterSpec);

    data = toBeEncrypted;

    return cipher.doFinal(data);
  }

  /**
   * Decrypts ciphertext using the key's own algorithm name to select the cipher.
   *
   * @param key           the key used to decrypt
   * @param toBeDecrypted the ciphertext bytes to decrypt
   * @return the decrypted plaintext bytes
   * @throws NoSuchAlgorithmException  if the cipher algorithm is not available in the current security environment
   * @throws NoSuchPaddingException    if the padding scheme is not available
   * @throws InvalidKeyException       if the key is not valid for the cipher
   * @throws IllegalBlockSizeException if the ciphertext length is not valid for the cipher's block size
   * @throws BadPaddingException       if padding removal fails during decryption
   */
  public static byte[] decrypt (Key key, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted);
  }

  /**
   * Decrypts ciphertext using an explicitly named cipher algorithm, falling back to the key's own algorithm when the name is {@code null}.
   *
   * @param key               the key used to decrypt
   * @param specificAlgorithm the cipher algorithm name to use, or {@code null} to use {@link Key#getAlgorithm()}
   * @param toBeDecrypted     the ciphertext bytes to decrypt
   * @return the decrypted plaintext bytes
   * @throws NoSuchAlgorithmException  if the cipher algorithm is not available in the current security environment
   * @throws NoSuchPaddingException    if the padding scheme is not available
   * @throws InvalidKeyException       if the key is not valid for the cipher
   * @throws IllegalBlockSizeException if the ciphertext length is not valid for the cipher's block size
   * @throws BadPaddingException       if padding removal fails during decryption
   */
  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key);

    return cipher.doFinal(toBeDecrypted);
  }

  /**
   * Decrypts ciphertext using algorithm parameters (e.g., an IV for CBC mode) and the key's own algorithm name.
   *
   * @param key                    the key used to decrypt
   * @param toBeDecrypted          the ciphertext bytes to decrypt
   * @param algorithmParameterSpec additional parameters such as an initialization vector
   * @return the decrypted plaintext bytes
   * @throws NoSuchAlgorithmException           if the cipher algorithm is not available in the current security environment
   * @throws InvalidAlgorithmParameterException if the algorithm parameters are not valid for the cipher
   * @throws NoSuchPaddingException             if the padding scheme is not available
   * @throws InvalidKeyException                if the key is not valid for the cipher
   * @throws IllegalBlockSizeException          if the ciphertext length is not valid for the cipher's block size
   * @throws BadPaddingException                if padding removal fails during decryption
   */
  public static byte[] decrypt (Key key, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted, algorithmParameterSpec);
  }

  /**
   * Decrypts ciphertext using algorithm parameters and an explicitly named cipher, falling back to the key's own algorithm when the name is {@code null}.
   *
   * @param key                    the key used to decrypt
   * @param specificAlgorithm      the cipher algorithm name to use, or {@code null} to use {@link Key#getAlgorithm()}
   * @param toBeDecrypted          the ciphertext bytes to decrypt
   * @param algorithmParameterSpec additional parameters such as an initialization vector
   * @return the decrypted plaintext bytes
   * @throws NoSuchAlgorithmException           if the cipher algorithm is not available in the current security environment
   * @throws InvalidAlgorithmParameterException if the algorithm parameters are not valid for the cipher
   * @throws NoSuchPaddingException             if the padding scheme is not available
   * @throws InvalidKeyException                if the key is not valid for the cipher
   * @throws IllegalBlockSizeException          if the ciphertext length is not valid for the cipher's block size
   * @throws BadPaddingException                if padding removal fails during decryption
   */
  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameterSpec);

    return cipher.doFinal(toBeDecrypted);
  }
}
