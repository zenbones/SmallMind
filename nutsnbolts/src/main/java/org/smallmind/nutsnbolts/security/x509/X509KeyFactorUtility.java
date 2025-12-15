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
package org.smallmind.nutsnbolts.security.x509;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.security.KeyParsingException;

/**
 * Utility for extracting RSA key factors from X.509 encoded keys.
 */
public class X509KeyFactorUtility {

  /**
   * Extracts modulus and exponent from an X.509 encoded RSA key.
   *
   * @param raw the raw PEM or Base64 encoded key text
   * @return parsed RSA key factors
   * @throws IOException         if the key cannot be decoded
   * @throws KeyParsingException if the ASN.1 structure is invalid
   */
  public static RSAKeyFactors extractKeyFactor (String raw)
    throws IOException, KeyParsingException {

    StringBuilder strippedRawBuilder = new StringBuilder();

    for (int index = 0; index < raw.length(); ++index) {
      char currentChar = raw.charAt(index);
      if (currentChar != ' ' && currentChar != '\n') {
        strippedRawBuilder.append(currentChar);
      }
    }

    ASN1Sequence outerSequence = (ASN1Sequence)ASN1Sequence.fromByteArray(Base64Codec.decode(strippedRawBuilder.toString()));
    if (outerSequence.size() < 2) {
      throw new KeyParsingException("ASN.1 sequence could not be parsed", new Object[0]);
    } else {
      if (outerSequence.size() == 2) {
        ASN1Sequence identifierSequence = (ASN1Sequence)outerSequence.getObjectAt(0);
        if (identifierSequence.size() < 1) {
          throw new KeyParsingException("ASN.1 identifier sequence is empty", new Object[0]);
        }

        ASN1ObjectIdentifier var10000 = (ASN1ObjectIdentifier)identifierSequence.getObjectAt(0);
        ASN1Sequence dataSequence = (ASN1Sequence)ASN1Sequence.fromByteArray(((ASN1BitString)outerSequence.getObjectAt(1)).getBytes());
        if (dataSequence.size() < 2) {
          throw new KeyParsingException("ASN.1 data sequence is missing elements", new Object[0]);
        }

        BigInteger modulus = ((ASN1Integer)dataSequence.getObjectAt(0)).getValue();
        BigInteger exponent = ((ASN1Integer)dataSequence.getObjectAt(1)).getValue();

        return new RSAKeyFactors(modulus, exponent);
      } else {
        int version = ((ASN1Integer)outerSequence.getObjectAt(0)).getValue().intValue();
        if (version != 0 && version != 1) {
          throw new IllegalArgumentException("Wrong version for RSA key");
        }

        if (outerSequence.size() == 3) {
          ASN1Sequence identifierSequence = (ASN1Sequence)outerSequence.getObjectAt(1);
          if (identifierSequence.size() < 1) {
            throw new KeyParsingException("ASN.1 identifier sequence is empty", new Object[0]);
          }

          ASN1ObjectIdentifier var17 = (ASN1ObjectIdentifier)identifierSequence.getObjectAt(0);
          ASN1Sequence dataSequence = (ASN1Sequence)ASN1Sequence.fromByteArray(((DEROctetString)outerSequence.getObjectAt(2)).getOctets());
          BigInteger modulus = ((ASN1Integer)dataSequence.getObjectAt(1)).getValue();
          BigInteger exponent = ((ASN1Integer)dataSequence.getObjectAt(3)).getValue();

          return new RSAKeyFactors(modulus, exponent);
        } else {
          BigInteger modulus = ((ASN1Integer)outerSequence.getObjectAt(1)).getValue();
          BigInteger exponent = ((ASN1Integer)outerSequence.getObjectAt(3)).getValue();

          return new RSAKeyFactors(modulus, exponent);
        }
      }
    }
  }
}
