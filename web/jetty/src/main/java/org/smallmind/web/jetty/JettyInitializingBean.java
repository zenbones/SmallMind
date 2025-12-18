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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import jakarta.xml.ws.Endpoint;
import com.sun.net.httpserver.HttpContext;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.spi.JettyHttpServer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.smallmind.web.jetty.installer.FilterInstaller;
import org.smallmind.web.jetty.installer.ListenerInstaller;
import org.smallmind.web.jetty.installer.ServletInstaller;
import org.smallmind.web.jetty.installer.WebServiceInstaller;
import org.smallmind.web.jetty.option.WebApplicationOption;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Spring-managed component that builds and starts a Jetty server using the provided options.
 * <p>
 * The bean collects installers for listeners, filters, servlets and web services, wires them into Jetty,
 * and optionally configures REST, Spring request context handling, WebSocket support, and SSL connectors.
 */
public class JettyInitializingBean implements JettyWebAppStateLocator, InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

  private final HashMap<String, JettyWebAppState> webAppStateMap = new HashMap<>();
  private Server server;
  private ResourceConfig resourceConfig;
  private WebApplicationOption[] webApplicationOptions;
  private SSLInfo sslInfo;
  private String host;
  private Integer maxHttpHeaderSize;
  private Integer initialWorkerPoolSize;
  private Integer maximumWorkerPoolSize;
  private int port = 80;
  private boolean allowInsecure = true;
  private boolean suppressConnectionClosedException;
  private boolean debug = false;

  /**
   * Sets the host name or address Jetty should bind to. A {@code null} value lets Jetty choose the default interface.
   *
   * @param host the host interface or {@code null} for all interfaces
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Sets the HTTP port used when insecure connections are enabled.
   *
   * @param port the port number to listen on for HTTP traffic
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Supplies SSL configuration used to create an HTTPS connector.
   *
   * @param sslInfo certificate and trust configuration for HTTPS
   */
  public void setSslInfo (SSLInfo sslInfo) {

    this.sslInfo = sslInfo;
  }

  /**
   * Sets the Jersey {@link ResourceConfig} that drives any JAX-RS servlet created by this bean.
   *
   * @param resourceConfig the Jersey resource configuration
   */
  public void setResourceConfig (ResourceConfig resourceConfig) {

    this.resourceConfig = resourceConfig;
  }

  /**
   * Defines the collection of web application options this bean should install.
   *
   * @param webApplicationOptions the contexts and their configuration
   */
  public void setWebApplicationOptions (WebApplicationOption[] webApplicationOptions) {

    this.webApplicationOptions = webApplicationOptions;
  }

  /**
   * Sets the minimum number of worker threads Jetty should keep available.
   *
   * @param initialWorkerPoolSize the minimum thread count or {@code null} to use Jetty defaults
   */
  public void setInitialWorkerPoolSize (Integer initialWorkerPoolSize) {

    this.initialWorkerPoolSize = initialWorkerPoolSize;
  }

  /**
   * Sets the maximum number of worker threads Jetty can create.
   *
   * @param maximumWorkerPoolSize the maximum thread count or {@code null} to use Jetty defaults
   */
  public void setMaximumWorkerPoolSize (Integer maximumWorkerPoolSize) {

    this.maximumWorkerPoolSize = maximumWorkerPoolSize;
  }

  /**
   * Limits the size of HTTP headers Jetty will accept.
   *
   * @param maxHttpHeaderSize the header size in bytes or {@code null} to use Jetty defaults
   */
  public void setMaxHttpHeaderSize (Integer maxHttpHeaderSize) {

    this.maxHttpHeaderSize = maxHttpHeaderSize;
  }

  /**
   * Enables or disables creation of an insecure HTTP connector.
   *
   * @param allowInsecure {@code true} to expose HTTP alongside HTTPS
   */
  public void setAllowInsecure (boolean allowInsecure) {

    this.allowInsecure = allowInsecure;
  }

  /**
   * Suppresses noisy connection-closed exceptions when wiring the per-application filter.
   *
   * @param suppressConnectionClosedException {@code true} to swallow benign connection errors
   */
  public void setSuppressConnectionClosedException (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * Enables verbose SOAP request dumping useful during debugging.
   *
   * @param debug {@code true} to enable SOAP adapter dump logging
   */
  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  /**
   * Validates that each declared application option has a unique context path and seeds the state map.
   *
   * @throws JettyInitializationException if two options share the same context path
   */
  @Override
  public void afterPropertiesSet () {

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {
      if (webAppStateMap.containsKey(webApplicationOption.getContextPath())) {
        throw new JettyInitializationException("Duplicate context paths(%s) are not allowed", webApplicationOption.getContextPath());
      } else {
        webAppStateMap.put(webApplicationOption.getContextPath(), new JettyWebAppState());
      }
    }
  }

  /**
   * Locates the mutable state object for the supplied context path.
   *
   * @param context the context path used to look up configuration
   * @return the state bucket that collects installers for the context
   * @throws JettyInitializationException if the context path is missing or was never configured
   */
  @Override
  public JettyWebAppState webAppStateFor (String context) {

    if ((context == null) || context.isEmpty()) {
      throw new JettyInitializationException("Missing context path");
    } else {

      JettyWebAppState webAppState;

      if ((webAppState = webAppStateMap.get(context)) == null) {
        throw new JettyInitializationException("The context path(%s) was not properly initialized", context);
      } else {

        return webAppState;
      }
    }
  }

  /**
   * Registers the supplied Spring application context so it can be exposed to Jersey.
   *
   * @param applicationContext the Spring application context received during bootstrapping
   */
  @Override
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

  /**
   * Builds and starts the Jetty server once the Spring context is fully refreshed.
   * <p>
   * The method configures thread pools, connectors (HTTP/HTTPS), static resources, SOAP endpoints,
   * Jersey servlets, WebSocket support, and Spring request context listeners based on the supplied options.
   *
   * @param contextRefreshedEvent the Spring refresh event that triggers initialization
   * @throws JettyInitializationException if any part of server creation or handler wiring fails
   */
  @Override
  public void onApplicationEvent (ContextRefreshedEvent contextRefreshedEvent) {

    QueuedThreadPool threadPool = new QueuedThreadPool();
    HttpConfiguration httpConfig = new HttpConfiguration();
    ContextHandlerCollection contextHandlerCollection = new ContextHandlerCollection();

    if (debug) {
      System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
    }

    if (initialWorkerPoolSize != null) {
      threadPool.setMinThreads(initialWorkerPoolSize);
    }
    if (maximumWorkerPoolSize != null) {
      threadPool.setMaxThreads(maximumWorkerPoolSize);
    }

    server = new Server(threadPool);

    httpConfig.setOutputBufferSize(32768);
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);
    if (maxHttpHeaderSize != null) {
      httpConfig.setRequestHeaderSize(maxHttpHeaderSize);
      httpConfig.setResponseHeaderSize(maxHttpHeaderSize);
    }

    if (sslInfo != null) {

      ServerConnector sslConnector;
      SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);

      httpsConfig.setSecureScheme("https");
      httpsConfig.setSecurePort(sslInfo.getPort());
      httpsConfig.addCustomizer(new SecureRequestCustomizer());

      sslContextFactory.setKeyStorePassword(sslInfo.getKeySSLStore().getPassword());
      try {
        sslContextFactory.setKeyStoreResource(new ByteArrayResource(sslInfo.getKeySSLStore().getBytes()));
      } catch (IOException | ResourceException exception) {
        throw new JettyInitializationException(exception);
      }

      sslContextFactory.setTrustStorePassword(sslInfo.getTrustSSLStore().getPassword());
      try {
        sslContextFactory.setTrustStoreResource(new ByteArrayResource(sslInfo.getTrustSSLStore().getBytes()));
      } catch (IOException | ResourceException exception) {
        throw new JettyInitializationException(exception);
      }

      sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), new HttpConnectionFactory(httpsConfig));
      sslConnector.setHost(host);
      sslConnector.setPort(sslInfo.getPort());

      server.addConnector(sslConnector);
    }
    if (allowInsecure) {

      ServerConnector httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));

      httpConnector.setHost(host);
      httpConnector.setPort(port);

      server.addConnector(httpConnector);
    } else if (sslInfo == null) {
      throw new JettyInitializationException("Instance is not configured to allow insecure connection, and does not provide any ssl info");
    }

    server.setHandler(contextHandlerCollection);

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {

      JettyWebAppState webAppState = webAppStateFor(webApplicationOption.getContextPath());

      if (webApplicationOption.getClassLoaderResourceOption() != null) {

        ContextHandler staticContextHandler = new ContextHandler(combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getClassLoaderResourceOption().getStaticPath()));
        ResourceHandler staticResourceHandler = new ResourceHandler();

        staticResourceHandler.setBaseResource(ResourceFactory.of(staticResourceHandler).newClassLoaderResource("/", false));
        staticContextHandler.setHandler(staticResourceHandler);
        contextHandlerCollection.addHandler(staticContextHandler);
      }

      if (webApplicationOption.getDocumentRootOption() != null) {
        for (Map.Entry<String, String> documentRootEntry : webApplicationOption.getDocumentRootOption().getDocumentRoots().entrySet()) {

          ContextHandler documentContextHandler = new ContextHandler(combinePaths(combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getDocumentRootOption().getDocumentPath()), normalizePath(documentRootEntry.getKey())));
          ResourceHandler documentResourceHandler = new ResourceHandler();

          documentResourceHandler.setBaseResource(ResourceFactory.of(documentResourceHandler).newResource(Paths.get(documentRootEntry.getValue())));
          documentContextHandler.setHandler(documentContextHandler);

          contextHandlerCollection.addHandler(documentContextHandler);
        }
      }

      if (!webAppState.getWebServiceInstallerList().isEmpty()) {

        JettyHttpServer jettyHttpServer = new JettyHttpServer(server, true);

        for (WebServiceInstaller webServiceInstaller : webAppState.getWebServiceInstallerList()) {

          HttpContext httpContext = jettyHttpServer.createContext(combinePaths(combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getSoapPath()), normalizePath(webServiceInstaller.getPath())));
          Endpoint endpoint = Endpoint.create(webServiceInstaller.getService());

          endpoint.publish(httpContext);
        }
      }

      if ((!webAppState.getListenerInstallerList().isEmpty()) || (!webAppState.getFilterInstallerList().isEmpty()) || (!webAppState.getServletInstallerList().isEmpty()) || (webApplicationOption.getJaxRSOption() != null) || (webApplicationOption.getSpringSupportOption() != null) || (webApplicationOption.getWebSocketOption() != null)) {

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

        servletContextHandler.setContextPath(webApplicationOption.getContextPath());

        for (ListenerInstaller listenerInstaller : webAppState.getListenerInstallerList()) {
          try {

            Map<String, String> contextParameters;

            servletContextHandler.addEventListener(listenerInstaller.getListener());
            if ((contextParameters = listenerInstaller.getContextParameters()) != null) {
              for (Map.Entry<String, String> parameterEntry : contextParameters.entrySet()) {
                servletContextHandler.setInitParameter(parameterEntry.getKey(), parameterEntry.getValue());
              }
            }
          } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            throw new JettyInitializationException(exception);
          }
        }
        for (FilterInstaller filterInstaller : webAppState.getFilterInstallerList()) {
          try {

            FilterHolder filterHolder = new FilterHolder(filterInstaller.getFilter());
            String urlPattern;

            filterHolder.setName(filterInstaller.getDisplayName());
            filterHolder.setDisplayName(filterInstaller.getDisplayName());
            if (filterInstaller.getAsyncSupported() != null) {
              filterHolder.setAsyncSupported(filterInstaller.getAsyncSupported());
            }
            if (filterInstaller.getInitParameters() != null) {
              filterHolder.setInitParameters(filterInstaller.getInitParameters());
            }

            servletContextHandler.addFilter(filterHolder, ((urlPattern = filterInstaller.getUrlPattern()) == null) ? "/*" : urlPattern, EnumSet.of(DispatcherType.REQUEST));
          } catch (IllegalAccessException | InstantiationException exception) {
            throw new JettyInitializationException(exception);
          }
        }
        for (ServletInstaller servletInstaller : webAppState.getServletInstallerList()) {
          try {

            ServletHolder servletHolder = new ServletHolder(servletInstaller.getServlet());
            String urlPattern;

            servletHolder.setName(servletInstaller.getDisplayName());
            servletHolder.setDisplayName(servletInstaller.getDisplayName());
            if (servletInstaller.getLoadOnStartup() != null) {
              servletHolder.setInitOrder(servletInstaller.getLoadOnStartup());
            }
            if (servletInstaller.getAsyncSupported() != null) {
              servletHolder.setAsyncSupported(servletInstaller.getAsyncSupported());
            }
            if (servletInstaller.getInitParameters() != null) {
              servletHolder.setInitParameters(servletInstaller.getInitParameters());
            }

            servletContextHandler.addServlet(servletHolder, ((urlPattern = servletInstaller.getUrlPattern()) == null) ? "/*" : urlPattern);
          } catch (IllegalAccessException | InstantiationException exception) {
            throw new JettyInitializationException(exception);
          }
        }

        if (webApplicationOption.getJaxRSOption() != null) {

          ServletHolder jerseyServletHolder = new ServletHolder(new ServletContainer(resourceConfig));
          FilterHolder jerseyFilterHolder = new FilterHolder(new PerApplicationContextFilter(suppressConnectionClosedException));

          jerseyServletHolder.setName("JAX-RS Application");
          jerseyServletHolder.setDisplayName("JAX-RS Application");

          jerseyFilterHolder.setName("per-application-data");
          jerseyFilterHolder.setDisplayName("per-application-data");

          servletContextHandler.addServlet(jerseyServletHolder, webApplicationOption.getJaxRSOption().getRestPath() + "/*");
          servletContextHandler.addFilter(jerseyFilterHolder, webApplicationOption.getJaxRSOption().getRestPath() + "/*", EnumSet.of(DispatcherType.REQUEST));
        }

        if (webApplicationOption.getSpringSupportOption() != null) {
          servletContextHandler.addEventListener(new JettyRequestContextListener());
        }

        if (webApplicationOption.getWebSocketOption() != null) {
          try {
            JakartaWebSocketServletContainerInitializer.configure(servletContextHandler, (servletContext, serverContainer) -> serverContainer.setDefaultMaxSessionIdleTimeout(webApplicationOption.getWebSocketOption().getMaxSessionIdleTimeout()));
          } catch (Exception exception) {
            throw new JettyInitializationException(exception);
          }
        }

        contextHandlerCollection.addHandler(servletContextHandler);
      }
    }

    try {
      server.start();

      LoggerManager.getLogger(JettyInitializingBean.class).info("Jetty service started...");
    } catch (Exception exception) {
      throw new JettyInitializationException(exception);
    }
  }

  /**
   * Normalizes a context or resource path to ensure a single leading slash and no trailing slash unless root.
   *
   * @param path the path to normalize
   * @return the normalized path or {@code null} if the input was {@code null}
   */
  private String normalizePath (String path) {

    if (path != null) {
      if (path.length() == 0) {

        return "/";
      } else if ((path.length() > 1) && path.endsWith("/")) {

        return (path.charAt(0) != '/') ? '/' + path.substring(0, path.length() - 1) : path.substring(0, path.length() - 1);
      } else {

        return (path.charAt(0) != '/') ? '/' + path : path;
      }
    }

    return null;
  }

  /**
   * Concatenates two path fragments, handling empty or root values gracefully.
   *
   * @param contextPath    the base context path
   * @param extensionPath  the path to append to the context
   * @return the combined path suitable for use as a Jetty context or URL pattern
   */
  private String combinePaths (String contextPath, String extensionPath) {

    return ((extensionPath == null) || (extensionPath.isEmpty()) || "/".equals(extensionPath)) ? contextPath : ((contextPath == null) || (contextPath.isEmpty()) || "/".equals(contextPath)) ? extensionPath : contextPath + extensionPath;
  }

  /**
   * Stops the Jetty server when the bean is destroyed, logging but ignoring shutdown errors.
   */
  @Override
  public synchronized void destroy () {

    if (server != null) {
      try {
        server.stop();
      } catch (Exception exception) {
        LoggerManager.getLogger(JettyInitializingBean.class).error(exception);
      }
    }
  }
}
