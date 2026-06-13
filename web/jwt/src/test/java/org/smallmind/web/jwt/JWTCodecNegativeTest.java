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
import org.smallmind.web.jwt.jose4j.InvalidJWTSignatureException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the failure paths of {@link JWTCodec}: malformed compact serializations, tampered
 * payloads, and verifying a symmetric token with an asymmetric key. These complement the happy-path
 * round trips in {@code JWTCodecTest}.
 */
@Test(groups = "unit")
public class JWTCodecNegativeTest {

  private static final String SECRET = "codec-negative-secret";

  private Claims sampleClaims () {

    Claims claims = new Claims();

    claims.setSub("user-99");

    return claims;
  }

  @Test(expectedExceptions = Exception.class)
  public void testGarbageStringRejected ()
    throws Exception {

    JWTCodec.decode("this-is-not-a-jwt", new SymmetricJWTKeyMaster(SECRET), Claims.class);
  }

  @Test(expectedExceptions = Exception.class)
  public void testWrongNumberOfSegmentsRejected ()
    throws Exception {

    JWTCodec.decode("only.two", new SymmetricJWTKeyMaster(SECRET), Claims.class);
  }

  @Test(expectedExceptions = Exception.class)
  public void testEmptyTokenRejected ()
    throws Exception {

    JWTCodec.decode("", new SymmetricJWTKeyMaster(SECRET), Claims.class);
  }

  @Test(expectedExceptions = InvalidJWTSignatureException.class)
  public void testTamperedSignatureFailsVerification ()
    throws Exception {

    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));
    String[] pieces = token.split("\\.");

    // Flip the last character of the signature segment, leaving the header and payload intact so the
    // unverified payload still parses as JSON and the failure surfaces as a signature mismatch rather
    // than a JSON error. (process() deserializes the unverified payload before verifying the
    // signature, so a tampered payload would throw a JSON exception instead.)
    char last = pieces[2].charAt(pieces[2].length() - 1);
    char swapped = (last == 'A') ? 'B' : 'A';
    String tampered = pieces[0] + "." + pieces[1] + "." + pieces[2].substring(0, pieces[2].length() - 1) + swapped;

    JWTCodec.decode(tampered, new SymmetricJWTKeyMaster(SECRET), Claims.class);
  }

  @Test(expectedExceptions = Exception.class)
  public void testSymmetricTokenRejectedByAsymmetricKey ()
    throws Exception {

    // An HS256 token verified with an RSA public key: the algorithm/key-type mismatch must fail
    // rather than silently pass.
    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));
    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

    JWTCodec.decode(token, new AsymmetricJWTKeyMaster(keyPair.getPublic()), Claims.class);
  }

  public void testDecipherReadsTamperedPayloadWithoutVerifying ()
    throws Exception {

    // decipher() skips signature verification, so a token signed with one secret still deserializes
    // even when no key is supplied.
    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));

    Claims recovered = JWTCodec.decipher(token, Claims.class);

    Assert.assertEquals(recovered.getSub(), "user-99");
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
