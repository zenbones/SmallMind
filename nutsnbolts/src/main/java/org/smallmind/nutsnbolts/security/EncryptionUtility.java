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
 * Convenience methods for hashing, signing, encryption, and key serialization.
 */
public class EncryptionUtility {

  /**
   * Computes a digest for the supplied bytes.
   *
   * @param algorithm  the hash algorithm
   * @param toBeHashed the data to hash
   * @return the digest
   * @throws NoSuchAlgorithmException if the digest algorithm is unavailable
   */
  public static byte[] hash (HashAlgorithm algorithm, byte[] toBeHashed)
    throws NoSuchAlgorithmException {

    return MessageDigest.getInstance(algorithm.getAlgorithmName()).digest(toBeHashed);
  }

  /**
   * Produces a MAC using a symmetric signing algorithm.
   *
   * @param algorithm the MAC algorithm
   * @param secretKey the key to use
   * @param data      the data to sign
   * @return the MAC bytes
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid
   */
  public static byte[] sign (SymmetricSigningAlgorithm algorithm, Key secretKey, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException {

    Mac mac = Mac.getInstance(algorithm.getAlgorithmName());

    mac.init(secretKey);

    return mac.doFinal(data);
  }

  /**
   * Verifies a MAC produced by {@link #sign(SymmetricSigningAlgorithm, Key, byte[])}.
   *
   * @param algorithm  the MAC algorithm
   * @param secretKey  the key to use
   * @param data       the original data
   * @param signedData the expected MAC
   * @return {@code true} if the MACs match
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid
   */
  public static boolean verify (SymmetricSigningAlgorithm algorithm, Key secretKey, byte[] data, byte[] signedData)
    throws NoSuchAlgorithmException, InvalidKeyException {

    Mac mac = Mac.getInstance(algorithm.getAlgorithmName());

    mac.init(secretKey);

    return Arrays.equals(mac.doFinal(data), signedData);
  }

  /**
   * Signs data using an asymmetric algorithm.
   *
   * @param algorithm  the signature algorithm
   * @param privateKey the private key
   * @param data       data to sign
   * @return the signature
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid
   * @throws SignatureException       if signing fails
   */
  public static byte[] sign (AsymmetricSigningAlgorithm algorithm, PrivateKey privateKey, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName());

    signature.initSign(privateKey);
    signature.update(data);

    return signature.sign();
  }

  /**
   * Verifies a signature produced by {@link #sign(AsymmetricSigningAlgorithm, PrivateKey, byte[])}.
   *
   * @param algorithm  the signature algorithm
   * @param publicKey  the public key
   * @param data       original data
   * @param signedData the signature to verify
   * @return {@code true} if verification succeeds
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   * @throws InvalidKeyException      if the key is invalid
   * @throws SignatureException       if verification fails
   */
  public static boolean verify (AsymmetricSigningAlgorithm algorithm, PublicKey publicKey, byte[] data, byte[] signedData)
    throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName());

    signature.initVerify(publicKey);
    signature.update(data);

    return signature.verify(signedData);
  }

  /**
   * Generates a symmetric key for the given algorithm.
   *
   * @param algorithm the symmetric algorithm
   * @return a newly generated key
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   */
  public static Key generateKey (SymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyGenerator keyGenerator;

    keyGenerator = KeyGenerator.getInstance(algorithm.name());
    return keyGenerator.generateKey();
  }

  /**
   * Generates an asymmetric key pair for the given algorithm.
   *
   * @param algorithm the asymmetric algorithm
   * @return a newly generated key pair
   * @throws NoSuchAlgorithmException if the algorithm is unavailable
   */
  public static KeyPair generateKeyPair (AsymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyPairGenerator keyPairGenerator;

    keyPairGenerator = KeyPairGenerator.getInstance(algorithm.name());
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Serializes a key using {@link java.security.KeyRep} for later reconstruction.
   *
   * @param key the key to serialize
   * @return the serialized bytes
   * @throws IOException if serialization fails
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
   * Reconstructs a key from the bytes produced by {@link #serializeKey(Key)}.
   *
   * @param keyBytes serialized key bytes
   * @return the deserialized key
   * @throws IOException            if deserialization fails
   * @throws ClassNotFoundException if the key type cannot be resolved
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
   * Encrypts data with the supplied key using the key's algorithm.
   *
   * @param key           the key to use
   * @param toBeEncrypted the plaintext
   * @return ciphertext
   * @throws NoSuchAlgorithmException  if the algorithm is unavailable
   * @throws NoSuchPaddingException    if the padding scheme is unavailable
   * @throws InvalidKeyException       if the key is invalid
   * @throws IllegalBlockSizeException if the data size is invalid for the cipher
   * @throws BadPaddingException       if padding is incorrect
   */
  public static byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted);
  }

  /**
   * Encrypts data with a specific algorithm override.
   *
   * @param key               the key to use
   * @param specificAlgorithm optional algorithm name; defaults to the key's algorithm when {@code null}
   * @param toBeEncrypted     the plaintext
   * @return ciphertext
   * @throws NoSuchAlgorithmException  if the algorithm is unavailable
   * @throws NoSuchPaddingException    if the padding scheme is unavailable
   * @throws InvalidKeyException       if the key is invalid
   * @throws IllegalBlockSizeException if the data size is invalid for the cipher
   * @throws BadPaddingException       if padding is incorrect
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
   * Encrypts data with algorithm parameters (e.g., IV).
   *
   * @param key                    the key to use
   * @param toBeEncrypted          the plaintext
   * @param algorithmParameterSpec additional algorithm parameters
   * @return ciphertext
   * @throws NoSuchAlgorithmException           if the algorithm is unavailable
   * @throws InvalidAlgorithmParameterException if the parameters are invalid
   * @throws NoSuchPaddingException             if the padding scheme is unavailable
   * @throws InvalidKeyException                if the key is invalid
   * @throws IllegalBlockSizeException          if the data size is invalid for the cipher
   * @throws BadPaddingException                if padding is incorrect
   */
  public static byte[] encrypt (Key key, byte[] toBeEncrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted, algorithmParameterSpec);
  }

  /**
   * Encrypts data with algorithm parameters and an explicit cipher name.
   *
   * @param key                    the key to use
   * @param specificAlgorithm      optional algorithm name; defaults to the key's algorithm when {@code null}
   * @param toBeEncrypted          the plaintext
   * @param algorithmParameterSpec additional algorithm parameters
   * @return ciphertext
   * @throws NoSuchAlgorithmException           if the algorithm is unavailable
   * @throws InvalidAlgorithmParameterException if the parameters are invalid
   * @throws NoSuchPaddingException             if the padding scheme is unavailable
   * @throws InvalidKeyException                if the key is invalid
   * @throws IllegalBlockSizeException          if the data size is invalid for the cipher
   * @throws BadPaddingException                if padding is incorrect
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
   * Decrypts data using the key's algorithm.
   *
   * @param key           the key to use
   * @param toBeDecrypted the ciphertext
   * @return plaintext
   * @throws NoSuchAlgorithmException  if the algorithm is unavailable
   * @throws NoSuchPaddingException    if the padding scheme is unavailable
   * @throws InvalidKeyException       if the key is invalid
   * @throws IllegalBlockSizeException if the data size is invalid for the cipher
   * @throws BadPaddingException       if padding is incorrect
   */
  public static byte[] decrypt (Key key, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted);
  }

  /**
   * Decrypts data using an explicit algorithm name.
   *
   * @param key               the key to use
   * @param specificAlgorithm optional algorithm name; defaults to the key's algorithm when {@code null}
   * @param toBeDecrypted     the ciphertext
   * @return plaintext
   * @throws NoSuchAlgorithmException  if the algorithm is unavailable
   * @throws NoSuchPaddingException    if the padding scheme is unavailable
   * @throws InvalidKeyException       if the key is invalid
   * @throws IllegalBlockSizeException if the data size is invalid for the cipher
   * @throws BadPaddingException       if padding is incorrect
   */
  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key);

    return cipher.doFinal(toBeDecrypted);
  }

  /**
   * Decrypts data using algorithm parameters (e.g., IV).
   *
   * @param key                    the key to use
   * @param toBeDecrypted          the ciphertext
   * @param algorithmParameterSpec additional algorithm parameters
   * @return plaintext
   * @throws NoSuchAlgorithmException           if the algorithm is unavailable
   * @throws InvalidAlgorithmParameterException if the parameters are invalid
   * @throws NoSuchPaddingException             if the padding scheme is unavailable
   * @throws InvalidKeyException                if the key is invalid
   * @throws IllegalBlockSizeException          if the data size is invalid for the cipher
   * @throws BadPaddingException                if padding is incorrect
   */
  public static byte[] decrypt (Key key, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted, algorithmParameterSpec);
  }

  /**
   * Decrypts data using algorithm parameters and an explicit cipher name.
   *
   * @param key                    the key to use
   * @param specificAlgorithm      optional algorithm name; defaults to the key's algorithm when {@code null}
   * @param toBeDecrypted          the ciphertext
   * @param algorithmParameterSpec additional algorithm parameters
   * @return plaintext
   * @throws NoSuchAlgorithmException           if the algorithm is unavailable
   * @throws InvalidAlgorithmParameterException if the parameters are invalid
   * @throws NoSuchPaddingException             if the padding scheme is unavailable
   * @throws InvalidKeyException                if the key is invalid
   * @throws IllegalBlockSizeException          if the data size is invalid for the cipher
   * @throws BadPaddingException                if padding is incorrect
   */
  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameterSpec);

    return cipher.doFinal(toBeDecrypted);
  }
}
