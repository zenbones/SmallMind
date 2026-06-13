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

import org.smallmind.web.jwt.jose4j.JWTConsumer;
import org.smallmind.web.jwt.jose4j.InvalidJWTException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the {@link JWTConsumer} policy matrix — require-signature, require-encryption,
 * require-integrity, and signature skipping — against signed HS256 tokens.
 *
 * <p>The unsecured ({@code none}) algorithm is not exercised because {@link JWTConsumer} carries no
 * {@code none}-specific handling: jose4j installs {@code DISALLOW_NONE} by default, so an unsecured
 * token is rejected during signature verification.
 */
@Test(groups = "unit")
public class JWTConsumerTest {

  private static final String SECRET = "consumer-test-secret";

  private Claims sampleClaims () {

    Claims claims = new Claims();

    claims.setSub("subject-1");

    return claims;
  }

  private String signedToken ()
    throws Exception {

    return JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));
  }

  public void testSignedTokenVerifiesByDefault ()
    throws Exception {

    Claims claims = new JWTConsumer().process(signedToken(), new SymmetricJWTKeyMaster(SECRET).getKey(), Claims.class);

    Assert.assertEquals(claims.getSub(), "subject-1");
  }

  public void testRequireIntegritySatisfiedBySignature ()
    throws Exception {

    Claims claims = new JWTConsumer().setRequireIntegrity(true).process(signedToken(), new SymmetricJWTKeyMaster(SECRET).getKey(), Claims.class);

    Assert.assertEquals(claims.getSub(), "subject-1");
  }

  @Test(expectedExceptions = InvalidJWTException.class)
  public void testRequireEncryptionRejectsSignedOnlyToken ()
    throws Exception {

    new JWTConsumer().setRequireEncryption(true).process(signedToken(), new SymmetricJWTKeyMaster(SECRET).getKey(), Claims.class);
  }

  public void testSkipSignatureVerificationWithoutKey ()
    throws Exception {

    Claims claims = new JWTConsumer().setSkipSignatureVerification(true).process(signedToken(), null, Claims.class);

    Assert.assertEquals(claims.getSub(), "subject-1");
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
