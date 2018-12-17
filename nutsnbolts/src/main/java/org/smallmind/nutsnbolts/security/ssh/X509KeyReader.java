/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.nutsnbolts.security.ssh;

import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.smallmind.nutsnbolts.http.Base64Codec;

public class X509KeyReader implements SSHKeyReader {

  @Override
  public SSHKeyFactors extractFactors (String raw)
    throws IOException, SSHParseException {

    StringBuilder stripedRawBuilder = new StringBuilder();

    for (int index = 0; index < raw.length(); index++) {

      char currentChar = raw.charAt(index);

      if ((currentChar != ' ') && (currentChar != '\n')) {
        stripedRawBuilder.append(currentChar);
      }
    }

    ASN1Sequence outerSequence = (ASN1Sequence)ASN1Sequence.fromByteArray(Base64Codec.decode(stripedRawBuilder.toString()));

    if (outerSequence.size() < 1) {
      throw new SSHParseException("ASN.1 outer sequence is missing elements");
    } else {

      Object firstObject = outerSequence.getObjectAt(0);

      if (firstObject instanceof ASN1Sequence) {

        if (outerSequence.size() < 2) {
          throw new SSHParseException("ASN.1 outer sequence is missing elements");
        } else {

          ASN1Sequence identifierSequence = ((ASN1Sequence)firstObject);

          if ((identifierSequence.size() < 1)) {
            throw new SSHParseException("ASN.1 identifier sequence is empty");
          } else {

            ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier)identifierSequence.getObjectAt(0);

            if (!oid.getId().equals("1.2.840.113549.1.1.1")) {
              throw new IllegalArgumentException("Unknown RSA object id");
            } else {

              ASN1Sequence dataSequence = (ASN1Sequence)ASN1Sequence.fromByteArray(((ASN1BitString)outerSequence.getObjectAt(1)).getBytes());

              if (dataSequence.size() < 2) {
                throw new SSHParseException("ASN.1 data sequence is missing elements");
              }

              BigInteger modulus = ((ASN1Integer)dataSequence.getObjectAt(0)).getValue();
              BigInteger exponent = ((ASN1Integer)dataSequence.getObjectAt(1)).getValue();

              return new SSHKeyFactors(modulus, exponent);
            }
          }
        }
      } else {

        if (outerSequence.size() < 4) {
          throw new SSHParseException("ASN.1 outer sequence is missing elements");
        } else {

          int version = ((ASN1Integer)firstObject).getValue().intValue();

          if (version != 0 && version != 1) {
            throw new IllegalArgumentException("Wrong version for RSA key");
          }

          BigInteger modulus = ((ASN1Integer)outerSequence.getObjectAt(1)).getValue();
          BigInteger exponent = ((ASN1Integer)outerSequence.getObjectAt(3)).getValue();

          return new SSHKeyFactors(modulus, exponent);
        }
      }
    }
  }
}
