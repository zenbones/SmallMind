/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.web.oauth.v1;

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

    // {0xE8, 0x0D, 0x9A, 0x29, 0x93, 0x8E, 0x2D, 0x1B, 0xF7, 0xA7, 0x19, 0xD5, 0x84, 0x18, 0x6C, 0xB9}
    private final byte[] iv = new byte[] {-24, 13, -102, 41, -109, -114, 45, 27, -9, -89, 25, -43, -124, 24, 108, -71};

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