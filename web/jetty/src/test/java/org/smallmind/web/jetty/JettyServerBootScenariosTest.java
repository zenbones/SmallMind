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
package org.smallmind.web.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.smallmind.web.jetty.option.ClassLoaderResourceOption;
import org.smallmind.web.jetty.option.DocumentRootOption;
import org.smallmind.web.jetty.option.JaxRSOption;
import org.smallmind.web.jetty.option.SpringSupportOption;
import org.smallmind.web.jetty.option.WebApplicationOption;
import org.smallmind.web.jetty.option.WebSocketOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Integration coverage that boots an embedded Jetty server through real Spring {@code ContextRefreshedEvent}
 * lifecycle events, exercising the option and installer branches of {@link JettyInitializingBean}. Each scenario
 * boots its own freshly created {@link GenericApplicationContext} on an ephemeral port and shuts the server down
 * on context close.
 */
@Test(groups = "integration")
public class JettyServerBootScenariosTest {

  @Path("echo")
  public static class EchoResource {

    @GET
    public String echo () {

      return "jetty-up";
    }
  }

  public static class GreetingServlet extends HttpServlet {

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws IOException {

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write("servlet-greeting");
    }
  }

  public static class MarkerFilter implements Filter {

    private static final AtomicBoolean INVOKED = new AtomicBoolean(false);

    public static boolean wasInvoked () {

      return INVOKED.get();
    }

    public static void reset () {

      INVOKED.set(false);
    }

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      INVOKED.set(true);
      ((HttpServletResponse)response).addHeader("X-Marker-Filter", "seen");
      chain.doFilter(request, response);
    }
  }

  public static class StartupListener implements ServletContextListener {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static boolean wasInitialized () {

      return INITIALIZED.get();
    }

    public static void reset () {

      INITIALIZED.set(false);
    }

    @Override
    public void contextInitialized (ServletContextEvent servletContextEvent) {

      INITIALIZED.set(true);
    }

    @Override
    public void contextDestroyed (ServletContextEvent servletContextEvent) {

    }
  }

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  private static HttpResponse<String> get (String url)
    throws Exception {

    return HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create(url)).GET().build(),
      HttpResponse.BodyHandlers.ofString());
  }

  public void testMultipleContextPathsOnOneServer ()
    throws Exception {

    int port = freePort();

    StartupListener.reset();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      // One server hosting two distinct context paths: a JAX-RS context and a servlet-installer context.
      // The bean exposes only a single shared ResourceConfig, so a second JAX-RS context cannot reuse it;
      // mounting a plain servlet on the second context keeps the multi-context boot exercise honest.
      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption firstRest = new JaxRSOption();
        firstRest.setRestPath("/rest");

        WebApplicationOption firstOption = new WebApplicationOption();
        firstOption.setContextPath("/alpha");
        firstOption.setJaxRSOption(firstRest);

        WebApplicationOption secondOption = new WebApplicationOption();
        secondOption.setContextPath("/beta");

        bean.setWebApplicationOptions(new WebApplicationOption[] {firstOption, secondOption});

        return bean;
      });

      applicationContext.registerBean("betaServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/beta");
        servletInstaller.setDisplayName("beta-greeting");
        servletInstaller.setServlet(new GreetingServlet());
        servletInstaller.setUrlPattern("/greet");

        return servletInstaller;
      });

      applicationContext.refresh();

      HttpResponse<String> alphaResponse = get("http://127.0.0.1:" + port + "/alpha/rest/echo");
      HttpResponse<String> betaResponse = get("http://127.0.0.1:" + port + "/beta/greet");

      Assert.assertEquals(alphaResponse.statusCode(), 200);
      Assert.assertEquals(alphaResponse.body(), "jetty-up");
      Assert.assertEquals(betaResponse.statusCode(), 200);
      Assert.assertEquals(betaResponse.body(), "servlet-greeting");
    }
  }

  public void testSpringSupportOptionStillServes ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/spring");
        webApplicationOption.setJaxRSOption(jaxRSOption);
        // The presence of a SpringSupportOption registers a JettyRequestContextListener on the context.
        webApplicationOption.setSpringSupportOption(new SpringSupportOption());

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/spring/rest/echo");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "jetty-up");
    }
  }

  public void testServletFilterAndListenerInstallersRun ()
    throws Exception {

    int port = freePort();

    MarkerFilter.reset();
    StartupListener.reset();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      // The post-processor routes installer beans into the JettyInitializingBean's per-context state.
      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      // The JettyInitializingBean is registered first so it is seen as the locator before the installer
      // beans arrive, letting the post-processor route each installer into the seeded per-context state
      // immediately rather than relying on a later flush.
      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/install");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("greetingServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/install");
        servletInstaller.setDisplayName("greeting");
        servletInstaller.setServlet(new GreetingServlet());
        servletInstaller.setUrlPattern("/greet");

        return servletInstaller;
      });

      applicationContext.registerBean("markerFilter", FilterInstaller.class, () -> {

        FilterInstaller filterInstaller = new FilterInstaller();

        filterInstaller.setContextPath("/install");
        filterInstaller.setDisplayName("marker");
        filterInstaller.setFilter(new MarkerFilter());
        filterInstaller.setUrlPattern("/*");

        return filterInstaller;
      });

      applicationContext.registerBean("startupListener", ListenerInstaller.class, () -> {

        ListenerInstaller listenerInstaller = new ListenerInstaller();

        listenerInstaller.setContextPath("/install");
        listenerInstaller.setEventListener(new StartupListener());

        return listenerInstaller;
      });

      applicationContext.refresh();

      // The listener fires during server start when the context handler initializes.
      Assert.assertTrue(StartupListener.wasInitialized(), "context listener should run at startup");

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/install/greet");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "servlet-greeting");
      Assert.assertTrue(MarkerFilter.wasInvoked(), "filter should be invoked on the matched request");
      Assert.assertEquals(response.headers().firstValue("X-Marker-Filter").orElse(null), "seen");
    }
  }

  public void testDocumentRootOptionServesStaticFile ()
    throws Exception {

    int port = freePort();
    java.nio.file.Path documentRoot = Files.createTempDirectory("jetty-doc-root");
    java.nio.file.Path staticFile = documentRoot.resolve("hello.txt");

    Files.write(staticFile, "static-content".getBytes(StandardCharsets.UTF_8));

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        Map<String, String> documentRoots = new HashMap<>();
        documentRoots.put("/files", documentRoot.toAbsolutePath().toString());

        DocumentRootOption documentRootOption = new DocumentRootOption();
        documentRootOption.setDocumentPath("/document");
        documentRootOption.setDocumentRoots(documentRoots);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/docs");
        webApplicationOption.setDocumentRootOption(documentRootOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      // The document ContextHandler is mounted at contextPath + documentPath + key (/docs/document/files) and
      // serves the document root through the configured ResourceHandler.
      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/docs/document/files/hello.txt");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "static-content");
    }
  }

  public void testInsecureDisabledWithoutSslFailsToBoot ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        // No SSL info and insecure disabled is a contradictory configuration that must fail fast.
        bean.setAllowInsecure(false);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/none");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testDuplicateContextPathsRejectedDuringAfterPropertiesSet ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption firstOption = new WebApplicationOption();
        firstOption.setContextPath("/dup");

        WebApplicationOption secondOption = new WebApplicationOption();
        secondOption.setContextPath("/dup");

        bean.setWebApplicationOptions(new WebApplicationOption[] {firstOption, secondOption});

        return bean;
      });

      // afterPropertiesSet runs during bean initialization and rejects duplicate context paths.
      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testWorkerPoolAndHeaderTuningBoots ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));
        // Exercise the optional thread-pool sizing, header-size, and debug branches in onApplicationEvent.
        bean.setInitialWorkerPoolSize(8);
        bean.setMaximumWorkerPoolSize(32);
        bean.setMaxHttpHeaderSize(16384);
        bean.setDebug(true);

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/tuned");
        webApplicationOption.setJaxRSOption(jaxRSOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/tuned/rest/echo");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "jetty-up");
    }
  }

  public void testClassLoaderResourceOptionServesStaticResource ()
    throws Exception {

    int port = freePort();
    String stagedName = stageClasspathRootFile("classloader-static-probe.txt", "classloader-static-content");

    if (stagedName == null) {
      throw new SkipException("Could not stage a static resource on the test classpath; skipping classloader resource scenario.");
    }

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        ClassLoaderResourceOption classLoaderResourceOption = new ClassLoaderResourceOption();
        classLoaderResourceOption.setStaticPath("/static");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/assets");
        // The classpath resource handler is rooted at "/" of the test classpath, so a file staged at the
        // classpath root is reachable beneath the contextPath + staticPath mount.
        webApplicationOption.setClassLoaderResourceOption(classLoaderResourceOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      // The static ContextHandler is mounted at contextPath + staticPath (/assets/static) and serves classpath
      // resources; a known test-classpath resource confirms the ResourceHandler branch is wired and reachable.
      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/assets/static/" + stagedName);

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body().trim(), "classloader-static-content");
    }
  }

  public void testWebSocketOptionContextBoots ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebSocketOption webSocketOption = new WebSocketOption();
        webSocketOption.setMaxSessionIdleTimeout(60000);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/ws");
        webApplicationOption.setJaxRSOption(jaxRSOption);
        // Enabling the WebSocketOption runs the Jakarta WebSocket container initializer on this context.
        webApplicationOption.setWebSocketOption(webSocketOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      // The WebSocket container initializer should not disturb ordinary JAX-RS serving on the same context.
      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/ws/rest/echo");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "jetty-up");
    }
  }

  public void testMissingKeystoreResourceFailsToBoot ()
    throws Exception {

    int httpsPort = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        SSLStore keyStore = new SSLStore();
        // A classpath resource that does not exist drives the keystore-read failure branch in onApplicationEvent.
        keyStore.setResource("classpath:/does-not-exist-keystore.jks");
        keyStore.setPassword("changeit");

        SSLStore trustStore = new SSLStore();
        trustStore.setResource("classpath:/does-not-exist-keystore.jks");
        trustStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySSLStore(keyStore);
        sslInfo.setTrustSSLStore(trustStore);

        bean.setHost("127.0.0.1");
        bean.setSslInfo(sslInfo);
        bean.setAllowInsecure(false);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/bad-ssl");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      // The unreadable keystore resource is wrapped as a JettyInitializationException during refresh.
      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testDualConnectorServesOverHttpAndHttps ()
    throws Exception {

    java.nio.file.Path keyStoreFile = generateSelfSignedKeyStore();

    if (keyStoreFile == null) {
      throw new SkipException("Could not generate a self-signed keystore via keytool; skipping dual-connector scenario.");
    }

    String keyStoreResource = classpathResourceFor(keyStoreFile);

    if (keyStoreResource == null) {
      throw new SkipException("Could not stage the generated keystore on the test classpath; skipping dual-connector scenario.");
    }

    int httpPort = freePort();
    int httpsPort = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        SSLStore keyStore = new SSLStore();
        keyStore.setResource(keyStoreResource);
        keyStore.setPassword("changeit");

        SSLStore trustStore = new SSLStore();
        trustStore.setResource(keyStoreResource);
        trustStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySSLStore(keyStore);
        sslInfo.setTrustSSLStore(trustStore);

        bean.setHost("127.0.0.1");
        bean.setPort(httpPort);
        bean.setSslInfo(sslInfo);
        // Both an SSL connector and an insecure connector are wired on the same boot.
        bean.setAllowInsecure(true);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/dual");
        webApplicationOption.setJaxRSOption(jaxRSOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> insecureResponse = get("http://127.0.0.1:" + httpPort + "/dual/rest/echo");

      Assert.assertEquals(insecureResponse.statusCode(), 200);
      Assert.assertEquals(insecureResponse.body(), "jetty-up");

      HttpClient httpsClient = trustAllHttpsClient();
      HttpResponse<String> secureResponse = httpsClient.send(
        HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + httpsPort + "/dual/rest/echo")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(secureResponse.statusCode(), 200);
      Assert.assertEquals(secureResponse.body(), "jetty-up");
    }
  }

  public void testHttpsBootWithSelfSignedKeystore ()
    throws Exception {

    java.nio.file.Path keyStoreFile = generateSelfSignedKeyStore();

    if (keyStoreFile == null) {
      throw new SkipException("Could not generate a self-signed keystore via keytool; skipping HTTPS boot scenario.");
    }

    int httpsPort = freePort();
    String keyStoreResource = classpathResourceFor(keyStoreFile);

    if (keyStoreResource == null) {
      throw new SkipException("Could not stage the generated keystore on the test classpath; skipping HTTPS boot scenario.");
    }

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        SSLStore keyStore = new SSLStore();
        keyStore.setResource(keyStoreResource);
        keyStore.setPassword("changeit");

        SSLStore trustStore = new SSLStore();
        trustStore.setResource(keyStoreResource);
        trustStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySSLStore(keyStore);
        sslInfo.setTrustSSLStore(trustStore);

        bean.setHost("127.0.0.1");
        bean.setSslInfo(sslInfo);
        // Only the HTTPS connector is wired here, exercising the SSL connector branch in isolation.
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

      HttpClient httpsClient = trustAllHttpsClient();
      HttpResponse<String> response = httpsClient.send(
        HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + httpsPort + "/secure/rest/echo")).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "jetty-up");
    }
  }

  public void testRequireClientAuthEnforcesMutualTls ()
    throws Exception {

    java.nio.file.Path keyStoreFile = generateSelfSignedKeyStore();

    if (keyStoreFile == null) {
      throw new SkipException("Could not generate a self-signed keystore via keytool; skipping mutual TLS scenario.");
    }

    int httpsPort = freePort();
    String keyStoreResource = classpathResourceFor(keyStoreFile);

    if (keyStoreResource == null) {
      throw new SkipException("Could not stage the generated keystore on the test classpath; skipping mutual TLS scenario.");
    }

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        SSLStore keyStore = new SSLStore();
        keyStore.setResource(keyStoreResource);
        keyStore.setPassword("changeit");

        SSLStore trustStore = new SSLStore();
        trustStore.setResource(keyStoreResource);
        trustStore.setPassword("changeit");

        SSLInfo sslInfo = new SSLInfo();
        sslInfo.setPort(httpsPort);
        sslInfo.setKeySSLStore(keyStore);
        sslInfo.setTrustSSLStore(trustStore);
        // requireClientAuth must drive setNeedClientAuth so the server demands a client certificate at the handshake.
        sslInfo.setRequireClientAuth(true);

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

      URI secureUri = URI.create("https://127.0.0.1:" + httpsPort + "/secure/rest/echo");

      // A client presenting no certificate is rejected at the TLS handshake when mutual TLS is required.
      HttpClient noCertificateClient = trustAllHttpsClient();

      Assert.assertThrows(Exception.class, () ->
        noCertificateClient.send(HttpRequest.newBuilder(secureUri).GET().build(), HttpResponse.BodyHandlers.ofString()));

      // A client presenting the trusted certificate completes the handshake and is served.
      HttpResponse<String> response = mutualTlsClient(keyStoreFile).send(
        HttpRequest.newBuilder(secureUri).GET().build(),
        HttpResponse.BodyHandlers.ofString());

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "jetty-up");
    }
  }

  private static java.nio.file.Path generateSelfSignedKeyStore ()
    throws Exception {

    java.nio.file.Path keyStoreFile = Files.createTempFile("jetty-test-keystore", ".jks");
    String javaHome = System.getProperty("java.home");
    String keytool = javaHome + "/bin/keytool";

    Files.deleteIfExists(keyStoreFile);

    ProcessBuilder processBuilder = new ProcessBuilder(
      keytool,
      "-genkeypair",
      "-alias", "jetty-test",
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

      return keyStoreFile;
    } catch (IOException ioException) {

      return null;
    }
  }

  private static String classpathResourceFor (java.nio.file.Path keyStoreFile)
    throws Exception {

    URI rootUri = JettyServerBootScenariosTest.class.getResource("/").toURI();

    if (!"file".equals(rootUri.getScheme())) {

      return null;
    }

    java.nio.file.Path classpathRoot = java.nio.file.Paths.get(rootUri);
    String stagedName = "jetty-https-test-keystore.jks";
    java.nio.file.Path stagedFile = classpathRoot.resolve(stagedName);

    Files.copy(keyStoreFile, stagedFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

    return "classpath:/" + stagedName;
  }

  private static String stageClasspathRootFile (String name, String content)
    throws Exception {

    URI rootUri = JettyServerBootScenariosTest.class.getResource("/").toURI();

    if (!"file".equals(rootUri.getScheme())) {

      return null;
    }

    java.nio.file.Path classpathRoot = java.nio.file.Paths.get(rootUri);
    java.nio.file.Path stagedFile = classpathRoot.resolve(name);

    Files.write(stagedFile, content.getBytes(StandardCharsets.UTF_8));

    return name;
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

  private static HttpClient mutualTlsClient (java.nio.file.Path keyStoreFile)
    throws Exception {

    KeyStore keyStore = KeyStore.getInstance("JKS");

    try (InputStream inputStream = Files.newInputStream(keyStoreFile)) {
      keyStore.load(inputStream, "changeit".toCharArray());
    }

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

    keyManagerFactory.init(keyStore, "changeit".toCharArray());

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

    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());

    return HttpClient.newBuilder().sslContext(sslContext).build();
  }
}
