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
import java.security.Key;
import org.smallmind.nutsnbolts.security.HMACSigningAlgorithm;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link SymmetricJWTKeyMaster} secret-to-key derivation: the algorithm is always
 * {@code HS256}, secrets shorter than 32 UTF-8 bytes are SHA-256 hashed into a 256-bit key, and
 * secrets already at least 32 bytes are used verbatim. Also confirms the derived key actually signs
 * and verifies through {@link JWTCodec}.
 */
@Test(groups = "unit")
public class SymmetricJWTKeyMasterTest {

  public void testAlgorithmIsHs256 ()
    throws Exception {

    Assert.assertEquals(new SymmetricJWTKeyMaster("any-secret").getEncryptionAlgorithm(), JWTEncryptionAlgorithm.HS256);
  }

  public void testDerivedKeyIsHmacSha256 ()
    throws Exception {

    Key key = new SymmetricJWTKeyMaster("any-secret").getKey();

    Assert.assertEquals(key.getAlgorithm(), "HmacSHA256");
  }

  public void testShortSecretHashedToFullKeyLength ()
    throws Exception {

    // A sub-32-byte secret is SHA-256 hashed, yielding a 256-bit (32-byte) key.
    Key key = new SymmetricJWTKeyMaster("short").getKey();

    Assert.assertEquals(key.getEncoded().length, 32);
  }

  public void testLongSecretUsedVerbatim ()
    throws Exception {

    // A secret already at least 32 bytes is used directly without hashing, so the key material
    // equals the raw UTF-8 bytes of the secret.
    String longSecret = "a-shared-secret-of-at-least-32-bytes-long";
    Key key = new SymmetricJWTKeyMaster(longSecret).getKey();

    Assert.assertEquals(key.getEncoded(), longSecret.getBytes(StandardCharsets.UTF_8));
  }

  public void testExactlyThirtyTwoByteSecretUsedVerbatim ()
    throws Exception {

    // Exactly 32 bytes sits on the boundary (length < 32 is false), so it is not hashed.
    String secret = "0123456789012345678901234567890X";
    Key key = new SymmetricJWTKeyMaster(secret).getKey();

    Assert.assertEquals(key.getEncoded().length, 32);
    Assert.assertEquals(key.getEncoded(), secret.getBytes(StandardCharsets.UTF_8));
  }

  public void testThirtyOneByteSecretIsHashed ()
    throws Exception {

    // One byte under the boundary is hashed, so the key is the 32-byte digest, not the 31 raw bytes.
    String secret = "012345678901234567890123456789X";
    Key key = new SymmetricJWTKeyMaster(secret).getKey();

    Assert.assertEquals(secret.getBytes(StandardCharsets.UTF_8).length, 31);
    Assert.assertEquals(key.getEncoded().length, 32);
    Assert.assertNotEquals(key.getEncoded(), secret.getBytes(StandardCharsets.UTF_8));
  }

  public void testTwoMastersFromSameSecretShareKeyMaterial ()
    throws Exception {

    Assert.assertEquals(new SymmetricJWTKeyMaster("repeatable").getKey().getEncoded(), new SymmetricJWTKeyMaster("repeatable").getKey().getEncoded());
  }

  public void testDerivedKeyMatchesDirectHmacGeneration ()
    throws Exception {

    // The long-secret path must produce exactly the same key bytes as generating the HMAC key
    // directly from the raw secret bytes.
    String longSecret = "another-secret-comfortably-over-thirty-two-bytes";
    Key direct = HMACSigningAlgorithm.HMAC_SHA_256.generateKey(longSecret.getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(new SymmetricJWTKeyMaster(longSecret).getKey().getEncoded(), direct.getEncoded());
  }

  public void testDerivedKeySignsAndVerifies ()
    throws Exception {

    SymmetricJWTKeyMaster keyMaster = new SymmetricJWTKeyMaster("round-trip-secret");

    String token = JWTCodec.encode(new Claims("subject-7"), keyMaster);
    Claims recovered = JWTCodec.decode(token, keyMaster, Claims.class);

    Assert.assertEquals(recovered.getSub(), "subject-7");
  }

  public static class Claims {

    private String sub;

    public Claims () {

    }

    public Claims (String sub) {

      this.sub = sub;
    }

    public String getSub () {

      return sub;
    }

    public void setSub (String sub) {

      this.sub = sub;
    }
  }
}
