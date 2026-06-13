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
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.smallmind.web.jetty.option.WebApplicationOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration coverage that boots an embedded Jetty server and exercises the finer installer-configuration branches
 * of {@link JettyInitializingBean#onApplicationEvent} that the broader boot scenarios leave untouched: filter and
 * servlet init parameters, async-support toggles, servlet load-on-startup ordering, the default {@code /*} URL
 * pattern taken when an installer leaves its pattern unset, listener context parameters, and the reflective
 * instantiation failure paths reached when a class-based installer cannot be instantiated. Each scenario boots its
 * own freshly created {@link GenericApplicationContext} on an ephemeral port and shuts the server down on context
 * close.
 */
@Test(groups = "integration")
public class JettyInstallerDetailBootScenariosTest {

  public static class EchoServlet extends HttpServlet {

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws IOException {

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write("servlet-echo");
    }
  }

  public static class InitParameterServlet extends HttpServlet {

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response)
      throws IOException {

      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write("servlet-param=" + getInitParameter("greeting"));
    }
  }

  public static class InitParameterFilter implements Filter {

    private String banner;

    @Override
    public void init (FilterConfig filterConfig) {

      banner = filterConfig.getInitParameter("banner");
    }

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      ((HttpServletResponse)response).addHeader("X-Filter-Banner", (banner == null) ? "none" : banner);
      chain.doFilter(request, response);
    }
  }

  public static class ContextParameterListener implements ServletContextListener {

    private static final AtomicReference<String> CAPTURED = new AtomicReference<>(null);

    public static String captured () {

      return CAPTURED.get();
    }

    public static void reset () {

      CAPTURED.set(null);
    }

    @Override
    public void contextInitialized (ServletContextEvent servletContextEvent) {

      ServletContext servletContext = servletContextEvent.getServletContext();

      CAPTURED.set(servletContext.getInitParameter("listener-context-key"));
    }

    @Override
    public void contextDestroyed (ServletContextEvent servletContextEvent) {

    }
  }

  /**
   * A servlet whose only constructor demands an argument, so the no-arg reflective instantiation performed by the
   * installer fails with an {@link InstantiationException} and surfaces as a boot failure.
   */
  public static class NonInstantiableServlet extends HttpServlet {

    public NonInstantiableServlet (String required) {

    }

    @Override
    protected void doGet (HttpServletRequest request, HttpServletResponse response) {

    }
  }

  /**
   * A filter whose only constructor demands an argument, so the no-arg reflective instantiation performed by the
   * installer fails with an {@link InstantiationException} and surfaces as a boot failure.
   */
  public static class NonInstantiableFilter implements Filter {

    public NonInstantiableFilter (String required) {

    }

    @Override
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

      chain.doFilter(request, response);
    }
  }

  /**
   * A listener whose only constructor demands an argument, so the no-arg reflective instantiation performed by the
   * installer fails with a {@link NoSuchMethodException} and surfaces as a boot failure.
   */
  public static class NonInstantiableListener implements ServletContextListener {

    public NonInstantiableListener (String required) {

    }

    @Override
    public void contextInitialized (ServletContextEvent servletContextEvent) {

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

  public void testServletInitParametersAsyncAndLoadOrderApplied ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/params");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("parameterServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();
        Map<String, String> initParameters = new HashMap<>();

        initParameters.put("greeting", "hello");

        servletInstaller.setContextPath("/params");
        servletInstaller.setDisplayName("parameter-servlet");
        servletInstaller.setServlet(new InitParameterServlet());
        servletInstaller.setUrlPattern("/show");
        // Exercise the optional init-parameter, async-support, and load-on-startup branches of the servlet installer loop.
        servletInstaller.setInitParameters(initParameters);
        servletInstaller.setAsyncSupported(true);
        servletInstaller.setLoadOnStartup(1);

        return servletInstaller;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/params/show");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "servlet-param=hello");
    }
  }

  public void testFilterInitParametersAsyncAndDefaultUrlPatternApplied ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/filtered");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("echoServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/filtered");
        servletInstaller.setDisplayName("echo");
        servletInstaller.setServlet(new EchoServlet());
        servletInstaller.setUrlPattern("/echo");

        return servletInstaller;
      });

      applicationContext.registerBean("bannerFilter", FilterInstaller.class, () -> {

        FilterInstaller filterInstaller = new FilterInstaller();
        Map<String, String> initParameters = new HashMap<>();

        initParameters.put("banner", "filter-banner-value");

        filterInstaller.setContextPath("/filtered");
        filterInstaller.setDisplayName("banner");
        filterInstaller.setFilter(new InitParameterFilter());
        // Leaving the URL pattern unset drives the default "/*" mapping branch in the filter installer loop.
        filterInstaller.setInitParameters(initParameters);
        filterInstaller.setAsyncSupported(true);

        return filterInstaller;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/filtered/echo");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "servlet-echo");
      // The filter mapped to the default "/*" pattern still runs for the request and applies its init-parameter banner.
      Assert.assertEquals(response.headers().firstValue("X-Filter-Banner").orElse(null), "filter-banner-value");
    }
  }

  public void testServletDefaultUrlPatternServesAtContextRoot ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/root");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("rootServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/root");
        servletInstaller.setDisplayName("root");
        servletInstaller.setServlet(new EchoServlet());
        // No URL pattern set, so the servlet installer falls back to the default "/*" mapping.

        return servletInstaller;
      });

      applicationContext.refresh();

      HttpResponse<String> response = get("http://127.0.0.1:" + port + "/root/anything");

      Assert.assertEquals(response.statusCode(), 200);
      Assert.assertEquals(response.body(), "servlet-echo");
    }
  }

  public void testListenerContextParametersAppliedToServletContext ()
    throws Exception {

    int port = freePort();

    ContextParameterListener.reset();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/ctxparams");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("contextParameterListener", ListenerInstaller.class, () -> {

        ListenerInstaller listenerInstaller = new ListenerInstaller();
        Map<String, String> contextParameters = new HashMap<>();

        contextParameters.put("listener-context-key", "listener-context-value");

        listenerInstaller.setContextPath("/ctxparams");
        listenerInstaller.setEventListener(new ContextParameterListener());
        // Supplying context parameters drives the init-parameter branch of the listener installer loop.
        listenerInstaller.setContextParameters(contextParameters);

        return listenerInstaller;
      });

      applicationContext.refresh();

      // The listener observes the context parameter the installer copied onto the servlet context during startup.
      Assert.assertEquals(ContextParameterListener.captured(), "listener-context-value");
    }
  }

  public void testServletClassInstantiationFailureFailsToBoot ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/bad-servlet");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("nonInstantiableServlet", ServletInstaller.class, () -> {

        ServletInstaller servletInstaller = new ServletInstaller();

        servletInstaller.setContextPath("/bad-servlet");
        servletInstaller.setDisplayName("non-instantiable");
        // No instance supplied; the class has no no-arg constructor, so reflective creation fails during boot.
        servletInstaller.setServletClass(NonInstantiableServlet.class);
        servletInstaller.setUrlPattern("/never");

        return servletInstaller;
      });

      // The InstantiationException raised while creating the servlet is wrapped as a JettyInitializationException.
      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testFilterClassInstantiationFailureFailsToBoot ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/bad-filter");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("nonInstantiableFilter", FilterInstaller.class, () -> {

        FilterInstaller filterInstaller = new FilterInstaller();

        filterInstaller.setContextPath("/bad-filter");
        filterInstaller.setDisplayName("non-instantiable");
        // No instance supplied; the class has no no-arg constructor, so reflective creation fails during boot.
        filterInstaller.setFilterClass(NonInstantiableFilter.class);
        filterInstaller.setUrlPattern("/never");

        return filterInstaller;
      });

      // The InstantiationException raised while creating the filter is wrapped as a JettyInitializationException.
      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }

  public void testListenerClassInstantiationFailureFailsToBoot ()
    throws Exception {

    int port = freePort();

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("jettyPostProcessor", JettyPostProcessor.class, JettyPostProcessor::new);

      applicationContext.registerBean("jettyServer", JettyInitializingBean.class, () -> {

        JettyInitializingBean bean = new JettyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        bean.setAllowInsecure(true);

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/bad-listener");

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.registerBean("nonInstantiableListener", ListenerInstaller.class, () -> {

        ListenerInstaller listenerInstaller = new ListenerInstaller();

        listenerInstaller.setContextPath("/bad-listener");
        // No instance supplied; the class has no no-arg constructor, so reflective creation fails during boot.
        listenerInstaller.setListenerClass(NonInstantiableListener.class);

        return listenerInstaller;
      });

      // The NoSuchMethodException raised while creating the listener is wrapped as a JettyInitializationException.
      Assert.assertThrows(Exception.class, applicationContext::refresh);
    }
  }
}
