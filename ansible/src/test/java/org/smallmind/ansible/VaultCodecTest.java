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
package org.smallmind.ansible;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class VaultCodecTest {

  private static final String PASSWORD = "correct-horse-battery-staple";

  private static ByteArrayInputStream streamOf (String text) {

    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  private static ByteArrayInputStream streamOf (byte[] bytes) {

    return new ByteArrayInputStream(bytes);
  }

  @BeforeClass
  public void beforeClass () {

    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public void testRoundTripFormat11 ()
    throws IOException, VaultCodecException {

    byte[] plaintext = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    String vault = VaultCodec.encrypt(streamOf(plaintext), PASSWORD);

    Assert.assertTrue(vault.startsWith("$ANSIBLE_VAULT;1.1;AES256\n"), "unexpected header: " + vault);

    byte[] recovered = VaultCodec.decrypt(streamOf(vault), PASSWORD);

    Assert.assertEquals(recovered, plaintext);
  }

  public void testRoundTripFormat12WithId ()
    throws IOException, VaultCodecException {

    byte[] plaintext = "labeled vault content".getBytes(StandardCharsets.UTF_8);

    String vault = VaultCodec.encrypt(streamOf(plaintext), PASSWORD, "production");

    Assert.assertTrue(vault.startsWith("$ANSIBLE_VAULT;1.2;AES256;production\n"), "unexpected header: " + vault);

    byte[] recovered = VaultCodec.decrypt(streamOf(vault), PASSWORD);

    Assert.assertEquals(recovered, plaintext);
  }

  public void testNullIdProducesFormat11 ()
    throws IOException, VaultCodecException {

    String vault = VaultCodec.encrypt(streamOf("payload"), PASSWORD, null);

    Assert.assertTrue(vault.startsWith("$ANSIBLE_VAULT;1.1;AES256\n"));
  }

  public void testBodyIsWrappedAtEightyCharacters ()
    throws IOException, VaultCodecException {

    String vault = VaultCodec.encrypt(streamOf("x"), PASSWORD);
    String[] lines = vault.split("\n", -1);

    Assert.assertTrue(lines.length > 2, "expected multi-line body, got: " + vault);
    for (int i = 1; i < lines.length - 1; i++) {
      Assert.assertEquals(lines[i].length(), 80, "line " + i + " not 80 chars: " + lines[i]);
    }
  }

  public void testRoundTripEmptyPlaintext ()
    throws IOException, VaultCodecException {

    String vault = VaultCodec.encrypt(streamOf(new byte[0]), PASSWORD);
    byte[] recovered = VaultCodec.decrypt(streamOf(vault), PASSWORD);

    Assert.assertEquals(recovered.length, 0);
  }

  @Test(expectedExceptions = VaultPasswordException.class)
  public void testWrongPasswordFailsDecrypt ()
    throws IOException, VaultCodecException {

    String vault = VaultCodec.encrypt(streamOf("secret"), PASSWORD);

    VaultCodec.decrypt(streamOf(vault), "not-the-password");
  }

  @Test(expectedExceptions = VaultCodecException.class)
  public void testUnknownHeaderFails ()
    throws IOException, VaultCodecException {

    VaultCodec.decrypt(streamOf("not-a-vault-file\nbody"), PASSWORD);
  }

  @Test(expectedExceptions = VaultCodecException.class)
  public void testUnsupportedCipherFails ()
    throws IOException, VaultCodecException {

    VaultCodec.decrypt(streamOf("$ANSIBLE_VAULT;1.1;DES\n0000"), PASSWORD);
  }

  @Test(expectedExceptions = VaultCodecException.class)
  public void testFormat11WithExtraSegmentFails ()
    throws IOException, VaultCodecException {

    VaultCodec.decrypt(streamOf("$ANSIBLE_VAULT;1.1;AES256;extra\n0000"), PASSWORD);
  }
}
