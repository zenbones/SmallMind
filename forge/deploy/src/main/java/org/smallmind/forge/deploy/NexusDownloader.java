/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

public class NexusDownloader {

  public static void download (File file, String nexusHost, String nexusUser, String nexusPassword, Repository repository, String groupId, String artifactId, String version, String classifier, String extension, boolean progressBar)
    throws IOException {

    SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(SSLContexts.createSystemDefault());
    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", sslConnectionSocketFactory).build();
    HttpClientConnectionManager connectionManager = new BasicHttpClientConnectionManager(registry);
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    CloseableHttpClient httpclient;
    HttpHost target;
    StringBuilder getBuilder;

    credentialsProvider.setCredentials(new AuthScope(target = new HttpHost(nexusHost, 443, "https")), new UsernamePasswordCredentials(nexusUser, nexusPassword));
    httpclient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultCredentialsProvider(credentialsProvider).build();

    getBuilder = new StringBuilder("/repository/service/local/artifact/maven/redirect?r=").append(repository.getCode()).append("&g=").append(groupId).append("&a=").append(artifactId).append("&v=").append(calculateVersion(repository, version)).append("&e=").append(extension);
    if (classifier != null) {
      getBuilder.append("&c=").append(classifier);
    }

    try (CloseableHttpResponse response = httpclient.execute(target, new HttpGet(getBuilder.toString()))) {

      if (response.getStatusLine().getStatusCode() != 200) {
        throw new IOException("Could not locate requested artifact");
      }

      Files.createDirectories(file.toPath().getParent());

      long bytesAvailable = response.getEntity().getContentLength();
      byte[] buffer = new byte[2048];

      try (InputStream inputStream = response.getEntity().getContent(); FileOutputStream fileOutputStream = new FileOutputStream(file)) {

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
    }
  }

  private static String calculateVersion (Repository repository, String version) {

    if (version.equals("LATEST") || version.equals("RELEASE")) {

      return version;
    }

    return repository.equals(Repository.RELEASES) ? version : version + "-SNAPSHOT";
  }
}
