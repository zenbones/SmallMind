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
package org.smallmind.web.grizzly;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.grizzly.http2.Http2AddOn;
import org.glassfish.grizzly.http2.Http2Configuration;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.nutsnbolts.lang.SecureStore;
import org.smallmind.web.grizzly.option.JaxRSOption;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Integration scenarios that boot a fresh embedded Grizzly server per test on an ephemeral port, exercising the SSL,
 * HTTP/2, and fail-fast configuration branches of {@link GrizzlyInitializingBean} that the plain HTTP boot tests do not
 * reach. Each scenario uses a self-signed keystore staged on the test classpath and a trust-all client to drive the
 * HTTPS listener, including the {@link ClientAuthProxyFilter} that the bean adds to the secure filter chain.
 */
@Test(groups = "integration")
public class GrizzlyInitializingBeanSSLBootTest {

  @Path("echo")
  public static class EchoResource {

    @GET
    public String echo () {

      return "grizzly-up";
    }
  }

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  public void testHttpsBootWithSelfSignedKeystore ()
    throws Exception {

    String keyStoreResource = stageSelfSignedKeyStore();

    if (keyStoreResource == null) {
      throw new SkipException("Could not generate or stage a self-signed keystore via keytool; skipping HTTPS boot scenario.");
    }

    int httpsPort = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        SecureStore keySecureStore = new SecureStore();
        keySecureStore.setResource(keyStoreResource);
        keySecureStore.setPassword("changeit");

        SecureStore trustSecureStore = new SecureStore();
        trustSecureStore.setResource(keyStoreResource);
        trustSecureStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySecureStore(keySecureStore);
        sslInfo.setTrustSecureStore(trustSecureStore);

        bean.setHost("127.0.0.1");
        bean.setSslInfo(sslInfo);
        // Only the HTTPS listener is wired here, exercising the secure-listener branch and the ClientAuthProxyFilter
        // installation that follows server start, in isolation from the plain HTTP listener.
        bean.setAllowInsecure(false);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/secure");
        webApplicationOption.setJaxRSOption(jaxRSOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = trustAllHttpsClient().send(
        HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + httpsPort + "/secure/rest/echo")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "grizzly-up");
    }
  }

  public void testHttpsBootWithProxyModeClientAuthFilter ()
    throws Exception {

    String keyStoreResource = stageSelfSignedKeyStore();

    if (keyStoreResource == null) {
      throw new SkipException("Could not generate or stage a self-signed keystore via keytool; skipping HTTPS proxy-mode boot scenario.");
    }

    int httpsPort = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        SecureStore keySecureStore = new SecureStore();
        keySecureStore.setResource(keyStoreResource);
        keySecureStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySecureStore(keySecureStore);
        // Proxy mode flows through to the ClientAuthProxyFilter constructor on the secure filter chain, exercising the
        // pass-through branch of the filter wiring.
        sslInfo.setProxyMode(true);

        bean.setHost("127.0.0.1");
        bean.setSslInfo(sslInfo);
        bean.setAllowInsecure(false);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/secure");
        webApplicationOption.setJaxRSOption(jaxRSOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = trustAllHttpsClient().send(
        HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + httpsPort + "/secure/rest/echo")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "grizzly-up");
    }
  }

  public void testInsecureDisabledWithoutSslInfoFailsFast ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        // Disallowing insecure connections with no SSL info supplied is a contradictory configuration that must fail
        // fast during the context-refreshed event.
        bean.setAllowInsecure(false);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/none");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testExplicitHttp2AddOnBootServesResource ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));
        // An explicit Http2AddOn supplied through the addOns array exercises the add-on registration loop in addition
        // to the always-registered HTTP/2 add-on.
        bean.setAddOns(new org.glassfish.grizzly.http.server.AddOn[] {new Http2AddOn(Http2Configuration.builder().build())});

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/h2");
        webApplicationOption.setJaxRSOption(jaxRSOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      // The JDK client negotiates HTTP/1.1 over cleartext here; the assertion confirms the server still serves while
      // the HTTP/2 add-ons are registered on the listener.
      HttpResponse<String> response = HttpClient.newHttpClient().send(
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/h2/rest/echo")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "grizzly-up");
    }
  }

  private static String stageSelfSignedKeyStore ()
    throws Exception {

    java.nio.file.Path keyStoreFile = Files.createTempFile("grizzly-test-keystore", ".jks");
    String javaHome = System.getProperty("java.home");
    String keytool = javaHome + "/bin/keytool";

    Files.deleteIfExists(keyStoreFile);

    ProcessBuilder processBuilder = new ProcessBuilder(
      keytool,
      "-genkeypair",
      "-alias", "grizzly-test",
      "-keyalg", "RSA",
      "-keysize", "2048",
      "-validity", "1",
      "-dname", "CN=localhost, OU=test, O=test, L=test, ST=test, C=US",
      "-ext", "san=dns:localhost,ip:127.0.0.1",
      "-keystore", keyStoreFile.toAbsolutePath().toString(),
      "-storepass", "changeit",
      "-keypass", "changeit",
      "-storetype", "JKS");

    processBuilder.redirectErrorStream(true);

    try {

      Process process = processBuilder.start();
      int exitCode = process.waitFor();

      if ((exitCode != 0) || (!Files.exists(keyStoreFile)) || (Files.size(keyStoreFile) == 0)) {
        Files.deleteIfExists(keyStoreFile);

        return null;
      }
    } catch (IOException ioException) {

      return null;
    }

    URI rootUri = GrizzlyInitializingBeanSSLBootTest.class.getResource("/").toURI();

    if (!"file".equals(rootUri.getScheme())) {

      return null;
    }

    java.nio.file.Path classpathRoot = java.nio.file.Paths.get(rootUri);
    String stagedName = "grizzly-https-test-keystore.jks";
    java.nio.file.Path stagedFile = classpathRoot.resolve(stagedName);

    Files.copy(keyStoreFile, stagedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    Files.deleteIfExists(keyStoreFile);

    return "classpath:/" + stagedName;
  }

  private static HttpClient trustAllHttpsClient ()
    throws Exception {

    TrustManager[] trustManagers = new TrustManager[] {
      new X509TrustManager() {

        @Override
        public void checkClientTrusted (X509Certificate[] chain, String authType) {

        }

        @Override
        public void checkServerTrusted (X509Certificate[] chain, String authType) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers () {

          return new X509Certificate[0];
        }
      }
    };

    SSLContext sslContext = SSLContext.getInstance("TLS");

    sslContext.init(null, trustManagers, new SecureRandom());

    return HttpClient.newBuilder().sslContext(sslContext).build();
  }
}
