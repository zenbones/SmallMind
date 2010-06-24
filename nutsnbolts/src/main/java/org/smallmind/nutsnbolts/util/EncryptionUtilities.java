package org.smallmind.nutsnbolts.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyRep;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

public class EncryptionUtilities {

   public static final HashMap<HashAlgorithm, MessageDigest> DIGEST_MAP = new HashMap<HashAlgorithm, MessageDigest>();

   public static enum SecurityAlgorithm {

      AES, DES
   }

   public static enum HashAlgorithm {

      MD5
   }

   public static String hexEncode (byte[] bytes) {

      StringBuilder encodingBuilder;

      encodingBuilder = new StringBuilder();
      for (byte b : bytes) {
         if ((b < 0x10) && (b >= 0)) {
            encodingBuilder.append('0');
         }
         encodingBuilder.append(Integer.toHexString(b & 0xff));
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

   public static byte[] hash (HashAlgorithm algorithm, String toBeHashed)
      throws NoSuchAlgorithmException {

      MessageDigest messageDigest;

      synchronized (DIGEST_MAP) {
         if ((messageDigest = DIGEST_MAP.get(algorithm)) == null) {
            messageDigest = MessageDigest.getInstance(algorithm.name());
            DIGEST_MAP.put(algorithm, messageDigest);
         }
      }

      synchronized (messageDigest) {
         return messageDigest.digest(toBeHashed.getBytes());
      }
   }

   public static Key generateKey (SecurityAlgorithm algorithm)
      throws NoSuchAlgorithmException {

      KeyGenerator keyGenerator;

      keyGenerator = KeyGenerator.getInstance(algorithm.name());
      return keyGenerator.generateKey();
   }

   public static byte[] serializeKey (Key key)
      throws IOException {

      KeyRep keyRep;
      ObjectOutputStream keyOutputStream;
      ByteArrayOutputStream byteOutputStream;

      keyRep = new KeyRep(KeyRep.Type.SECRET, key.getAlgorithm(), key.getFormat(), key.getEncoded());
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

   public static byte[] encrypt (Key key, String toBeEncrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

      Cipher cipher;
      byte[] data;

      cipher = Cipher.getInstance(key.getAlgorithm());
      cipher.init(Cipher.ENCRYPT_MODE, key);

      data = toBeEncrypted.getBytes();

      return cipher.doFinal(data);
   }

   public static String decrypt (Key key, byte[] toBeDecrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

      Cipher cipher;

      cipher = Cipher.getInstance(key.getAlgorithm());
      cipher.init(Cipher.DECRYPT_MODE, key);

      return new String(cipher.doFinal(toBeDecrypted));
   }
}


