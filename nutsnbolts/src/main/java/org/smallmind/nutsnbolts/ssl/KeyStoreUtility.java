/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
package org.smallmind.nutsnbolts.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.LinkedList;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;

public class KeyStoreUtility {

  /*
   * 1) openssl genrsa -des3 -out <private key file name>.key 2048
   *   a) When prompted for a pass phrase: enter a secure password and remember it, as this pass phrase is what protects the private key. Both the private
   *      key and the certificate are required to enable SSL.
   * 2) openssl req -new -key <private key file name>.key -out <csr file name>.csr
   *   a) Country Name: Use the two-letter code without punctuation for country, for example: US or CA.
   *   b) State or Province: Spell out the state completely; do not abbreviate the state or province name, for example: California
   *   c) Locality or City: The Locality field is the city or town name, for example: Berkeley. Do not abbreviate. For example: Saint Louis, not St. Louis
   *   d) Company: If the company or department has an &, @, or any other symbol using the shift key in its name, the symbol must be spelled out or omitted,
   *      in order to enroll. Example: XY & Z Corporation would be XYZ Corporation or XY and Z Corporation.
   *   e) Organizational Unit: This field is optional; but can be used to help identify certificates registered to an organization. The Organizational Unit
   *      (OU) field is the name of the department or organization unit making the request. To skip the OU field, press Enter on the keyboard.
   *   f) Common Name: The Common Name is the Host + Domain Name. It looks like "www.company.com" or "company.com".
   *   g) NOTE: Please do not enter an email address, challenge password or an optional company name when generating the CSR.
   * 3) Send the csr to a CA which will generate and send back a signed cert.
   * 4) Once the certificate has been generated collect it and any intermediate and/or root certificates
   * 5) openssl pkcs8 -topk8 -nocrypt -in <private key file name>.key -out <private key file name>.key.der -outform der
   * 6) openssl x509 -in <cert file name>.cert -out <cert file name>.cert.der -outform der - for each cert
   * 7) Run this utility which will generate a JKS format keystore file
   */

  public static KeyStoreInfo construct (String keystoreName, String keystoreAlias, String keystorePassword, Resource keyResource, Resource... certResources)
    throws IOException, ResourceException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException, InvalidKeySpecException {

    String validatedName = ((keystoreName == null) || keystoreName.isEmpty()) ? "keystore.jks" : keystoreName.endsWith(".jks") ? keystoreName : keystoreName + ".jks";
    Path keystorePath = Paths.get(System.getProperty("user.home"), validatedName);
    KeyStoreInfo keyStoreInfo = new KeyStoreInfo(keystorePath);
    char[] passwordArray = generatePasswordArray(keystorePassword, keyStoreInfo);
    KeyStore keyStore = initializeKeystore(keystorePath, passwordArray);
    PrivateKey privateKey = generatePrivateKey(keyResource);
    Certificate[] certificates = linkCertificateChain(keyStore, certResources);
    String validatedAlias = ((keystoreAlias == null) || keystoreAlias.isEmpty()) ? "mykeystore" : keystoreAlias;

    keyStoreInfo.setKeystoreName(validatedName);
    keyStoreInfo.setKeystoreAlias(validatedAlias);
    keyStore.setKeyEntry(validatedAlias, privateKey, passwordArray, certificates);
    keyStore.store(Files.newOutputStream(keystorePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), passwordArray);

    return keyStoreInfo;
  }

  private static char[] generatePasswordArray (String keystorePassword, KeyStoreInfo keyStoreInfo) {

    String validatedPassword = ((keystorePassword == null) || keystorePassword.isEmpty()) ? "changeit" : keystorePassword;

    keyStoreInfo.setKeystorePassword(validatedPassword);

    return validatedPassword.toCharArray();
  }

  private static KeyStore initializeKeystore (Path keystorePath, char[] passwordArray)
    throws IOException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException, CertificateException {

    KeyStore keyStore = KeyStore.getInstance("JKS", "SUN");

    keyStore.load(null, passwordArray);
    keyStore.store(Files.newOutputStream(keystorePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), passwordArray);
    keyStore.load(Files.newInputStream(keystorePath, StandardOpenOption.READ), passwordArray);

    return keyStore;
  }

  private static PrivateKey generatePrivateKey (Resource keyResource)
    throws IOException, ResourceException, NoSuchAlgorithmException, InvalidKeySpecException {

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec;

    try (InputStream keyInputStream = keyResource.getInputStream()) {

      byte[] keyBuffer = new byte[keyInputStream.available()];
      int bytesRead = 0;

      while (bytesRead < keyBuffer.length) {
        bytesRead += keyInputStream.read(keyBuffer, bytesRead, keyBuffer.length - bytesRead);
      }

      keySpec = new PKCS8EncodedKeySpec(keyBuffer);
    }

    return keyFactory.generatePrivate(keySpec);
  }

  private static Certificate[] linkCertificateChain (KeyStore keyStore, Resource... certResources)
    throws ResourceException, CertificateException, KeyStoreException {

    LinkedList<Certificate> certificateList = new LinkedList<>();
    Certificate[] certificates;

    for (Resource certResource : certResources) {
      for (Certificate certificate : CertificateFactory.getInstance("X509").generateCertificates(certResource.getInputStream())) {
        keyStore.setCertificateEntry(((X509Certificate)certificate).getSubjectX500Principal().getName(), certificate);
        certificateList.add(certificate);
      }
    }

    certificates = new Certificate[certificateList.size()];
    certificateList.toArray(certificates);

    return certificates;
  }
}
