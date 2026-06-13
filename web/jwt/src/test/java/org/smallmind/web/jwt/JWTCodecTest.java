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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.smallmind.web.jwt.jose4j.InvalidJWTSignatureException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises {@link JWTCodec} end to end with in-memory keys: symmetric and asymmetric sign/verify
 * round trips, signature rejection under the wrong key, the {@code kid} header path, and the
 * signature-skipping {@code decipher} path.
 */
@Test(groups = "unit")
public class JWTCodecTest {

  // SymmetricJWTKeyMaster SHA-256 hashes the secret into a fixed 256-bit key, so a short secret is
  // accepted even though HS256 itself requires a 256-bit key.
  private static final String SECRET = "secret";
  private static final String OTHER_SECRET = "different-secret";

  private Claims sampleClaims () {

    Claims claims = new Claims();

    claims.setSub("user-42");
    claims.setRole("admin");

    return claims;
  }

  public void testSymmetricRoundTrip ()
    throws Exception {

    JWTKeyMaster keyMaster = new SymmetricJWTKeyMaster(SECRET);

    String token = JWTCodec.encode(sampleClaims(), keyMaster);
    Claims recovered = JWTCodec.decode(token, keyMaster, Claims.class);

    Assert.assertEquals(recovered.getSub(), "user-42");
    Assert.assertEquals(recovered.getRole(), "admin");
  }

  @Test(expectedExceptions = InvalidJWTSignatureException.class)
  public void testWrongSecretFailsVerification ()
    throws Exception {

    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));

    JWTCodec.decode(token, new SymmetricJWTKeyMaster(OTHER_SECRET), Claims.class);
  }

  public void testAsymmetricRoundTrip ()
    throws Exception {

    KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

    String token = JWTCodec.encode(sampleClaims(), new AsymmetricJWTKeyMaster(keyPair.getPrivate()));
    Claims recovered = JWTCodec.decode(token, new AsymmetricJWTKeyMaster(keyPair.getPublic()), Claims.class);

    Assert.assertEquals(recovered.getSub(), "user-42");
  }

  public void testEd25519RoundTrip ()
    throws Exception {

    KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();

    String token = JWTCodec.encode(sampleClaims(), new AsymmetricJWTKeyMaster(keyPair.getPrivate()));
    Claims recovered = JWTCodec.decode(token, new AsymmetricJWTKeyMaster(keyPair.getPublic()), Claims.class);

    Assert.assertEquals(recovered.getSub(), "user-42");
  }

  public void testKeyIdEmbeddedInHeader ()
    throws Exception {

    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET), "key-2024");
    String header = new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))), StandardCharsets.UTF_8);

    Assert.assertTrue(header.contains("key-2024"), header);
  }

  public void testDecipherSkipsSignatureVerification ()
    throws Exception {

    String token = JWTCodec.encode(sampleClaims(), new SymmetricJWTKeyMaster(SECRET));

    Claims recovered = JWTCodec.decipher(token, Claims.class);

    Assert.assertEquals(recovered.getSub(), "user-42");
  }

  public static class Claims {

    private String sub;
    private String role;

    public String getSub () {

      return sub;
    }

    public void setSub (String sub) {

      this.sub = sub;
    }

    public String getRole () {

      return role;
    }

    public void setRole (String role) {

      this.role = role;
    }
  }
}
