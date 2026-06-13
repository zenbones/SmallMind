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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ListenerInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.option.DocumentRootOption;
import org.smallmind.web.grizzly.option.JaxRSOption;
import org.smallmind.web.grizzly.option.SpringSupportOption;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration scenarios that boot a fresh embedded Grizzly server per test on an ephemeral port, each exercising a
 * distinct {@link GrizzlyInitializingBean} configuration branch (multiple contexts, document root, Spring support, and
 * servlet/filter/listener installers) and verifying the deployed surface responds.
 */
@Test(groups = "integration")
public class GrizzlyServerBootScenariosTest {

  @Path("echo")
  public static class EchoResource {

    @GET
    public String echo () {

      return "grizzly-up";
    }
  }

  public static class RecordingServlet extends GenericServlet {

    static final AtomicBoolean SERVICED = new AtomicBoolean(false);

    @Override
    public void service (ServletRequest request, ServletResponse response)
      throws IOException {

      SERVICED.set(true);
      response.setContentType("text/plain");
      response.getWriter().write("servlet-ok");
      response.getWriter().flush();
    }
  }

  public static class RecordingFilter implements Filter {

    static final AtomicBoolean FILTERED = new AtomicBoolean(false);

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      FILTERED.set(true);
      chain.doFilter(request, response);
    }
  }

  public static class RecordingListener implements ServletContextListener {

    static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void contextInitialized (ServletContextEvent event) {

      INITIALIZED.set(true);
    }
  }

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  private static HttpResponse<String> get (int port, String path)
    throws Exception {

    return HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
      HttpResponse.BodyHandlers.ofString());
  }

  public void testMultipleContextPathsOnOneServer ()
    throws Exception {

    int port = freePort();
    java.nio.file.Path documentRoot = Files.createTempDirectory("grizzly-multi-docroot");
    java.nio.file.Path staticFile = documentRoot.resolve("file.txt");

    Files.writeString(staticFile, "second-content");
    // Grizzly's StaticHttpHandler keeps the file open on Windows until the server stops, so defer cleanup to JVM exit
    // rather than deleting while the still-running server holds the handle.
    documentRoot.toFile().deleteOnExit();
    staticFile.toFile().deleteOnExit();

    // The bean carries a single shared ResourceConfig, which Jersey locks after the first servlet initializes, so two
    // JAX-RS contexts cannot share it. This scenario instead deploys one JAX-RS context alongside one document-root
    // context to prove multiple WebApplicationOption context paths boot together on a single server.
    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption firstJaxRSOption = new JaxRSOption();
        firstJaxRSOption.setRestPath("/rest");

        WebApplicationOption firstOption = new WebApplicationOption();
        firstOption.setContextPath("/first");
        firstOption.setJaxRSOption(firstJaxRSOption);

        Map<String, String> documentRoots = new HashMap<>();
        documentRoots.put("/static", documentRoot.toAbsolutePath().toString());

        DocumentRootOption documentRootOption = new DocumentRootOption();
        documentRootOption.setDocumentRoots(documentRoots);

        WebApplicationOption secondOption = new WebApplicationOption();
        secondOption.setContextPath("/second");
        secondOption.setDocumentRootOption(documentRootOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {firstOption, secondOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> firstResponse = get(port, "/first/rest/echo");
      // The document-root handler serves at contextPath + documentPath + key (documentPath defaults to /document).
      HttpResponse<String> secondResponse = get(port, "/second/document/static/file.txt");

      Assert.assertEquals(firstResponse.statusCode(), 200);
      Assert.assertEquals(firstResponse.body(), "grizzly-up");
      Assert.assertEquals(secondResponse.statusCode(), 200);
      Assert.assertEquals(secondResponse.body(), "second-content");
    }
  }

  public void testDocumentRootServesStaticFile ()
    throws Exception {

    int port = freePort();
    java.nio.file.Path documentRoot = Files.createTempDirectory("grizzly-docroot");
    java.nio.file.Path staticFile = documentRoot.resolve("hello.txt");

    Files.writeString(staticFile, "static-content");
    // Grizzly's StaticHttpHandler keeps the file open on Windows until the server stops, so defer cleanup to JVM exit
    // rather than deleting while the still-running server holds the handle.
    documentRoot.toFile().deleteOnExit();
    staticFile.toFile().deleteOnExit();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);

        Map<String, String> documentRoots = new HashMap<>();
        documentRoots.put("/files", documentRoot.toAbsolutePath().toString());

        DocumentRootOption documentRootOption = new DocumentRootOption();
        documentRootOption.setDocumentPath("/document");
        documentRootOption.setDocumentRoots(documentRoots);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/app");
        webApplicationOption.setDocumentRootOption(documentRootOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      // The document root is served beneath the full configured prefix contextPath + documentPath + key
      // (/app/document/files) and is not reachable at the bare root path.
      HttpResponse<String> mappedResponse = get(port, "/app/document/files/hello.txt");
      HttpResponse<String> rootResponse = get(port, "/hello.txt");

      Assert.assertEquals(mappedResponse.statusCode(), 200);
      Assert.assertEquals(mappedResponse.body(), "static-content");
      Assert.assertEquals(rootResponse.statusCode(), 404);
    }
  }

  public void testSpringSupportListenerRegisteredStillServes ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setResourceConfig(new ResourceConfig().register(EchoResource.class));

        JaxRSOption jaxRSOption = new JaxRSOption();
        jaxRSOption.setRestPath("/rest");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/app");
        webApplicationOption.setJaxRSOption(jaxRSOption);
        webApplicationOption.setSpringSupportOption(new SpringSupportOption());

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get(port, "/app/rest/echo");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "grizzly-up");
    }
  }

  public void testServletFilterAndListenerInstallersRun ()
    throws Exception {

    int port = freePort();

    RecordingServlet.SERVICED.set(false);
    RecordingFilter.FILTERED.set(false);
    RecordingListener.INITIALIZED.set(false);

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      // The post processor routes installer beans to the locator's web application state.
      applicationContext.registerBean("grizzlyPostProcessor", GrizzlyPostProcessor.class, GrizzlyPostProcessor::new);

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/app");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("recordingListener", ListenerInstaller.class, () -> {

        ListenerInstaller listenerInstaller = new ListenerInstaller();

        listenerInstaller.setContextPath("/app");
        listenerInstaller.setEventListener(new RecordingListener());

        Map<String, String> contextParameters = new HashMap<>();
        contextParameters.put("recording", "true");
        listenerInstaller.setContextParameters(contextParameters);

        return listenerInstaller;
      });

      applicationContext.registerBean("recordingFilter", FilterInstaller.class, () -> {

        FilterInstaller filterInstaller = new FilterInstaller();

        filterInstaller.setContextPath("/app");
        filterInstaller.setDisplayName("recordingFilter");
        filterInstaller.setUrlPattern("/probe/*");
        filterInstaller.setFilter(new RecordingFilter());

        return filterInstaller;
      });

      applicationContext.registerBean("recordingServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/app");
        servletInstaller.setDisplayName("recordingServlet");
        servletInstaller.setUrlPattern("/probe/*");
        servletInstaller.setLoadOnStartup(1);
        servletInstaller.setServlet(new RecordingServlet());

        return servletInstaller;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get(port, "/app/probe/run");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "servlet-ok");
      Assert.assertTrue(RecordingListener.INITIALIZED.get(), "listener contextInitialized should have run");
      Assert.assertTrue(RecordingFilter.FILTERED.get(), "filter doFilter should have run");
      Assert.assertTrue(RecordingServlet.SERVICED.get(), "servlet service should have run");
    }
  }
}
