/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public enum Encryption {

  AES {

    private final byte[] iv = new byte[] {0x47, 0x6C, 0x75, 0x20, 0x4D, 0x6F, 0x62, 0x69, 0x6C, 0x65, 0x20, 0x47, 0x61, 0x6D, 0x65, 0x73};

    @Override
    public byte[] encrypt (Key key, byte[] toBeEncrypted)
      throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

      return EncryptionUtility.encrypt(key, "AES/CBC/PKCS5Padding", toBeEncrypted, new IvParameterSpec(iv));
    }

    @Override
    public byte[] decrypt (Key key, byte[] encrypted)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

      return EncryptionUtility.decrypt(key, "AES/CBC/PKCS5Padding", encrypted, new IvParameterSpec(iv));
    }
  };

  public abstract byte[] encrypt (Key key, byte[] toBeEncrypted)
    throws Exception;

  public abstract byte[] decrypt (Key key, byte[] encrypted)
    throws Exception;
}
