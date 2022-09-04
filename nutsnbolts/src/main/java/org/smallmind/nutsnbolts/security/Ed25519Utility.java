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
package org.smallmind.nutsnbolts.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
// import java.security.spec.EdECPoint;
// import java.security.spec.EdECPrivateKeySpec;
// import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import org.bouncycastle.util.Arrays;
import org.smallmind.nutsnbolts.http.Base64Codec;

//TODO: We await java 17
public class Ed25519Utility {

  private static final String OPENSSH_KEY_V1 = "openssh-key-v1";

  public static void main (String... args) throws Exception {
    /*

    String pub = "AAAAC3NzaC1lZDI1NTE5AAAAIJ7cvxjZ1uYYJ1JAHGwcxj7GrHcSE4/+gFPiJmtWImLI";
    String priv = "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZWQyNTUxOQAAACCe3L8Y2dbmGCdSQBxsHMY+xqx3EhOP/oBT4iZrViJiyAAAAJiI0v1hiNL9YQAAAAtzc2gtZWQyNTUxOQAAACCe3L8Y2dbmGCdSQBxsHMY+xqx3EhOP/oBT4iZrViJiyAAAAEBLQKllpr2C/jJGK1oluiI2NrjpgacEzxDrtXnc6as+0J7cvxjZ1uYYJ1JAHGwcxj7GrHcSE4/+gFPiJmtWImLIAAAAEHN5c3RlbUBmb3Jpby5jb20BAgMEBQ==";

    KeyFactory kf = KeyFactory.getInstance("Ed25519");
    byte[] pk = Base64Codec.decode(pub);
    // determine if x was odd.
    boolean xisodd = false;
    int lastbyteInt = pk[pk.length - 1];
    if ((lastbyteInt & 255) >> 7 == 1) {
      xisodd = true;
    }
    // make sure most significant bit will be 0 - after reversing.
    pk[pk.length - 1] &= 127;
    // apparently we must reverse the byte array...
    pk = Arrays.reverse(pk);
    BigInteger y = new BigInteger(1, pk);

    NamedParameterSpec paramSpec = new NamedParameterSpec("Ed25519");
    EdECPoint ep = new EdECPoint(xisodd, y);
    EdECPublicKeySpec pubSpec = new EdECPublicKeySpec(paramSpec, ep);
    PublicKey pubKey = kf.generatePublic(pubSpec);

    byte[] rawKey = Base64Codec.decode(priv);

    byte[] verify = java.util.Arrays.copyOfRange(rawKey, 0, OPENSSH_KEY_V1.length());
    if (!new String(verify).equals(OPENSSH_KEY_V1)) {
      throw new RuntimeException("Invalid OPENSSH file");
    }

    boolean occurred = false;
    int index = 0;
    for (int i = 0; i < rawKey.length; i++) {
      if (rawKey[i] == 's'
            && rawKey[i + 1] == 's'
            && rawKey[i + 2] == 'h'
            && rawKey[i + 3] == '-'
            && rawKey[i + 4] == 'e'
            && rawKey[i + 5] == 'd'
            && rawKey[i + 6] == '2'
            && rawKey[i + 7] == '5'
            && rawKey[i + 8] == '5'
            && rawKey[i + 9] == '1'
            && rawKey[i + 10] == '9'
            && rawKey[i + 11] == 0x00
            && rawKey[i + 12] == 0x00
            && rawKey[i + 13] == 0x00
            && rawKey[i + 14] == ' ') {
        index = i + 15;
        if (occurred) {
          break;
        }
        occurred = true;
      }
    }

    byte[] publicKey = java.util.Arrays.copyOfRange(rawKey, index, index + 32);

    index += 32;
    for (int i = index; i < rawKey.length; i++) {
      if (rawKey[i] == 0x00
            && rawKey[i + 1] == 0x00
            && rawKey[i + 2] == 0x00
            && rawKey[i + 3] == '@') {
        index = i + 4;
        break;
      }
    }

    byte[] privateKey = java.util.Arrays.copyOfRange(rawKey, index, index + 32);

    EdECPrivateKeySpec privSpec = new EdECPrivateKeySpec(paramSpec, privateKey);

    PrivateKey privKey = kf.generatePrivate(privSpec);

    System.out.println(pubKey.getAlgorithm());
    System.out.println(privKey.getAlgorithm());

    Signature sig = Signature.getInstance("Ed25519");
//    Cipher cipher = Cipher.getInstance("Ed25519", new BouncyCastleProvider());

    KeyPair pair = KeyPairGenerator.getInstance("Ed25519") .generateKeyPair();
    System.out.println(pair.getPrivate().getAlgorithm());
    System.out.println(pair.getPublic().getAlgorithm());

     */
  }
}
