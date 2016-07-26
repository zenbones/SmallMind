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
package org.smallmind.nutsnbolts.security.ssh;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import org.smallmind.nutsnbolts.http.Base64Codec;

public class X509KeyReader implements SSHKeyReader {

  @Override
  public SSHKeyFactors extractFactors (String raw)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, SSHParseException {

    StringBuilder stripedRawBuilder = new StringBuilder();

    for (int index = 0; index < raw.length(); index++) {

      char currentChar = raw.charAt(index);

      if ((currentChar != ' ') && (currentChar != '\n')) {
        stripedRawBuilder.append(currentChar);
      }
    }

    System.out.println(Arrays.toString(Base64Codec.decode(stripedRawBuilder.toString())));
    try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(Base64Codec.decode(stripedRawBuilder.toString())))) {

      if (dataInputStream.read() != 48) {
        throw new SSHParseException("Missing id_rsa SEQUENCE");
      }
      if (dataInputStream.read() != 130) {
        throw new SSHParseException("Missing Version marker");
      }

      dataInputStream.skipBytes(5);

      BigInteger modulus;

      try {
        modulus = readAsBigInteger(dataInputStream);
        readAsBigInteger(dataInputStream);
      } catch (SSHParseException sshParseException) {
        dataInputStream.skipBytes(20);
        modulus = readAsBigInteger(dataInputStream);
      }

      BigInteger exponent = readAsBigInteger(dataInputStream);

      return new SSHKeyFactors(modulus, exponent);
    }
  }

  private BigInteger readAsBigInteger (DataInputStream dataInputStream)
    throws IOException, SSHParseException {

    if (dataInputStream.read() != 2) {
      throw new SSHParseException("Missing INTEGER marker");
    }

    int length = dataInputStream.read();

    if (length >= 0x80) {

      byte[] extended = new byte[4];
      int bytesToRead = length & 0x7f;

      if (bytesToRead == 0) {
        bytesToRead = 4;
      }
      dataInputStream.readFully(extended, 4 - bytesToRead, bytesToRead);
      length = new BigInteger(extended).intValue();
    }

    byte[] data = new byte[length];

    dataInputStream.readFully(data);

    return new BigInteger(data);
  }
}
