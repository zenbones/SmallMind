/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.smallmind.web.jwt.jose4j.JWTConsumer;

/**
 * Utility methods for encoding and decoding JWTs using provided keys.
 */
public class JWTCodec {

  /**
   * Encodes claims into a compact JWT using the provided key master.
   *
   * @param claims the claim object to serialize as the JWT payload
   * @param keyMaster the signing key provider
   * @return the signed JWT string
   * @throws Exception if serialization or signing fails
   */
  public static String encode (Object claims, JWTKeyMaster keyMaster)
    throws Exception {

    return encode(claims, keyMaster, null);
  }

  /**
   * Encodes claims into a compact JWT using the provided key master and an optional key id.
   *
   * @param claims the claim object to serialize as the JWT payload
   * @param keyMaster the signing key provider
   * @param keyId an optional key identifier to embed in the JWT header
   * @return the signed JWT string
   * @throws Exception if serialization or signing fails
   */
  public static String encode (Object claims, JWTKeyMaster keyMaster, String keyId)
    throws Exception {

    JsonWebSignature jws = new JsonWebSignature();

    jws.setPayloadBytes(JsonCodec.writeAsBytes(claims));
    jws.setHeader(HeaderParameterNames.TYPE, "JWT");
    jws.setKey(keyMaster.getKey());
    jws.setAlgorithmHeaderValue(keyMaster.getEncryptionAlgorithm().getCode());

    if (keyId != null) {
      jws.setKeyIdHeaderValue(keyId);
    }

    return jws.getCompactSerialization();
  }

  /**
   * Decodes and verifies a JWT using the supplied key master.
   *
   * @param jwtToken the compact JWT value
   * @param keyMaster the verification key provider
   * @param claimsClass the target class for the payload
   * @param <T> the claim type
   * @return the deserialized claim object
   * @throws Exception if verification or deserialization fails
   */
  public static <T> T decode (String jwtToken, JWTKeyMaster keyMaster, Class<T> claimsClass)
    throws Exception {

    return new JWTConsumer().process(jwtToken, keyMaster.getKey(), claimsClass);
  }

  /**
   * Deciphers a JWT without verifying its signature.
   *
   * @param jwtToken the compact JWT value
   * @param claimsClass the target class for the payload
   * @param <T> the claim type
   * @return the deserialized claim object
   * @throws Exception if parsing or deserialization fails
   */
  public static <T> T decipher (String jwtToken, Class<T> claimsClass)
    throws Exception {

    return new JWTConsumer().setSkipSignatureVerification(true).process(jwtToken, null, claimsClass);
  }
}
