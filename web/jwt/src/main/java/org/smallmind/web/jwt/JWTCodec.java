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
package org.smallmind.web.jwt;

import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.web.jersey.util.JsonCodec;

public class JWTCodec {

  public static String encode (Object claims, JWTKeyMaster keyMaster)
    throws Exception {

    return encode(claims, keyMaster, false);
  }

  public static String encode (Object claims, JWTKeyMaster keyMaster, boolean urlSafe)
    throws Exception {

    String header = "{\"typ\":\"JWT\",\r\n \"alg\":\"" + keyMaster.getEncryptionAlgorithm().name() + "\"}";
    String encodedHeader = urlSafe ? Base64Codec.urlSafeEncode(header) : Base64Codec.encode(header);
    String encodedClaims = urlSafe ? Base64Codec.urlSafeEncode(JsonCodec.writeAsBytes(claims)) : Base64Codec.encode(JsonCodec.writeAsBytes(claims));
    String prologue = encodedHeader + '.' + encodedClaims;
    String epilogue;
    byte[] encryptedBytes = keyMaster.getEncryptionAlgorithm().encrypt(keyMaster.getKey(), prologue);

    epilogue = urlSafe ? Base64Codec.urlSafeEncode(encryptedBytes) : Base64Codec.encode(encryptedBytes);

    return prologue + '.' + epilogue;
  }

  public static <T> T decode (String jwtToken, JWTKeyMaster keyMaster, Class<T> claimsClass)
    throws Exception {

    return decode(jwtToken, keyMaster, claimsClass, false);
  }

  public static <T> T decode (String jwtToken, JWTKeyMaster keyMaster, Class<T> claimsClass, boolean urlSafe)
    throws Exception {

    String[] parts;

    if ((parts = jwtToken.split("\\.", -1)).length != 3) {
      throw new UnsupportedEncodingException("Not a JWT token");
    }
    if (!keyMaster.getEncryptionAlgorithm().verify(keyMaster.getKey(), parts)) {
      throw new UnsupportedEncodingException("Not a JWT token");
    }

    return JsonCodec.read(urlSafe ? Base64Codec.urlSfeDecode(parts[1]) : Base64Codec.decode(parts[1]), claimsClass);
  }
}
