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

import java.util.LinkedList;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.JsonWebStructure;
import org.smallmind.web.jwt.jose4j.InvalidJWTException;
import org.smallmind.web.jwt.jose4j.InvalidJWTSignatureException;
import org.smallmind.web.jwt.jose4j.JWTConsumer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the negative and policy-branch paths of {@link JWTConsumer}: signature verification
 * failure under a wrong key, malformed input, the {@code requireSignature} default rejecting a
 * signatureless token (driven through {@code processContext} since jose4j refuses to parse an
 * unsecured {@code none} token), and the {@code requireSignature(false)}/{@code requireIntegrity}
 * combinations.
 */
@Test(groups = "unit")
public class JWTConsumerNegativeTest {

  private static final String SECRET = "consumer-negative-secret";
  private static final String OTHER_SECRET = "consumer-other-secret";

  private Claims sampleClaims () {

    Claims claims = new Claims();

    claims.setSub("subject-x");

    return claims;
  }

  private String signedToken ()
    throws Exception {

    return JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));
  }

  @Test(expectedExceptions = InvalidJWTSignatureException.class)
  public void testWrongKeyFailsSignature ()
    throws Exception {

    new JWTConsumer().process(signedToken(), new SymmetricJWTKeyMaster(OTHER_SECRET).getKey(), Claims.class);
  }

  @Test(expectedExceptions = Exception.class)
  public void testMalformedTokenRejected ()
    throws Exception {

    new JWTConsumer().process("not.a.jwt", new SymmetricJWTKeyMaster(SECRET).getKey(), Claims.class);
  }

  public void testRequireSignatureFalseStillVerifiesPresentSignature ()
    throws Exception {

    // Relaxing the require-signature policy does not disable verification of a signature that is
    // present, so a valid key still recovers the claims.
    Claims claims = new JWTConsumer().setRequireSignature(false).process(signedToken(), new SymmetricJWTKeyMaster(SECRET).getKey(), Claims.class);

    Assert.assertEquals(claims.getSub(), "subject-x");
  }

  public void testSkipVerificationLeavesRequireIntegritySatisfiedBySignaturePresence ()
    throws Exception {

    // Even with verification skipped, the JWS structure counts as a present signature, so
    // require-integrity is satisfied and the (unverified) claims are returned.
    Claims claims = new JWTConsumer().setSkipSignatureVerification(true).setRequireIntegrity(true).process(signedToken(), null, Claims.class);

    Assert.assertEquals(claims.getSub(), "subject-x");
  }

  @Test(expectedExceptions = InvalidJWTException.class)
  public void testRequireSignatureRejectsContextWithNoSignature ()
    throws Exception {

    // jose4j's DISALLOW_NONE policy prevents constructing an unsecured token through process(), so the
    // require-signature/!hasSignature branch is driven directly via processContext with an empty layer
    // list.
    new JWTConsumer().processContext("token", null, new LinkedList<>());
  }

  public void testRequireSignatureFalseAcceptsContextWithNoSignature ()
    throws Exception {

    // With require-signature disabled and no other requirement, an empty layer list passes the policy
    // check without throwing.
    new JWTConsumer().setRequireSignature(false).processContext("token", null, new LinkedList<>());
  }

  @Test(expectedExceptions = InvalidJWTException.class)
  public void testRequireIntegrityRejectsContextWithNoProtection ()
    throws Exception {

    // No signature and no encryption: require-integrity must reject. require-signature is relaxed so
    // the integrity branch is the one that fires.
    new JWTConsumer().setRequireSignature(false).setRequireIntegrity(true).processContext("token", null, new LinkedList<>());
  }

  public void testProcessContextWithSignatureLayerSatisfiesRequireSignature ()
    throws Exception {

    // A JWS layer in the list satisfies require-signature; with verification skipped no key is needed.
    JsonWebStructure jws = JsonWebStructure.fromCompactSerialization(signedToken());
    LinkedList<JsonWebStructure> layers = new LinkedList<>();

    layers.addFirst(jws);

    Assert.assertTrue(jws instanceof JsonWebSignature);

    new JWTConsumer().setSkipSignatureVerification(true).processContext("token", null, layers);
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
