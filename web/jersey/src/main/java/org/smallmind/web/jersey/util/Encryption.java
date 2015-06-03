package org.smallmind.web.jersey.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

public enum Encryption {

  AES {

    private final byte[] iv = new byte[]{0x47, 0x6C, 0x75, 0x20, 0x4D, 0x6F, 0x62, 0x69, 0x6C, 0x65, 0x20, 0x47, 0x61, 0x6D, 0x65, 0x73};

    @Override
    public byte[] encrypt (Key key, byte[] toBeEncrypted)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

      Cipher cipher;

      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

      return cipher.doFinal(toBeEncrypted);
    }

    @Override
    public byte[] decrypt (Key key, byte[] encrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

      Cipher cipher;

      cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

      return cipher.doFinal(encrypted);
    }
  };

  public abstract byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws Exception;

  public abstract byte[] decrypt (Key key, byte[] encrypted)
    throws Exception;
}
