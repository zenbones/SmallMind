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
 * Static utility class for encoding objects as signed JWTs and decoding or deciphering JWT strings back to typed claim objects.
 */
public class JWTCodec {

  /**
   * Serializes the given claims object and signs it as a compact JWT without embedding a key id.
   *
   * @param claims    the object to serialize as the JWT payload
   * @param keyMaster the provider of the signing key and algorithm
   * @return the signed compact JWT string
   * @throws Exception if JSON serialization or JWT signing fails
   */
  public static String encode (Object claims, JWTKeyMaster keyMaster)
    throws Exception {

    return encode(claims, keyMaster, null);
  }

  /**
   * Serializes the given claims object, signs it as a compact JWT, and optionally embeds a key identifier in the header.
   *
   * @param claims    the object to serialize as the JWT payload
   * @param keyMaster the provider of the signing key and algorithm
   * @param keyId     optional key identifier written to the JWT {@code kid} header; {@code null} omits the header
   * @return the signed compact JWT string
   * @throws Exception if JSON serialization or JWT signing fails
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
   * Verifies the signature of a compact JWT and deserializes its payload into the specified type.
   *
   * @param jwtToken    the compact JWT string to decode
   * @param keyMaster   the provider of the verification key
   * @param claimsClass the target type for the deserialized payload
   * @param <T>         the claims type
   * @return the deserialized claims object
   * @throws Exception if signature verification or deserialization fails
   */
  public static <T> T decode (String jwtToken, JWTKeyMaster keyMaster, Class<T> claimsClass)
    throws Exception {

    return new JWTConsumer().process(jwtToken, keyMaster.getKey(), claimsClass);
  }

  /**
   * Deserializes the payload of a compact JWT without verifying its signature.
   *
   * @param jwtToken    the compact JWT string to parse
   * @param claimsClass the target type for the deserialized payload
   * @param <T>         the claims type
   * @return the deserialized claims object
   * @throws Exception if JWT parsing or deserialization fails
   */
  public static <T> T decipher (String jwtToken, Class<T> claimsClass)
    throws Exception {

    return new JWTConsumer().setSkipSignatureVerification(true).process(jwtToken, null, claimsClass);
  }
}
