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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.property.PropertyClosure;
import org.smallmind.nutsnbolts.property.PropertyExpander;
import org.smallmind.nutsnbolts.property.PropertyExpanderException;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.smallmind.nutsnbolts.spring.PropertyPlaceholderStringValueResolver;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.SystemPropertyMode;
import org.smallmind.scribe.pen.Level;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.option.JaxRSOption;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Boots an embedded Grizzly server with and without a {@link PerApplicationScope} configured on the
 * {@link GrizzlyInitializingBean}, and verifies through a JAX-RS probe, a plain installer-declared servlet, and an
 * {@code AsyncContext.dispatch} leg which per-application binding the request thread sees.
 */
@Test(groups = "integration")
public class GrizzlyPerApplicationScopeBootTest {

  private static final AtomicReference<String> CHAIN_PROBE = new AtomicReference<>();
  private static final AtomicReference<SpringPropertyAccessor> REGISTERED_ACCESSOR = new AtomicReference<>();

  @BeforeMethod
  public void setUp () {

    CHAIN_PROBE.set(null);
    REGISTERED_ACCESSOR.set(null);
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

  // With no PerApplicationContext on the refresh thread the bean registers no context filter at all, so a scope-only
  // deployment's REST workers carry only the scope; the probe must therefore see the context as unbound rather than
  // as an empty, filter-created map.
  public void testScopeOnlyDeploymentRegistersNoContextFilter ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {

        int port = freePort();
        PerApplicationScope perApplicationScope = new PerApplicationScope();

        perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

          registerProbeBeans(applicationContext, port, perApplicationScope);
          registerAccessorBean(applicationContext);
          perApplicationScope.run(applicationContext::refresh);

          HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
          HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
          HttpResponse<String> accessorResponse = get(port, "/probe/rest/probe/accessor");

          Assert.assertEquals(scopeResponse.statusCode(), 200);
          Assert.assertEquals(scopeResponse.body(), "scope:scoped");
          Assert.assertEquals(contextResponse.statusCode(), 200);
          Assert.assertEquals(contextResponse.body(), "context:unbound");
          Assert.assertEquals(accessorResponse.statusCode(), 200);
          Assert.assertEquals(accessorResponse.body(), "accessor:registered");
          Assert.assertEquals(CHAIN_PROBE.get(), "scope:true,context:false", "the scope filter must run ahead of installer-declared filters");
          Assert.assertNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-context"));
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  // Runs on a thread that inherits nothing, attaches a context there, and refreshes on it: the bean must register
  // the context filter from that bound thread, and the probe must see the context the filter binds. (Worker threads
  // also inherit this thread's map, so the REST probe alone cannot distinguish "filter" from "inherited"; the plain
  // and async probes outside the REST path show the scope is absent either way.)
  public void testDefaultKeepsPerApplicationContextFilter ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        defaultFilterProbe();
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  // When a different scope is bound on the refreshing thread, the configured one is refused rather than wired empty:
  // the bean fails the refresh with a ScopeException. (With no scope bound at all the property is accepted; the
  // bridge then falls back to the context.)
  public void testWrongScopeInstanceIsRefusedAtRefresh ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {

        int port = freePort();
        PerApplicationScope boundScope = new PerApplicationScope();
        PerApplicationScope otherScope = new PerApplicationScope();

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

          registerProbeBeans(applicationContext, port, otherScope);
          try {
            boundScope.run(applicationContext::refresh);
            Assert.fail("a scope the refresh did not run in must be refused");
          } catch (RuntimeException runtimeException) {

            Throwable cause = runtimeException;

            while ((cause != null) && (!(cause instanceof ScopeException))) {
              cause = cause.getCause();
            }

            Assert.assertNotNull(cause, "expected a ScopeException in the cause chain, saw " + runtimeException);
          }
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  // Runs on a thread that inherits nothing, attaches a context there and binds the configured scope around the
  // refresh: the bean registers both filters, and a REST request thread sees the scope and the context together.
  public void testScopeAndContextBothBoundAroundRefreshBindBothOnRequestThreads ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {

        int port = freePort();
        PerApplicationScope perApplicationScope = new PerApplicationScope();

        new PerApplicationContext();
        PerApplicationContext.setPerApplicationData(SampleManager.class, "thread-local");
        perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

          registerProbeBeans(applicationContext, port, perApplicationScope);
          registerAccessorBean(applicationContext);
          perApplicationScope.run(applicationContext::refresh);

          HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
          HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
          HttpResponse<String> accessorResponse = get(port, "/probe/rest/probe/accessor");
          HttpResponse<String> plainResponse = get(port, "/probe/plain/scope");

          Assert.assertEquals(scopeResponse.statusCode(), 200);
          Assert.assertEquals(scopeResponse.body(), "scope:scoped");
          Assert.assertEquals(contextResponse.statusCode(), 200);
          Assert.assertEquals(contextResponse.body(), "context:thread-local");
          Assert.assertEquals(accessorResponse.statusCode(), 200);
          Assert.assertEquals(accessorResponse.body(), "accessor:registered");
          Assert.assertEquals(plainResponse.statusCode(), 200);
          Assert.assertEquals(plainResponse.body(), "scope:scoped");
          Assert.assertEquals(CHAIN_PROBE.get(), "scope:true,context:true", "the scope filter must run ahead of installer-declared filters; the probe's context value may also come from worker inheritance");
          Assert.assertNotNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-scope"));
          Assert.assertNotNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-context"));
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  // A scope bound around the refresh that the bean was never given is not wired: no scope filter, and with no context
  // on the refresh thread no context filter either, so request threads are bound to nothing and the accessor the
  // refresh registered into that scope is out of reach. The bean says so with exactly one warning.
  public void testScopeBoundAroundRefreshWithoutBeanScopeLeavesRequestThreadsUnbound ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {

        int port = freePort();
        PerApplicationScope perApplicationScope = new PerApplicationScope();

        perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

        CapturingAppender appender = CapturingAppender.attach(GrizzlyInitializingBean.class, Level.WARN);

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

          registerProbeBeans(applicationContext, port, null);
          registerAccessorBean(applicationContext);
          perApplicationScope.run(applicationContext::refresh);

          Assert.assertEquals(appender.size(), 1, "exactly one warning is expected from the refresh, saw " + appender.getRecords());
          Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.WARN);
          Assert.assertTrue(appender.getRecords().get(0).getMessage().contains("no perApplicationScope was set on this bean"), appender.getRecords().get(0).getMessage());

          HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
          HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
          HttpResponse<String> accessorResponse = get(port, "/probe/rest/probe/accessor");

          Assert.assertEquals(scopeResponse.statusCode(), 200);
          Assert.assertEquals(scopeResponse.body(), "scope:unbound");
          Assert.assertEquals(contextResponse.statusCode(), 200);
          Assert.assertEquals(contextResponse.body(), "context:unbound");
          Assert.assertEquals(accessorResponse.statusCode(), 200);
          Assert.assertEquals(accessorResponse.body(), "accessor:unbound");
          Assert.assertEquals(CHAIN_PROBE.get(), "scope:false,context:false");
          Assert.assertNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-scope"));
          Assert.assertNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-context"));
        } finally {
          CapturingAppender.release(GrizzlyInitializingBean.class);
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  // Runs on a thread that inherits nothing, attaches a context there and binds a scope the bean was never given
  // around the refresh: the scope is not wired, but the context filter is registered from the bound thread, so request
  // threads carry the context alone and reach the accessor the refresh registered into both maps through it. With a
  // context for the request threads to carry the bean has nothing to warn about.
  public void testBothBoundAroundRefreshWithoutBeanScopeBindsOnlyTheContextOnRequestThreads ()
    throws Exception {

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {

        int port = freePort();
        PerApplicationScope perApplicationScope = new PerApplicationScope();
        CapturingAppender appender = CapturingAppender.attach(GrizzlyInitializingBean.class, Level.WARN);

        new PerApplicationContext();
        PerApplicationContext.setPerApplicationData(SampleManager.class, "thread-local");
        perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

        try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

          registerProbeBeans(applicationContext, port, null);
          registerAccessorBean(applicationContext);
          perApplicationScope.run(applicationContext::refresh);

          HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
          HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
          HttpResponse<String> accessorResponse = get(port, "/probe/rest/probe/accessor");

          Assert.assertEquals(scopeResponse.statusCode(), 200);
          Assert.assertEquals(scopeResponse.body(), "scope:unbound");
          Assert.assertEquals(contextResponse.statusCode(), 200);
          Assert.assertEquals(contextResponse.body(), "context:thread-local");
          Assert.assertEquals(accessorResponse.statusCode(), 200);
          Assert.assertEquals(accessorResponse.body(), "accessor:registered");
          Assert.assertEquals(CHAIN_PROBE.get(), "scope:false,context:true");
          Assert.assertEquals(appender.size(), 0, "no warning is expected when the refresh thread carries a context, saw " + appender.getRecords());
          Assert.assertNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-scope"));
          Assert.assertNotNull(probeWebAppContext(applicationContext).getFilterRegistration("per-application-context"));
        } finally {
          CapturingAppender.release(GrizzlyInitializingBean.class);
        }
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }
  }

  private static void defaultFilterProbe ()
    throws Exception {

    int port = freePort();

    // The bean adopts the map attached to the refresh thread, which is this thread.
    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(SampleManager.class, "thread-local");

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      registerProbeBeans(applicationContext, port, null);
      registerAccessorBean(applicationContext);
      applicationContext.refresh();

      WebappContext webAppContext = probeWebAppContext(applicationContext);
      HttpResponse<String> scopeResponse = get(port, "/probe/rest/probe/scope");
      HttpResponse<String> contextResponse = get(port, "/probe/rest/probe/context");
      HttpResponse<String> accessorResponse = get(port, "/probe/rest/probe/accessor");
      HttpResponse<String> plainResponse = get(port, "/probe/plain/scope");
      HttpResponse<String> asyncResponse = get(port, "/probe/async/scope");
      HttpResponse<String> asyncRestResponse = get(port, "/probe/async-rest/x");
      HttpResponse<String> restAsyncResponse = get(port, "/probe/rest/async/x");

      Assert.assertEquals(scopeResponse.statusCode(), 200);
      Assert.assertEquals(scopeResponse.body(), "scope:unbound");
      Assert.assertEquals(contextResponse.statusCode(), 200);
      Assert.assertEquals(contextResponse.body(), "context:thread-local");
      Assert.assertEquals(accessorResponse.statusCode(), 200);
      Assert.assertEquals(accessorResponse.body(), "accessor:registered");
      Assert.assertEquals(plainResponse.statusCode(), 200);
      Assert.assertEquals(plainResponse.body(), "scope:unbound");
      Assert.assertEquals(asyncResponse.statusCode(), 200);
      Assert.assertEquals(asyncResponse.body(), "scope:unbound");
      // The request leg of /async-rest/* meets no bean filter, so this shows only that the ASYNC dispatch reaches the
      // context probe with a context visible; the worker may carry that context by inheritance.
      Assert.assertEquals(asyncRestResponse.statusCode(), 200);
      Assert.assertEquals(asyncRestResponse.body(), "context:thread-local", "the ASYNC dispatch onto the REST path must reach the context probe");
      // The request leg of /rest/async/* runs through the context filter before the servlet suspends the request. This
      // shows only that the rest-async servlet mapping wins over the JAX-RS servlet (a 404 would mean it lost) and that
      // the ASYNC dispatch reaches the probe; the worker carries the context by inheritance either way, so neither
      // the filter's ASYNC mapping nor its async-supported flag is exercised here. Grizzly's FilterRegistration
      // exposes only the URL patterns, asserted below, and never reads the async flag at all.
      Assert.assertEquals(restAsyncResponse.statusCode(), 200, "the rest-async servlet mapping must win over the JAX-RS servlet, saw " + restAsyncResponse.body());
      Assert.assertEquals(restAsyncResponse.body(), "context:thread-local");

      // The context filter is registered on the REST path and the installer-declared probe filter on that path ran
      // with the context bound. Registration order is not chain order in Grizzly, and the probe's context value cannot
      // distinguish the filter from worker inheritance, so the context filter's place in the chain rests on the
      // container-source ordering described in the bean; only the scope half of the probe is proof of ordering.
      Assert.assertNull(webAppContext.getFilterRegistration("per-application-scope"));
      Assert.assertNotNull(webAppContext.getFilterRegistration("per-application-context"));
      Assert.assertTrue(webAppContext.getFilterRegistration("per-application-context").getUrlPatternMappings().contains("/rest/*"));
      Assert.assertEquals(CHAIN_PROBE.get(), "scope:false,context:true");
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

  private static FilterInstaller filterInstaller (String displayName, String urlPattern, Filter filter) {

    FilterInstaller filterInstaller = new FilterInstaller();

    filterInstaller.setContextPath("/probe");
    filterInstaller.setDisplayName(displayName);
    filterInstaller.setUrlPattern(urlPattern);
    filterInstaller.setFilter(filter);

    return filterInstaller;
  }

  // The post-processor must be registered before the installers and the server so it sees them all. The rest-async
  // servlet sits under the REST path on a mapping more specific than the JAX-RS "/rest/*", so its REQUEST leg runs
  // through the context filter's "/rest/*" mapping before it suspends the request.
  private static void registerProbeBeans (GenericApplicationContext applicationContext, int port, PerApplicationScope perApplicationScope) {

    applicationContext.registerBean("grizzlyPostProcessor", GrizzlyPostProcessor.class, GrizzlyPostProcessor::new);
    applicationContext.registerBean("plainServlet", ServletInstaller.class, () -> servletInstaller("plain", "/plain/*", new ScopeProbeServlet()));
    applicationContext.registerBean("asyncServlet", ServletInstaller.class, () -> servletInstaller("async", "/async/*", new AsyncDispatchingServlet()));
    applicationContext.registerBean("asyncRestServlet", ServletInstaller.class, () -> servletInstaller("async-rest", "/async-rest/*", new AsyncRestDispatchingServlet()));
    applicationContext.registerBean("restAsyncServlet", ServletInstaller.class, () -> servletInstaller("rest-async", "/rest/async/*", new AsyncRestDispatchingServlet()));
    applicationContext.registerBean("chainProbeFilter", FilterInstaller.class, () -> filterInstaller("chain-probe", "/rest/*", new ChainProbeFilter()));
    applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> probeServer(port, perApplicationScope));
  }

  // Registers an accessor through the bridging manager during the refresh, from a bean's afterPropertiesSet, so the
  // probe can report whether a request thread resolves it.
  private static void registerAccessorBean (GenericApplicationContext applicationContext) {

    applicationContext.registerBean("accessorRegistrar", AccessorRegisteringBean.class, AccessorRegisteringBean::new);
  }

  private static WebappContext probeWebAppContext (GenericApplicationContext applicationContext) {

    return applicationContext.getBean("grizzlyServer", GrizzlyInitializingBean.class).webAppStateFor("/probe").getWebAppContext();
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

  // Suspends the request and dispatches it onto the REST path, so the ASYNC leg runs through the context filter's
  // mapping; mapped under the REST path as well, its REQUEST leg runs through that mapping too.
  public static class AsyncRestDispatchingServlet extends HttpServlet {

    @Override
    protected void service (HttpServletRequest request, HttpServletResponse response) {

      request.startAsync().dispatch("/rest/probe/context");
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

    // Resolves through the bridging manager, which consults the scope first and the context otherwise; with neither
    // bound the context lookup throws.
    @GET
    @Path("/accessor")
    public String accessor () {

      try {

        SpringPropertyAccessor springPropertyAccessor = SpringPropertyAccessorManager.getSpringPropertyAccessor();

        return "accessor:" + ((springPropertyAccessor == null) ? "null" : (springPropertyAccessor == REGISTERED_ACCESSOR.get()) ? "registered" : "other");
      } catch (MissingPerApplicationContextException missingPerApplicationContextException) {

        return "accessor:unbound";
      }
    }
  }

  // Declared through a FilterInstaller, so it runs behind whatever filters the bean registered ahead of the
  // installers. A scope it sees can only have been bound by a scope filter earlier in the chain, since no thread
  // inherits a ScopedValue; a context it sees may also have been inherited by the worker thread from the refresh.
  public static class ChainProbeFilter implements Filter {

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      CHAIN_PROBE.set("scope:" + PerApplicationScope.isBound() + ",context:" + PerApplicationContext.isBound());
      chain.doFilter(request, response);
    }
  }

  public static class AccessorRegisteringBean implements InitializingBean {

    @Override
    public void afterPropertiesSet ()
      throws PropertyExpanderException {

      Map<String, Object> propertyMap = new HashMap<>();
      SpringPropertyAccessor springPropertyAccessor;

      propertyMap.put("answer", "42");
      springPropertyAccessor = new SpringPropertyAccessor(new PropertyPlaceholderStringValueResolver(new PropertyExpander(new PropertyClosure(), false, SystemPropertyMode.FALLBACK, false), propertyMap));

      REGISTERED_ACCESSOR.set(springPropertyAccessor);
      SpringPropertyAccessorManager.register(springPropertyAccessor);
    }
  }
}
