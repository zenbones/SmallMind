/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.AsymmetricAlgorithm;
import org.smallmind.nutsnbolts.security.SecurityProvider;
import org.smallmind.nutsnbolts.security.key.AsymmetricKeyType;
import org.smallmind.nutsnbolts.security.key.KeyFactors;
import org.smallmind.nutsnbolts.security.key.KeyParser;
import org.smallmind.nutsnbolts.security.key.OpenSSHPubicKeyUtility;
import org.smallmind.nutsnbolts.security.key.X509KeyParser;

public class Wombat {

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  public static void main (String... args)
    throws Exception {

    String openssh;

    KeyParser keyParser = new X509KeyParser("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzVZf2eeD6AInl3wgyZcHr8FlDDfG3mwXpqXXmM5B6rX4lN09fxAjhM9AAUm8KNanNLV5MrSdQZq4wqTXDyOhcuFYal3mzi4qZ/LpM3MiX1BMU7WHrjRUw4M9G6PQGgty6xE8tdDb8ywRpiuvS1L4Uw52P/sCBh8lVzbL+JinIpZLh+9Azs1/nPkKHr/+SuyIG/zdD24reU02YvdXdIsQTzMSkzssiFDaIvsx7ribTC2EPgylUOCFpRZYWVpPdveSE7ttQsm4+Lv4rXjPWl8C/NXN/z9pxj4hnHy/i8fC1y/0f30BRB8Qnb11oKwOdewTnSVF65J69lwCC5G2ig44gwIDAQAB");
    System.out.println(openssh = OpenSSHPubicKeyUtility.convert(keyParser.extractFactors()));

    RSAPublicKey key = (RSAPublicKey)AsymmetricKeyType.PUBLIC.generateKey(AsymmetricAlgorithm.RSA, SecurityProvider.BOUNCY_CASTLE, openssh);

    KeyFactors keyFactors = new KeyFactors(key.getModulus(), key.getPublicExponent());
    System.out.println(key.getAlgorithm());
    System.out.println(key.getFormat());

    EdDSAPublicKey pubkey = (EdDSAPublicKey)AsymmetricKeyType.PUBLIC.generateKey(AsymmetricAlgorithm.ED25519, SecurityProvider.BOUNCY_CASTLE, "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBa/L+1f0SRlLPAxBeQU2UG7zgKzBcNIyqqCXq3fyG0p root@ops-dev-build-1t");

    System.out.println(pubkey.getAlgorithm());
    System.out.println(pubkey.getFormat());
    System.out.println(Base64Codec.urlSafeEncode(pubkey.getPointEncoding()));

    PrivateKey privKey = (PrivateKey)AsymmetricKeyType.PRIVATE.generateKey(AsymmetricAlgorithm.ED25519, SecurityProvider.BOUNCY_CASTLE, "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
                                                                                                                                          "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n" +
                                                                                                                                          "QyNTUxOQAAACAWvy/tX9EkZSzwMQXkFNlBu84CswXDSMqqgl6t38htKQAAAJinA8axpwPG\n" +
                                                                                                                                          "sQAAAAtzc2gtZWQyNTUxOQAAACAWvy/tX9EkZSzwMQXkFNlBu84CswXDSMqqgl6t38htKQ\n" +
                                                                                                                                          "AAAEAP0kAMIeXi8thWh+nasvyjRdvpE2DMPTrBPDvyNCmmARa/L+1f0SRlLPAxBeQU2UG7\n" +
                                                                                                                                          "zgKzBcNIyqqCXq3fyG0pAAAAFHJvb3RAb3BzLWRldi1idWlsZC0xAQ==\n" +
                                                                                                                                          "-----END OPENSSH PRIVATE KEY-----");

    String jwt = JWTCodec.encode("hooptyfrood", new AsymmetricJWTKeyMaster(privKey));
    System.out.println(jwt);
    System.out.println(JWTCodec.decipher(jwt, String.class));
    System.out.println(JWTCodec.decode(jwt, new AsymmetricJWTKeyMaster(pubkey), String.class));

    foo(privKey, pubkey, null, "hooptyfrood".getBytes());
  }

  public static void foo (PrivateKey privKey, PublicKey pubKey, String specificAlgorithm, byte[] toBeEncrypted)
    throws Exception {

    Signature sig1 = Signature.getInstance((specificAlgorithm == null) ? privKey.getAlgorithm() : specificAlgorithm);
    sig1.initSign(privKey);
    sig1.update(toBeEncrypted);

    byte[] stuff = sig1.sign();

    Signature sig2 = Signature.getInstance((specificAlgorithm == null) ? pubKey.getAlgorithm() : specificAlgorithm);
    sig2.initVerify(pubKey);
    sig2.update(toBeEncrypted);

    System.out.println(sig2.verify(stuff));
  }
}
