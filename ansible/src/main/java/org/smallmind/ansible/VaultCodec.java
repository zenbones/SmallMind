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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.smallmind.nutsnbolts.security.EncryptionUtility;

public class VaultCodec {

  public static byte[] decrypt (InputStream inputStream, String password)
    throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, VaultException {

    String header = readLine(inputStream);
    String[] headerParts = header.split(";", -1);

    if ((headerParts.length >= 3) && "$ANSIBLE_VAULT".equals(headerParts[0]) && (("1.1".equals(headerParts[1]) && (headerParts.length == 3)) || ("1.2".equals(headerParts[1]) && (headerParts.length == 4)))) {
      if (!"AES256".equals(headerParts[2])) {
        throw new VaultException("Unknown cypher(%s)", headerParts[2]);
      } else {

        byte[] salt = readBytes(inputStream, 32);
        byte[] hmac = readBytes(inputStream, 32);
        byte[] encrypted = readBytes(inputStream);

        SecretKeyFactory pbkdf2KeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey pbkdf2Key = pbkdf2KeyFactory.generateSecret(new PBEKeySpec(password.toCharArray(), salt, 10000, 640));
        byte[] pbkdf2KeyBytes = pbkdf2Key.getEncoded();
        byte[] hmacKeyBytes = new byte[32];

        System.arraycopy(pbkdf2KeyBytes, 32, hmacKeyBytes,0, 32);

        SecretKeySpec hmacKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(hmacKey);
        byte[] test = mac.doFinal(encrypted);

        System.out.println(test);
        return null;
      }
    } else {
      throw new VaultException("Unknown header format(%s)", header);
    }
  }

  private static void skip0A (InputStream inputStream)
    throws IOException, VaultException {

    if ((inputStream.read() != '0') || (inputStream.read() != 'a')) {
      throw new VaultException("Expected line terminator(0a)");
    }
  }

  private static byte[] readBytes (InputStream inputStream, int length)
    throws IOException, VaultException {

    int quadrupleLength = length * 4;
    int bytesRead = 0;
    byte[] buffer = new byte[quadrupleLength];

    int singleChar;

    while ((singleChar = inputStream.read()) >= 0) {
      if (singleChar != '\n') {
        buffer[bytesRead++] = (byte)singleChar;
        if (bytesRead == quadrupleLength) {
          break;
        }
      }
    }

    if (bytesRead < quadrupleLength) {
      throw new VaultException("Unable to read required bytes(%d)", length);
    } else {
      skip0A(inputStream);

      return EncryptionUtility.hexDecode(new String(EncryptionUtility.hexDecode(new String(buffer))));
    }
  }

  private static byte[] readBytes (InputStream inputStream)
    throws IOException {

    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();

    int singleChar;

    while ((singleChar = inputStream.read()) >= 0) {
      if (singleChar != '\n') {
        byteOutputStream.write(singleChar);
      }
    }

    return EncryptionUtility.hexDecode(new String(EncryptionUtility.hexDecode(byteOutputStream.toString())));
  }

  private static String readLine (InputStream inputStream)
    throws IOException {

    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    int singleChar;

    while ((singleChar = inputStream.read()) >= 0) {
      if (singleChar == '\n') {

        return byteOutputStream.toString();
      } else {
        byteOutputStream.write(singleChar);
      }
    }

    return byteOutputStream.toString();
  }
}
