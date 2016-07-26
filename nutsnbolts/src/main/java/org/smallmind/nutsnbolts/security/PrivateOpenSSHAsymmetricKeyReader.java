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
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import org.smallmind.nutsnbolts.http.Base64Codec;

public class PrivateOpenSSHAsymmetricKeyReader implements AsymmetricKeyReader {

  @Override
  public Key readKey (String raw)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(Base64Codec.decode(raw)))) {

      if (dataInputStream.read() != 48) {
        throw new IOException("Missing id_rsa SEQUENCE");
      }
      if (dataInputStream.read() != 130) {
        throw new IOException("Missing Version marker");
      }

      dataInputStream.skipBytes(5);

      BigInteger mod = readAsBigInteger(dataInputStream);
      readAsBigInteger(dataInputStream);
      BigInteger exp = readAsBigInteger(dataInputStream);

      return KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(mod, exp));
    }
  }

  private BigInteger readAsBigInteger (DataInputStream dataInputStream)
    throws IOException {

    int length;

    dataInputStream.read();
    if ((length = dataInputStream.read()) >= 0x80) {

      byte[] extended = new byte[4];
      int bytesToRead = length & 0x7f;

      dataInputStream.readFully(extended, 4 - bytesToRead, bytesToRead);
      length = new BigInteger(extended).intValue();
    }

    byte[] data = new byte[length];
    dataInputStream.readFully(data);

    return new BigInteger(data);
  }
}
