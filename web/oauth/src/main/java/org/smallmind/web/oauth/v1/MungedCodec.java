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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import org.smallmind.nutsnbolts.security.EncryptionUtilities;
import org.smallmind.web.jersey.util.JsonCodec;

/*
Public Key
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbk6FT5T1aubaPHpvpcgRGqAOWYIOMMzgxI96rzIr/SxZ0c2hhjvVd5JoEy5n4wzEuXWQpgCDsgSWmO92Nx6UWzLOnbGnIkffPbX4sHg45MWxamDdz4Q6XY8vojitMbIrumG+RjnnTR+YSXG/12Eb5TBvlNTdq31AM8eeMRPMjfQIDAQAB
*/
public class MungedCodec {

  private static final PrivateKey PRIVATE_KEY;
  private static final PublicKey PUBLIC_KEY;

  static {

    InputStream inputStream;

    if ((inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/web/oauth/oauth_rsa")) == null) {
      throw new StaticInitializationError("Missing private key resource");
    }

    try {

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      int bytesRead;
      byte[] buffer = new byte[1024];

      while ((bytesRead = inputStream.read(buffer)) >= 0) {
        byteArrayOutputStream.write(buffer, 0, bytesRead);
      }
      byteArrayOutputStream.close();
      inputStream.close();

      PRIVATE_KEY = (PrivateKey) EncryptionUtilities.deserializeKey(byteArrayOutputStream.toByteArray());
      PUBLIC_KEY = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64Codec.decode("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCbk6FT5T1aubaPHpvpcgRGqAOWYIOMMzgxI96rzIr/SxZ0c2hhjvVd5JoEy5n4wzEuXWQpgCDsgSWmO92Nx6UWzLOnbGnIkffPbX4sHg45MWxamDdz4Q6XY8vojitMbIrumG+RjnnTR+YSXG/12Eb5TBvlNTdq31AM8eeMRPMjfQIDAQAB")));
    } catch (Exception exception) {
      throw new StaticInitializationError(exception);
    }
  }

  public static String toJsonString(Object obj)
      throws JsonProcessingException {

    return JsonCodec.writeAsString(obj);
  }

  public static String encrypt(Object obj, boolean doubleMunged)
      throws Exception {

    Key aesKey;
    byte[] aesKeyBytes = new byte[16];

    ThreadLocalRandom.current().nextBytes(aesKeyBytes);
    aesKey = new SecretKeySpec(aesKeyBytes, "AES");

    return Base64Codec.encode(EncryptionUtilities.encrypt(PUBLIC_KEY, (doubleMunged) ? Base64Codec.encode(aesKeyBytes).getBytes() : aesKeyBytes)) + '.' + Base64Codec.encode(Encryption.AES.encrypt(aesKey, JsonCodec.writeAsBytes(obj)));
  }

  public static <T> T decrypt(Class<T> clazz, String toBeDecrypted, boolean aesDoubleMunged)
      throws Exception {

    Key aesKey;
    byte[] aesKeyBytes;
    int dotPos;

    if ((dotPos = toBeDecrypted.indexOf('.')) < 0) {
      throw new IllegalArgumentException("Encrypted value is in an unknown format (expecting munged key.encrypted)");
    }

    aesKeyBytes = EncryptionUtilities.decrypt(PRIVATE_KEY, Base64Codec.decode(toBeDecrypted.substring(0, dotPos)));
    if (aesDoubleMunged) {
      aesKeyBytes = Base64Codec.decode(aesKeyBytes);
    }

    aesKey = new SecretKeySpec(aesKeyBytes, "AES");

    return JsonCodec.read(Encryption.AES.decrypt(aesKey, Base64Codec.decode(toBeDecrypted.substring(dotPos + 1))), clazz);
  }
}
