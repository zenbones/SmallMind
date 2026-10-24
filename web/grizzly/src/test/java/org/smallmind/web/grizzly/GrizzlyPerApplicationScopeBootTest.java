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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.option.JaxRSOption;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Boots an embedded Grizzly server with and without a {@link PerApplicationScope} configured on the
 * {@link GrizzlyInitializingBean}, and verifies through a JAX-RS probe, a plain installer-declared servlet, and an
 * {@code AsyncContext.dispatch} leg which per-application binding the request thread sees.
 */
@Test(groups = "integration")
public class GrizzlyPerApplicationScopeBootTest {

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

  private static GrizzlyInitializingBean probeServer (int port, PerApplicationScope perApplicationScope) {

    GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

    bean.setHost("127.0.0.1");
    bean.setPort(port);
    bean.setResourceConfig(new ResourceConfig().register(ProbeResource.class));
    bean.setPerApplicationScope(perApplicationScope);

    JaxRSOption jaxRSOption = new JaxRSOption();
    jaxRSOption.setRestPath("/rest");

    WebApplicationOption webApplicationOption = new WebApplicationOption();
    webApplicationOption.setContextPath("/probe");
    webApplicationOption.setJaxRSOption(jaxRSOption);

    bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

    return bean;
  }

  private static ServletInstaller servletInstaller (String displayName, String urlPattern, HttpServlet servlet) {

    ServletInstaller servletInstaller = new ServletInstaller();

    servletInstaller.setContextPath("/probe");
    servletInstaller.setDisplayName(displayName);
    servletInstaller.setUrlPattern(urlPattern);
    servletInstaller.setServlet(servlet);
    servletInstaller.setAsyncSupported(true);

    return servletInstaller;
  }

  // The post-processor must be registered before the installers and the server so it sees them all.
  private static void registerProbeBeans (GenericApplicationContext applicationContext, int port, PerApplicationScope perApplicationScope) {

    applicationContext.registerBean("grizzlyPostProcessor", GrizzlyPostProcessor.class, GrizzlyPostProcessor::new);
    applicationContext.registerBean("plainServlet", ServletInstaller.class, () -> servletInstaller("plain", "/plain/*", new ScopeProbeServlet()));
    applicationContext.registerBean("asyncServlet", ServletInstaller.class, () -> servletInstaller("async", "/async/*", new AsyncDispatchingServlet()));
    applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> probeServer(port, perApplicationScope));
  }

  public void testScopeFilterBindsRequestThreadWhenScopeConfigured ()
    throws Exception {

    int port = freePort();
    PerApplicationScope perApplicationScope = new PerApplicationScope();

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      registerProbeBeans(applicationContext, port, perApplicationScope);
      applicationContext.refresh();

      HttpResponse<String> restResponse = get(port, "/probe/rest/probe/scope");
      HttpResponse<String> plainResponse = get(port, "/probe/plain/scope");
      HttpResponse<String> asyncResponse = get(port, "/probe/async/scope");

      Assert.assertEquals(restResponse.statusCode(), 200);
      Assert.assertEquals(restResponse.body(), "scope:scoped");
      Assert.assertEquals(plainResponse.statusCode(), 200);
      Assert.assertEquals(plainResponse.body(), "scope:scoped");
      Assert.assertEquals(asyncResponse.statusCode(), 200);
      Assert.assertEquals(asyncResponse.body(), "scope:scoped");
    }
  }

  public void testDefaultKeepsPerApplicationContextFilter ()
    throws Exception {

    int port = freePort();

    // The default filter adopts the map already on the refresh thread, which is this thread.
    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(SampleManager.class, "thread-local");

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      registerProbeBeans(applicationContext, port, null);
      applicationContext.refresh();

      HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
      HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
      HttpResponse<String> plainResponse = get(port, "/probe/plain/scope");
      HttpResponse<String> asyncResponse = get(port, "/probe/async/scope");

      Assert.assertEquals(scopeResponse.statusCode(), 200);
      Assert.assertEquals(scopeResponse.body(), "scope:unbound");
      Assert.assertEquals(contextResponse.statusCode(), 200);
      Assert.assertEquals(contextResponse.body(), "context:thread-local");
      Assert.assertEquals(plainResponse.statusCode(), 200);
      Assert.assertEquals(plainResponse.body(), "scope:unbound");
      Assert.assertEquals(asyncResponse.statusCode(), 200);
      Assert.assertEquals(asyncResponse.body(), "scope:unbound");
    }
  }

  private static String scopeProbe () {

    try {

      return "scope:" + PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {

      return "scope:unbound";
    }
  }

  public static class ScopeProbeServlet extends HttpServlet {

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws IOException {

      response.setContentType("text/plain");
      response.getWriter().write(scopeProbe());
    }
  }

  // Suspends the request and dispatches it to the plain probe, so the probe runs on the ASYNC dispatch leg.
  public static class AsyncDispatchingServlet extends HttpServlet {

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response) {

      request.startAsync().dispatch("/plain/scope");
    }
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  @Path("/probe")
  public static class ProbeResource {

    @GET
    @Path("/scope")
    public String scope () {

      try {

        return "scope:" + PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {

        return "scope:unbound";
      }
    }

    @GET
    @Path("/context")
    public String context () {

      try {

        return "context:" + PerApplicationContext.getPerApplicationData(SampleManager.class, String.class);
      } catch (MissingPerApplicationContextException missingPerApplicationContextException) {

        return "context:unbound";
      }
    }
  }
}
