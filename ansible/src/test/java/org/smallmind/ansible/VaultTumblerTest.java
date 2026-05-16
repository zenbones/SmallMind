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

import java.nio.charset.StandardCharsets;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class VaultTumblerTest {

  private static final String PASSWORD = "correct-horse-battery-staple";
  private static final byte[] FIXED_SALT = new byte[] {
    0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
    (byte)0x88, (byte)0x99, (byte)0xAA, (byte)0xBB, (byte)0xCC, (byte)0xDD, (byte)0xEE, (byte)0xFF,
    0x10, 0x21, 0x32, 0x43, 0x54, 0x65, 0x76, (byte)0x87,
    (byte)0x98, (byte)0xA9, (byte)0xBA, (byte)0xCB, (byte)0xDC, (byte)0xED, (byte)0xFE, 0x0F
  };

  @BeforeClass
  public void beforeClass () {

    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      Security.addProvider(new BouncyCastleProvider());
    }
  }

  public void testRoundTripWithGeneratedSalt ()
    throws VaultCodecException {

    byte[] plaintext = "hello vault".getBytes(StandardCharsets.UTF_8);

    VaultTumbler encryptor = new VaultTumbler(PASSWORD);
    VaultCake cake = encryptor.encrypt(plaintext);

    Assert.assertNotNull(cake.getSalt());
    Assert.assertEquals(cake.getSalt().length, 32);
    Assert.assertEquals(cake.getHmac().length, 32);

    VaultTumbler decryptor = new VaultTumbler(PASSWORD, cake.getSalt());
    byte[] recovered = decryptor.decrypt(cake.getHmac(), cake.getEncrypted());

    Assert.assertEquals(recovered, plaintext);
  }

  public void testRoundTripWithFixedSalt ()
    throws VaultCodecException {

    byte[] plaintext = "deterministic payload".getBytes(StandardCharsets.UTF_8);

    VaultCake cake = new VaultTumbler(PASSWORD, FIXED_SALT).encrypt(plaintext);

    Assert.assertSame(cake.getSalt(), FIXED_SALT);

    byte[] recovered = new VaultTumbler(PASSWORD, FIXED_SALT).decrypt(cake.getHmac(), cake.getEncrypted());

    Assert.assertEquals(recovered, plaintext);
  }

  public void testEmptyPlaintextRoundTrips ()
    throws VaultCodecException {

    VaultCake cake = new VaultTumbler(PASSWORD, FIXED_SALT).encrypt(new byte[0]);
    byte[] recovered = new VaultTumbler(PASSWORD, FIXED_SALT).decrypt(cake.getHmac(), cake.getEncrypted());

    Assert.assertEquals(recovered.length, 0);
  }

  @Test(expectedExceptions = VaultPasswordException.class)
  public void testWrongPasswordFailsHmacCheck ()
    throws VaultCodecException {

    byte[] plaintext = "secret".getBytes(StandardCharsets.UTF_8);
    VaultCake cake = new VaultTumbler(PASSWORD, FIXED_SALT).encrypt(plaintext);

    new VaultTumbler("wrong-password", FIXED_SALT).decrypt(cake.getHmac(), cake.getEncrypted());
  }
}
