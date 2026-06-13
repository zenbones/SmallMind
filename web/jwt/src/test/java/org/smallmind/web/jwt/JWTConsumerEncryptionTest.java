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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.smallmind.web.json.scaffold.util.JsonCodec;
import org.smallmind.web.jwt.jose4j.InvalidJWTException;
import org.smallmind.web.jwt.jose4j.JWTConsumer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the JWE / encryption branches of {@link JWTConsumer} that the signature-only tests leave
 * untouched: the {@code JsonWebEncryption} decryption arm of {@code process}, the nested-JWT loop
 * where a JWE carries a {@code cty=JWT} signed payload, and the {@code processContext} accounting of
 * {@code hasEncryption} / {@code hasSymmetricEncryption} that drives {@code requireEncryption} and
 * {@code requireIntegrity}.
 *
 * <p>Symmetric JWE is built with the {@code A256KW} key-wrap management algorithm (key persuasion
 * SYMMETRIC) and asymmetric JWE with {@code RSA-OAEP-256} (key persuasion ASYMMETRIC); the two cover
 * the true and false sides of the {@code hasSymmetricEncryption} branch.
 */
@Test(groups = "unit")
public class JWTConsumerEncryptionTest {

  private Claims sampleClaims () {

    Claims claims = new Claims();

    claims.setSub("encrypted-subject");

    return claims;
  }

  private SecretKey symmetricKey ()
    throws Exception {

    KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

    keyGenerator.init(256);

    return keyGenerator.generateKey();
  }

  // A JWE whose key-management algorithm is symmetric (AES key wrap).
  private String symmetricEncryptedToken (SecretKey key)
    throws Exception {

    JsonWebEncryption jwe = new JsonWebEncryption();

    jwe.setPayload(new String(JsonCodec.writeAsBytes(sampleClaims())));
    jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A256KW);
    jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512);
    jwe.setKey(key);

    return jwe.getCompactSerialization();
  }

  // A JWE whose key-management algorithm is asymmetric (RSA-OAEP-256).
  private String asymmetricEncryptedToken (KeyPair keyPair)
    throws Exception {

    JsonWebEncryption jwe = new JsonWebEncryption();

    jwe.setPayload(new String(JsonCodec.writeAsBytes(sampleClaims())));
    jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.RSA_OAEP_256);
    jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512);
    jwe.setKey(keyPair.getPublic());

    return jwe.getCompactSerialization();
  }

  // A JWE wrapping a signed JWS, flagged as a nested JWT via the cty header. The inner JWS is signed
  // with the same key used for decryption, since process() verifies the JWS with the decryption key.
  private String nestedSignedThenEncryptedToken (SecretKey encryptionKey)
    throws Exception {

    JsonWebSignature jws = new JsonWebSignature();

    jws.setPayloadBytes(JsonCodec.writeAsBytes(sampleClaims()));
    jws.setHeader(HeaderParameterNames.TYPE, "JWT");
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
    jws.setKey(encryptionKey);

    String innerJws = jws.getCompactSerialization();
    JsonWebEncryption jwe = new JsonWebEncryption();

    jwe.setPayload(innerJws);
    jwe.setContentTypeHeaderValue("JWT");
    jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A256KW);
    jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_CBC_HMAC_SHA_512);
    jwe.setKey(encryptionKey);

    return jwe.getCompactSerialization();
  }

  public void testSymmetricEncryptedTokenDecrypts ()
    throws Exception {

    // Drives the JsonWebEncryption arm of process(): no signature, the payload is recovered by
    // decryption. require-signature is relaxed so the default policy does not reject the
    // signatureless token.
    SecretKey key = symmetricKey();
    Claims claims = new JWTConsumer().setRequireSignature(false).process(symmetricEncryptedToken(key), key, Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  public void testSymmetricEncryptionSatisfiesRequireEncryption ()
    throws Exception {

    // hasEncryption is true, so require-encryption passes.
    SecretKey key = symmetricKey();
    Claims claims = new JWTConsumer().setRequireSignature(false).setRequireEncryption(true).process(symmetricEncryptedToken(key), key, Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  public void testSymmetricEncryptionSatisfiesRequireIntegrity ()
    throws Exception {

    // Symmetric AEAD counts as integrity protection even with no signature, so require-integrity
    // passes through the hasSymmetricEncryption branch.
    SecretKey key = symmetricKey();
    Claims claims = new JWTConsumer().setRequireSignature(false).setRequireIntegrity(true).process(symmetricEncryptedToken(key), key, Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  public void testAsymmetricEncryptedTokenDecrypts ()
    throws Exception {

    // RSA-OAEP key management is asymmetric, so hasSymmetricEncryption stays false while
    // hasEncryption is true. The private key decrypts the payload.
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    Claims claims = new JWTConsumer().setRequireSignature(false).process(asymmetricEncryptedToken(keyPair), keyPair.getPrivate(), Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  @Test(expectedExceptions = InvalidJWTException.class)
  public void testAsymmetricEncryptionDoesNotSatisfyRequireIntegrity ()
    throws Exception {

    // Asymmetric encryption is not AEAD integrity protection: with no signature and
    // hasSymmetricEncryption false, require-integrity must reject.
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

    new JWTConsumer().setRequireSignature(false).setRequireIntegrity(true).process(asymmetricEncryptedToken(keyPair), keyPair.getPrivate(), Claims.class);
  }

  public void testNestedSignedThenEncryptedTokenVerifiesAndDecrypts ()
    throws Exception {

    // The outer JWE carries cty=JWT, so isNestedJwt() is true and the loop peels it to reach the
    // inner JWS, whose signature is then verified with the same key. Both an encryption and a
    // signature layer are present.
    SecretKey key = symmetricKey();
    Claims claims = new JWTConsumer().process(nestedSignedThenEncryptedToken(key), key, Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  public void testNestedTokenSatisfiesRequireEncryptionAndRequireIntegrity ()
    throws Exception {

    // The nested token carries both layers, so the combined encryption + signature policy is met.
    SecretKey key = symmetricKey();
    Claims claims = new JWTConsumer().setRequireEncryption(true).setRequireIntegrity(true).process(nestedSignedThenEncryptedToken(key), key, Claims.class);

    Assert.assertEquals(claims.getSub(), "encrypted-subject");
  }

  public static class Claims {

    private String sub;

    public String getSub () {

      return sub;
    }

    public void setSub (String sub) {

      this.sub = sub;
    }
  }
}
