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
package org.smallmind.ansible;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

public class VaultCodec {

  public static String encrypt (InputStream inputStream, String password)
    throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    return encrypt(inputStream, password, null);
  }

  public static String encrypt (InputStream inputStream, String password, String id)
    throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

    StringBuilder encryptedBuilder = new StringBuilder();
    VaultCake vaultCake;

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        byteOutputStream.write(singleByte);
      }

      vaultCake = new VaultTumbler(password).encrypt(byteOutputStream.toByteArray());
    }

    encryptedBuilder.append(EncryptionUtility.hexEncode(EncryptionUtility.hexEncode(vaultCake.getSalt()).getBytes()));
    encryptedBuilder.append("0a");
    encryptedBuilder.append(EncryptionUtility.hexEncode(EncryptionUtility.hexEncode(vaultCake.getHmac()).getBytes()));
    encryptedBuilder.append("0a");
    encryptedBuilder.append(EncryptionUtility.hexEncode(EncryptionUtility.hexEncode(vaultCake.getEncrypted()).getBytes()));

    for (int index = (encryptedBuilder.length() / 80) * 80; index > 0; index -= 80) {
      encryptedBuilder.insert(index, '\n');
    }

    if (id != null) {
      encryptedBuilder.insert(0, '\n').insert(0, id).insert(0, "1.2;AES256;");
    } else {
      encryptedBuilder.insert(0, "1.1;AES256\n");
    }
    encryptedBuilder.insert(0, "$ANSIBLE_VAULT;");

    return encryptedBuilder.toString();
  }

  public static byte[] decrypt (InputStream inputStream, String password)
    throws IOException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, VaultCodecException {

    String header = readLine(inputStream);
    String[] headerParts = header.split(";", -1);

    if ((headerParts.length >= 3) && "$ANSIBLE_VAULT".equals(headerParts[0]) && (("1.1".equals(headerParts[1]) && (headerParts.length == 3)) || ("1.2".equals(headerParts[1]) && (headerParts.length == 4)))) {
      if (!"AES256".equals(headerParts[2])) {
        throw new VaultCodecException("Unknown cypher(%s)", headerParts[2]);
      } else {

        return new VaultTumbler(password, readBytes(inputStream, 32)).decrypt(readBytes(inputStream, 32), readBytes(inputStream));
      }
    } else {
      throw new VaultCodecException("Unknown header format(%s)", header);
    }
  }

  private static void skip0A (InputStream inputStream)
    throws IOException, VaultCodecException {

    if ((inputStream.read() != '0') || (inputStream.read() != 'a')) {
      throw new VaultCodecException("Expected line terminator(0a)");
    }
  }

  private static byte[] readBytes (InputStream inputStream, int length)
    throws IOException, VaultCodecException {

    int quadrupleLength = length * 4;
    int bytesRead = 0;
    byte[] buffer = new byte[quadrupleLength];

    int singleByte;

    while ((singleByte = inputStream.read()) >= 0) {
      if (singleByte != '\n') {
        buffer[bytesRead++] = (byte)singleByte;
        if (bytesRead == quadrupleLength) {
          break;
        }
      }
    }

    if (bytesRead < quadrupleLength) {
      throw new VaultCodecException("Unable to read required bytes(%d)", length);
    } else {
      skip0A(inputStream);

      return EncryptionUtility.hexDecode(EncryptionUtility.hexDecode(buffer));
    }
  }

  private static byte[] readBytes (InputStream inputStream)
    throws IOException {

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        if (singleByte != '\n') {
          byteOutputStream.write(singleByte);
        }
      }

      return EncryptionUtility.hexDecode(EncryptionUtility.hexDecode(byteOutputStream.toByteArray()));
    }
  }

  private static String readLine (InputStream inputStream)
    throws IOException {

    try (ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

      int singleByte;

      while ((singleByte = inputStream.read()) >= 0) {
        if (singleByte == '\n') {

          return byteOutputStream.toString();
        } else {
          byteOutputStream.write(singleByte);
        }
      }

      return byteOutputStream.toString();
    }
  }
}
