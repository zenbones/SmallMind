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
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class EncryptionUtility {

  static {

    Security.addProvider(new BouncyCastleProvider());
  }

  public static String hexEncode (byte[] bytes) {

    return hexEncode(bytes, 0, bytes.length);
  }

  public static String hexEncode (byte[] bytes, int offset, int length) {

    StringBuilder encodingBuilder;

    encodingBuilder = new StringBuilder();
    for (int index = offset; index < offset + length; index++) {
      if ((bytes[index] < 0x10) && (bytes[index] >= 0)) {
        encodingBuilder.append('0');
      }
      encodingBuilder.append(Integer.toHexString(bytes[index] & 0xff));
    }

    return encodingBuilder.toString();
  }

  public static byte[] hexDecode (String toBeDecoded) {

    byte[] bytes;

    bytes = new byte[toBeDecoded.length() / 2];
    for (int count = 0; count < toBeDecoded.length(); count += 2) {
      bytes[count / 2] = (byte)Integer.parseInt(toBeDecoded.substring(count, count + 2), 16);
    }

    return bytes;
  }

  public static byte[] hash (HashAlgorithm algorithm, byte[] toBeHashed)
    throws NoSuchAlgorithmException {

    return MessageDigest.getInstance(algorithm.getAlgorithmName()).digest(toBeHashed);
  }

  public static byte[] sign (HMACSigningAlgorithm algorithm, Key secretKey, byte[] data)
    throws NoSuchAlgorithmException, InvalidKeyException {

    Mac mac = Mac.getInstance(algorithm.getAlgorithmName());

    mac.init(secretKey);

    return mac.doFinal(data);
  }

  public static byte[] sign (RSASigningAlgorithm algorithm, PrivateKey privateKey, byte[] data)
    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName(), "BC");

    signature.initSign(privateKey);
    signature.update(data);

    return signature.sign();
  }

  public static boolean verify (RSASigningAlgorithm algorithm, PublicKey publicKey, byte[] data, byte[] signedData)
    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    Signature signature = Signature.getInstance(algorithm.getAlgorithmName(), "BC");

    signature.initVerify(publicKey);
    signature.update(data);

    return signature.verify(signedData);
  }

  public static Key generateKey (SymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyGenerator keyGenerator;

    keyGenerator = KeyGenerator.getInstance(algorithm.name());
    return keyGenerator.generateKey();
  }

  public static KeyPair generateKeyPair (AsymmetricAlgorithm algorithm)
    throws NoSuchAlgorithmException {

    KeyPairGenerator keyPairGenerator;

    keyPairGenerator = KeyPairGenerator.getInstance(algorithm.name());
    return keyPairGenerator.generateKeyPair();
  }

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

  public static byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted);
  }

  public static byte[] encrypt (Key key, String specificAlgorithm, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;
    byte[] data;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key);

    data = toBeEncrypted;

    return cipher.doFinal(data);
  }

  public static byte[] encrypt (Key key, byte[] toBeEncrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(key, null, toBeEncrypted, algorithmParameterSpec);
  }

  public static byte[] encrypt (Key key, String specificAlgorithm, byte[] toBeEncrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;
    byte[] data;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.ENCRYPT_MODE, key, algorithmParameterSpec);

    data = toBeEncrypted;

    return cipher.doFinal(data);
  }

  public static byte[] decrypt (Key key, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted);
  }

  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key);

    return cipher.doFinal(toBeDecrypted);
  }

  public static byte[] decrypt (Key key, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return decrypt(key, null, toBeDecrypted, algorithmParameterSpec);
  }

  public static byte[] decrypt (Key key, String specificAlgorithm, byte[] toBeDecrypted, AlgorithmParameterSpec algorithmParameterSpec)
    throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance((specificAlgorithm == null) ? key.getAlgorithm() : specificAlgorithm);
    cipher.init(Cipher.DECRYPT_MODE, key, algorithmParameterSpec);

    return cipher.doFinal(toBeDecrypted);
  }
}


