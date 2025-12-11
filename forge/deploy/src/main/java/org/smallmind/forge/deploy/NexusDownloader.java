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
package org.smallmind.forge.deploy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.ssl.SSLContexts;

public class NexusDownloader {

  public static void download (Path filePath, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, boolean progressBar)
    throws IOException {

    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createSystemDefault());
    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslConnectionSocketFactory).build();
    HttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry);
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    HttpHost target;
    StringBuilder getBuilder;

    credentialsProvider.setCredentials(new AuthScope(target = new HttpHost("https", nexusHost, 443)), new UsernamePasswordCredentials(nexusUser, nexusPassword.toCharArray()));
    getBuilder = new StringBuilder("/repository/service/local/artifact/maven/redirect?r=").append(repository.getCode()).append("&g=").append(groupId).append("&a=").append(artifactId).append("&v=").append(calculateVersion(repository, version)).append("&e=").append(extension);
    if (classifier != null) {
      getBuilder.append("&c=").append(classifier);
    }

    try (CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultCredentialsProvider(credentialsProvider).build()) {
      httpclient.execute(target, new HttpGet(getBuilder.toString()), (HttpClientResponseHandler<Void>)response -> {

        if (response.getCode() != 200) {
          throw new IOException("Could not locate requested artifact");
        }

        Files.createDirectories(filePath.getParent());

        long bytesAvailable = response.getEntity().getContentLength();
        byte[] buffer = new byte[2048];

        try (InputStream inputStream = response.getEntity().getContent(); OutputStream fileOutputStream = Files.newOutputStream(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

          TextProgressBar downloadProgressBar;
          long bytesWritten = 0;
          int bytesRead;

          downloadProgressBar = new TextProgressBar(bytesAvailable, "bytes", 2, progressBar);
          downloadProgressBar.update(0);

          do {
            if ((bytesRead = inputStream.read(buffer)) < 0) {
              throw new IOException("Unexpected end of stream");
            } else {
              fileOutputStream.write(buffer, 0, bytesRead);
              bytesWritten += bytesRead;
              downloadProgressBar.update(bytesWritten);
            }
          } while (bytesWritten < bytesAvailable);

          downloadProgressBar.update(bytesAvailable);
        }

        return null;
      });
    }
  }

  private static String calculateVersion (Repository repository, String version) {

    if (version.equals("LATEST") || version.equals("RELEASE")) {

      return version;
    }

    return repository.equals(Repository.RELEASES) ? version : version + "-SNAPSHOT";
  }
}
