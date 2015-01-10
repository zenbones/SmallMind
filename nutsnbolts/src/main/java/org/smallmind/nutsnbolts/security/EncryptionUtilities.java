/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyRep;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class EncryptionUtilities {

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
    }
    finally {
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
    }
    finally {
      keyInputStream.close();
    }
  }

  public static byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;
    byte[] data;

    cipher = Cipher.getInstance(key.getAlgorithm());
    cipher.init(Cipher.ENCRYPT_MODE, key);

    data = toBeEncrypted;

    return cipher.doFinal(data);
  }

  public static byte[] decrypt (Key key, byte[] toBeDecrypted)
    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    Cipher cipher;

    cipher = Cipher.getInstance(key.getAlgorithm());
    cipher.init(Cipher.DECRYPT_MODE, key);

    return cipher.doFinal(toBeDecrypted);
  }
}


