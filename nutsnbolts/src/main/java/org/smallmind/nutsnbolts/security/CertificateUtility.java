/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

public class CertificateUtility {

  /*
  "CN=server.mycompany.com,OU=My Company Dev Team,O=My Company,C=US,ST=California,L=San Francisco"
  */
  public static String constructCertificate (String cn, String ou, String o, String c, String st, String l, Date notBefore, Date now, Date notAfter, AsymmetricAlgorithm algorithm, KeyPair keyPair)
    throws OperatorCreationException, CertificateException, IOException {

    X500Name x500Name = new X500Name("CN=%s,OU=%s,O=%s,C=%s,ST=%s,L=%s".formatted(cn, ou, o, c, st, l));

    X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
      x500Name,
      BigInteger.valueOf(now.getTime()),
      notBefore,
      notAfter,
      x500Name,
      SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
    );

    certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
    certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment));
    certificateBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(new KeyPurposeId[] {KeyPurposeId.id_kp_serverAuth, KeyPurposeId.id_kp_clientAuth}));

    X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(new JcaContentSignerBuilder(algorithm.getAlgorithmName()).build(keyPair.getPrivate())));

    try (StringWriter certificateWriter = new StringWriter()) {
      try (PemWriter pemWriter = new PemWriter(certificateWriter)) {
        pemWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
      }

      return certificateWriter.toString();
    }
  }
}
