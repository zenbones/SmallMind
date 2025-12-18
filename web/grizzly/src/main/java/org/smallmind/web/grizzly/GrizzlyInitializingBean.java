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
import java.net.BindException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.http2.Http2AddOn;
import org.glassfish.grizzly.http2.Http2Configuration;
import org.glassfish.grizzly.jaxws.JaxwsHandler;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.grizzly.installer.FilterInstaller;
import org.smallmind.web.grizzly.installer.ListenerInstaller;
import org.smallmind.web.grizzly.installer.ServletInstaller;
import org.smallmind.web.grizzly.installer.WebServiceInstaller;
import org.smallmind.web.grizzly.installer.WebSocketExtensionInstaller;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.smallmind.web.grizzly.tyrus.TyrusGrizzlyServerContainer;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Spring lifecycle bean that bootstraps a Grizzly HTTP/HTTPS server and deploys servlet, JAX-WS, JAX-RS, Spring and
 * WebSocket components configured via {@link WebApplicationOption} and the various installer beans.
 */
public class GrizzlyInitializingBean implements GrizzlyWebAppStateLocator, InitializingBean, DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

  private final HashMap<String, GrizzlyWebAppState> webAppStateMap = new HashMap<>();
  private HttpServer httpServer;
  private IOStrategy ioStrategy;
  private ThreadPoolProbe threadPoolProbe;
  private ResourceConfig resourceConfig;
  private AddOn[] addOns;
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
   * @param ioStrategy custom Grizzly IO strategy to apply to the listeners; {@code null} keeps the default
   */
  public void setIoStrategy (IOStrategy ioStrategy) {

    this.ioStrategy = ioStrategy;
  }

  /**
   * @param threadPoolProbe optional probe for observing worker thread pools
   */
  public void setThreadPoolProbe (ThreadPoolProbe threadPoolProbe) {

    this.threadPoolProbe = threadPoolProbe;
  }

  /**
   * @param host host name or address Grizzly should bind to; {@code null} binds on all interfaces
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * @param port HTTP listener port when insecure connections are allowed
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * @param maxHttpHeaderSize maximum allowed HTTP header size; {@code null} retains Grizzly default
   */
  public void setMaxHttpHeaderSize (Integer maxHttpHeaderSize) {

    this.maxHttpHeaderSize = maxHttpHeaderSize;
  }

  /**
   * @param initialWorkerPoolSize lower bound for the worker pool
   */
  public void setInitialWorkerPoolSize (Integer initialWorkerPoolSize) {

    this.initialWorkerPoolSize = initialWorkerPoolSize;
  }

  /**
   * @param maximumWorkerPoolSize upper bound for the worker pool
   */
  public void setMaximumWorkerPoolSize (Integer maximumWorkerPoolSize) {

    this.maximumWorkerPoolSize = maximumWorkerPoolSize;
  }

  /**
   * @param sslInfo SSL/TLS configuration for secure listeners; {@code null} disables HTTPS
   */
  public void setSslInfo (SSLInfo sslInfo) {

    this.sslInfo = sslInfo;
  }

  /**
   * @param resourceConfig Jersey resource configuration used for REST deployment
   */
  public void setResourceConfig (ResourceConfig resourceConfig) {

    this.resourceConfig = resourceConfig;
  }

  /**
   * @param addOns additional Grizzly add-ons to apply to every listener
   */
  public void setAddOns (AddOn[] addOns) {

    this.addOns = addOns;
  }

  /**
   * @param webApplicationOptions per-context configuration objects describing what should be deployed
   */
  public void setWebApplicationOptions (WebApplicationOption[] webApplicationOptions) {

    this.webApplicationOptions = webApplicationOptions;
  }

  /**
   * @param allowInsecure whether an HTTP listener should be started alongside HTTPS
   */
  public void setAllowInsecure (boolean allowInsecure) {

    this.allowInsecure = allowInsecure;
  }

  /**
   * @param suppressConnectionClosedException true to silence broken pipe style exceptions in the request filter
   */
  public void setSuppressConnectionClosedException (boolean suppressConnectionClosedException) {

    this.suppressConnectionClosedException = suppressConnectionClosedException;
  }

  /**
   * @param debug enables verbose SOAP logging when true
   */
  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  /**
   * Validates context paths and seeds a {@link GrizzlyWebAppState} for each declared application option.
   *
   * @throws GrizzlyInitializationException if context paths are missing or duplicated
   */
  @Override
  public void afterPropertiesSet () {

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {
      if (webAppStateMap.containsKey(webApplicationOption.getContextPath())) {
        throw new GrizzlyInitializationException("Duplicate context paths(%s) are not allowed", webApplicationOption.getContextPath());
      } else {
        webAppStateMap.put(webApplicationOption.getContextPath(), new GrizzlyWebAppState(new WebappContext("Grizzly Application Context(" + webApplicationOption.getContextPath() + ")", webApplicationOption.getContextPath())));
      }
    }
  }

  /**
   * Locates the {@link GrizzlyWebAppState} matching the supplied context path.
   *
   * @param context servlet context path
   * @return the registered state for the context
   * @throws GrizzlyInitializationException when the path is missing or was never registered
   */
  @Override
  public synchronized GrizzlyWebAppState webAppStateFor (String context) {

    if ((context == null) || context.isEmpty()) {
      throw new GrizzlyInitializationException("Missing context path");
    } else {

      GrizzlyWebAppState webAppState;

      if ((webAppState = webAppStateMap.get(context)) == null) {
        throw new GrizzlyInitializationException("The context path(%s) was not properly initialized", context);
      } else {

        return webAppState;
      }
    }
  }

  /**
   * Starts the Grizzly server once the Spring context is refreshed, wiring HTTP/HTTPS listeners, REST, SOAP, Spring
   * listeners, filters, servlets and WebSocket endpoints according to the configured options.
   *
   * @param event Spring refreshed event trigger
   * @throws GrizzlyInitializationException on any startup/configuration failure
   */
  @Override
  public synchronized void onApplicationEvent (ContextRefreshedEvent event) {

    NetworkListener insecureNetworkListener = null;
    NetworkListener secureNetworkListener = null;

    if (debug) {
      System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
    }

    httpServer = new HttpServer();

    if (sslInfo != null) {
      try {
        httpServer.addListener(secureNetworkListener = configureNetworkListener(generateSecureNetworkListener(sslInfo)));
      } catch (Exception exception) {
        throw new GrizzlyInitializationException(exception);
      }
    }
    if (allowInsecure) {
      httpServer.addListener(insecureNetworkListener = configureNetworkListener(new NetworkListener("grizzly2", host, port)));
    } else if (sslInfo == null) {
      throw new GrizzlyInitializationException("Instance is not configured to allow insecure connection, and does not provide any ssl info");
    }

    Http2Configuration configuration = Http2Configuration.builder().build();
    Http2AddOn http2Addon = new Http2AddOn(configuration);

    for (NetworkListener networkListener : httpServer.getListeners()) {
      networkListener.registerAddOn(http2Addon);
    }

    if (addOns != null) {
      for (AddOn addOn : addOns) {
        for (NetworkListener networkListener : httpServer.getListeners()) {
          networkListener.registerAddOn(addOn);
        }
      }
    }

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {

      GrizzlyWebAppState webAppState = webAppStateFor(webApplicationOption.getContextPath());

      if (webApplicationOption.getWebSocketOption() != null) {

        NetworkListener configuredNetworkListener = (secureNetworkListener != null) ? secureNetworkListener : insecureNetworkListener;

        webAppState.setTyrusGrizzlyServerContainer(new TyrusGrizzlyServerContainer(httpServer, configuredNetworkListener, webAppState.getWebAppContext(), null, webApplicationOption.getWebSocketOption().isIncludeWsadlSupport(), null, webAppState.getWebSocketExtensionInstallerList().toArray(new WebSocketExtensionInstaller[0])));
      }

      if (webApplicationOption.getClassLoaderResourceOption() != null) {
        httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(webApplicationOption.getClassLoaderResourceOption().getStaticClassLoader(), "/"), combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getClassLoaderResourceOption().getStaticPath()));
      }

      if (webApplicationOption.getDocumentRootOption() != null) {
        for (Map.Entry<String, String> documentRootEntry : webApplicationOption.getDocumentRootOption().getDocumentRoots().entrySet()) {
          httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(PathUtility.asResourceString(Paths.get(documentRootEntry.getValue())), combinePaths(combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getDocumentRootOption().getDocumentPath()), normalizePath(documentRootEntry.getKey()))));
        }
      }

      for (WebServiceInstaller webServiceInstaller : webAppState.getWebServiceInstallerList()) {

        HttpHandler httpHandler = new JaxwsHandler(webServiceInstaller.getService(), Boolean.TRUE.equals(webServiceInstaller.getAsyncSupported()));

        httpServer.getServerConfiguration().addHttpHandler(httpHandler, combinePaths(combinePaths(webApplicationOption.getContextPath(), webApplicationOption.getSoapPath()), normalizePath(webServiceInstaller.getPath())));
        LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed web service(%s)", webServiceInstaller.getService().getClass().getName());
      }
    }

    try {
      httpServer.start();

      if (secureNetworkListener != null) {
        secureNetworkListener.getFilterChain().add(secureNetworkListener.getFilterChain().size() - 1, new ClientAuthProxyFilter(sslInfo.isProxyMode()));
      }
    } catch (IOException ioException) {
      if (!(ioException instanceof BindException)) {
        throw new GrizzlyInitializationException(ioException);
      }
    }

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {

      GrizzlyWebAppState webAppState = webAppStateFor(webApplicationOption.getContextPath());

      if (webApplicationOption.getJaxRSOption() != null) {
        webAppState.getWebAppContext().addServlet("JAX-RS Application", new ServletContainer(resourceConfig)).addMapping(webApplicationOption.getJaxRSOption().getRestPath() + "/*");
        webAppState.getWebAppContext().addFilter("per-application-data", new PerApplicationContextFilter(suppressConnectionClosedException)).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), webApplicationOption.getJaxRSOption().getRestPath() + "/*");
      }

      if (webApplicationOption.getSpringSupportOption() != null) {
        webAppState.getWebAppContext().addListener(new GrizzlyRequestContextListener());
      }

      if (webAppState.getTyrusGrizzlyServerContainer() != null) {
        webAppState.getTyrusGrizzlyServerContainer().doneDeployment();
        try {
          webAppState.getTyrusGrizzlyServerContainer().start();
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }

      for (ListenerInstaller listenerInstaller : webAppState.getListenerInstallerList()) {
        try {

          Map<String, String> contextParameters;

          webAppState.getWebAppContext().addListener(listenerInstaller.getListener());
          if ((contextParameters = listenerInstaller.getContextParameters()) != null) {
            for (Map.Entry<String, String> parameterEntry : contextParameters.entrySet()) {
              webAppState.getWebAppContext().addContextInitParameter(parameterEntry.getKey(), parameterEntry.getValue());
            }
          }

          LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed listener(%s)", listenerInstaller.getListener().getClass().getName());
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }
      for (FilterInstaller filterInstaller : webAppState.getFilterInstallerList()) {
        try {

          FilterRegistration filterRegistration;
          Map<String, String> initParameters;
          String urlPattern;

          filterRegistration = webAppState.getWebAppContext().addFilter(filterInstaller.getDisplayName(), filterInstaller.getFilter());
          filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), filterInstaller.isMatchAfter(), ((urlPattern = filterInstaller.getUrlPattern()) == null) ? "/*" : urlPattern);
          if (filterInstaller.getAsyncSupported() != null) {
            filterRegistration.setAsyncSupported(filterInstaller.getAsyncSupported());
          }
          if ((initParameters = filterInstaller.getInitParameters()) != null) {
            filterRegistration.setInitParameters(initParameters);
          }

          LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed filter(%s)", filterInstaller.getDisplayName());
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }
      for (ServletInstaller servletInstaller : webAppState.getServletInstallerList()) {
        try {

          ServletRegistration servletRegistration;
          Map<String, String> initParameters;
          String urlPattern;

          servletRegistration = webAppState.getWebAppContext().addServlet(servletInstaller.getDisplayName(), servletInstaller.getServlet());
          servletRegistration.addMapping(((urlPattern = servletInstaller.getUrlPattern()) == null) ? "/*" : urlPattern);
          if (servletInstaller.getLoadOnStartup() != null) {
            servletRegistration.setLoadOnStartup(servletInstaller.getLoadOnStartup());
          }
          if (servletInstaller.getAsyncSupported() != null) {
            servletRegistration.setAsyncSupported(servletInstaller.getAsyncSupported());
          }
          if ((initParameters = servletInstaller.getInitParameters()) != null) {
            servletRegistration.setInitParameters(initParameters);
          }

          LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed servlet(%s)", servletInstaller.getDisplayName());
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }

      webAppState.getWebAppContext().deploy(httpServer);
    }

    LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly service started...");
  }

  /**
   * Normalizes a servlet path to ensure leading slash and remove trailing slash where appropriate.
   *
   * @param path raw path fragment which may be {@code null}
   * @return normalized path or {@code null} if input is {@code null}
   */
  private String normalizePath (String path) {

    if (path != null) {
      if (path.isEmpty()) {

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
   * Concatenates a base context path with an extension path while handling default root representations.
   *
   * @param contextPath   the servlet context path
   * @param extensionPath an additional path fragment to append
   * @return the normalized combined path
   */
  private String combinePaths (String contextPath, String extensionPath) {

    return ((extensionPath == null) || (extensionPath.isEmpty()) || "/".equals(extensionPath)) ? contextPath : ((contextPath == null) || (contextPath.isEmpty()) || "/".equals(contextPath)) ? extensionPath : contextPath + extensionPath;
  }

  /**
   * Captures the Spring {@link ApplicationContext} in a globally accessible wrapper for later lookup.
   *
   * @param applicationContext active Spring context
   */
  @Override
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

  /**
   * Stops deployed WebSocket containers and shuts down the Grizzly server.
   */
  @Override
  public synchronized void destroy () {

    for (WebApplicationOption webApplicationOption : webApplicationOptions) {

      GrizzlyWebAppState webAppState;

      if ((webAppState = webAppStateMap.get(webApplicationOption.getContextPath())) != null) {
        if (webAppState.getTyrusGrizzlyServerContainer() != null) {
          webAppState.getTyrusGrizzlyServerContainer().stop();
        }
      }
    }

    if (httpServer != null) {
      httpServer.shutdown();
    }
  }

  /**
   * Applies optional IO, header size, worker pool and monitoring configuration to the supplied listener.
   *
   * @param networkListener the listener to configure
   * @return the same listener instance for chaining
   */
  private NetworkListener configureNetworkListener (NetworkListener networkListener) {

    if (maxHttpHeaderSize != null) {
      networkListener.setMaxHttpHeaderSize(maxHttpHeaderSize);
    }
    if (ioStrategy != null) {
      networkListener.getTransport().setIOStrategy(ioStrategy);
    }
    if (initialWorkerPoolSize != null) {
      networkListener.getTransport().getWorkerThreadPoolConfig().setCorePoolSize(initialWorkerPoolSize);
    }
    if (maximumWorkerPoolSize != null) {
      networkListener.getTransport().getWorkerThreadPoolConfig().setMaxPoolSize(maximumWorkerPoolSize);
    }
    if (threadPoolProbe != null) {
      networkListener.getTransport().getWorkerThreadPoolConfig().getInitialMonitoringConfig().addProbes(threadPoolProbe);
    }

    return networkListener;
  }

  /**
   * Builds an HTTPS {@link NetworkListener} according to the supplied SSL configuration.
   *
   * @param sslInfo SSL configuration including keystore and truststore
   * @return a secure listener instance
   * @throws IOException        if key material cannot be read
   * @throws ResourceException  if secure stores cannot supply credential bytes
   */
  private NetworkListener generateSecureNetworkListener (SSLInfo sslInfo)
    throws IOException, ResourceException {

    NetworkListener secureListener = new NetworkListener("grizzlySecure", host, sslInfo.getPort());
    SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();

    secureListener.setSecure(true);

    if (sslInfo.getKeySecureStore() != null) {
      sslContextConfigurator.setKeyStoreBytes(sslInfo.getKeySecureStore().getBytes());
      sslContextConfigurator.setKeyStorePass(sslInfo.getKeySecureStore().getPassword());
    }

    if (sslInfo.getTrustSecureStore() != null) {
      sslContextConfigurator.setTrustStoreBytes(sslInfo.getTrustSecureStore().getBytes());
      sslContextConfigurator.setTrustStorePass(sslInfo.getTrustSecureStore().getPassword());
    }

    /* Note: clientMode (2nd param) means server does not
     *  authenticate the client - which we never want
     */
    SSLEngineConfigurator sslEngineConfig = new SSLEngineConfigurator(sslContextConfigurator.createSSLContext(true), false, sslInfo.isRequireClientAuth(), true);
    secureListener.setSSLEngineConfig(sslEngineConfig);

    return secureListener;
  }
}
