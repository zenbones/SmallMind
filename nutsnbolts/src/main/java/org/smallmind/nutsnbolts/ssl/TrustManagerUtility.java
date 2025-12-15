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
package org.smallmind.nutsnbolts.ssl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.smallmind.nutsnbolts.resource.Resource;
import org.smallmind.nutsnbolts.resource.ResourceException;

/**
 * Helper utilities for constructing {@link TrustManager} arrays from certificate resources.
 * Supports loading a single certificate into an in-memory keystore for use in SSL contexts.
 */
public class TrustManagerUtility {

  /**
   * Loads a certificate resource into a temporary keystore and returns the initialized trust managers.
   *
   * @param alias        the alias under which to store the certificate
   * @param certResource the resource supplying the X509 certificate data
   * @return an array of trust managers ready for SSL context initialization
   * @throws IOException              if the certificate data cannot be read
   * @throws ResourceException        if the resource cannot be opened
   * @throws CertificateException     if the certificate cannot be parsed
   * @throws KeyStoreException        if the keystore cannot be created or populated
   * @throws NoSuchAlgorithmException if the default trust manager algorithm is unavailable
   */
  public static TrustManager[] load (String alias, Resource certResource)
    throws IOException, ResourceException, CertificateException, KeyStoreException, NoSuchAlgorithmException {

    try (InputStream inputStream = certResource.getInputStream()) {

      TrustManagerFactory trustManagerFactory;
      X509Certificate certificate = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new BufferedInputStream(inputStream));
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

      keyStore.load(null, null);
      keyStore.setCertificateEntry(alias, certificate);

      trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);

      return trustManagerFactory.getTrustManagers();
    }
  }
}
