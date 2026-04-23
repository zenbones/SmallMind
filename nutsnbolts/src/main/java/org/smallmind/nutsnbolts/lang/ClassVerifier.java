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
package org.smallmind.nutsnbolts.lang;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.CodeSource;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import org.smallmind.nutsnbolts.resource.ResourceException;

/**
 * Utility class for verifying that a loaded class is signed with a trusted certificate from a given keystore.
 */
public class ClassVerifier {

  // false if not signed, true if signed and verified, otherwise an exception is thrown

  /**
   * Checks whether the supplied class is signed and that its certificate verifies against the identified keystore entry.
   * Returns {@code true} when the class is signed and the signature is valid, {@code false} when the class carries no certificates, and throws an exception when the certificate exists but cannot be verified.
   *
   * @param clazz        the class whose signature is to be verified
   * @param secureStore  the keystore material (bytes and password) used to load the trusted certificate
   * @param keyStoreType the keystore type (e.g. {@code "JKS"}), or {@code null} to use the platform default
   * @param alias        the alias identifying the certificate entry in the keystore
   * @return {@code true} if the class is signed and the signature verifies; {@code false} if the class is unsigned
   * @throws GeneralSecurityException   if the certificate verification fails
   * @throws IOException                if the keystore bytes cannot be read
   * @throws ResourceException          if keystore bytes cannot be retrieved from the {@link SecureStore}
   * @throws ClassVerificationException if the specified alias does not exist in the keystore
   */
  public static boolean verifySignature (Class<?> clazz, SecureStore secureStore, String keyStoreType, String alias)
    throws GeneralSecurityException, IOException, ResourceException, ClassVerificationException {

    ProtectionDomain protectionDomain;

    if ((protectionDomain = clazz.getProtectionDomain()) != null) {

      CodeSource codeSource;

      if ((codeSource = protectionDomain.getCodeSource()) != null) {

        Certificate[] certificates;

        if ((certificates = codeSource.getCertificates()) != null) {

          KeyStore keyStore = KeyStore.getInstance((keyStoreType != null) ? keyStoreType : KeyStore.getDefaultType());
          Certificate publicCertificate;

          keyStore.load(new ByteArrayInputStream(secureStore.getBytes()), secureStore.getPassword().toCharArray());

          if ((publicCertificate = keyStore.getCertificate(alias)) == null) {
            throw new ClassVerificationException("Incorrect alias(%s) provided for class verification");
          } else {
            certificates[0].verify(publicCertificate.getPublicKey());

            return true;
          }
        }
      }
    }

    return false;
  }
}
