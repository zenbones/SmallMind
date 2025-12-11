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

public class ClassVerifier {

  // false if not signed, true if signed and verified, otherwise an exception is thrown
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
