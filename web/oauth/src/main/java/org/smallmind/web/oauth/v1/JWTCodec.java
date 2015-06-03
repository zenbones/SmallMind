/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.web.oauth.v1;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;
import org.smallmind.nutsnbolts.security.EncryptionUtility;
import org.smallmind.nutsnbolts.security.MessageAuthenticationCodeAlgorithm;
import org.smallmind.web.jersey.util.JsonCodec;

public class JWTCodec {

  private static final String ENCODED_HEADER;

  static {

    try {
      ENCODED_HEADER = Base64Codec.encode("{\"typ\":\"JWT\",\r\n \"alg\":\"HS256\"}");
    } catch (IOException ioException) {
      throw new StaticInitializationError(ioException);
    }
  }

  public static String encode (Object claims, String key)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException {

    String encodedClaims = Base64Codec.encode(JsonCodec.writeAsBytes(claims));
    String prologue = ENCODED_HEADER + '.' + encodedClaims;
    String epilogue = Base64Codec.encode(EncryptionUtility.encrypt(MessageAuthenticationCodeAlgorithm.HMAC_SHA_256, key.getBytes(), prologue.getBytes()));

    return prologue + '.' + epilogue;
  }

  public static <T> T decode (String jwtToken, String key, Class<T> claimsClass)
    throws IOException, NoSuchAlgorithmException, InvalidKeyException {

    String[] parts;

    if ((parts = jwtToken.split("\\.", -1)).length != 3) {
      throw new UnsupportedEncodingException("Not a JWT token");
    }
    if (!parts[2].equals(Base64Codec.encode(EncryptionUtility.encrypt(MessageAuthenticationCodeAlgorithm.HMAC_SHA_256, key.getBytes(), (parts[0] + '.' + parts[1]).getBytes())))) {
      throw new UnsupportedEncodingException("Not a JWT token");
    }

    return JsonCodec.read(Base64Codec.decode(parts[1]), claimsClass);
  }
}
