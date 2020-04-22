/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.xml.ws.Endpoint;
import com.sun.net.httpserver.HttpContext;
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
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.smallmind.web.jersey.spring.JerseyResourceConfig;
import org.smallmind.web.jersey.spring.ResourceConfigExtension;
import org.smallmind.web.jetty.option.ClassLoaderResourceOption;
import org.smallmind.web.jetty.option.DocumentRootOption;
import org.smallmind.web.jetty.option.JaxRSOption;
import org.smallmind.web.jetty.option.SpringSupportOption;
import org.smallmind.web.jetty.option.WebSocketOption;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class JettyInitializingBean implements DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanPostProcessor {

  private Server server;
  private LinkedList<WebServiceInstaller> webServiceInstallerList = new LinkedList<>();
  private LinkedList<ListenerInstaller> listenerInstallerList = new LinkedList<>();
  private LinkedList<FilterInstaller> filterInstallerList = new LinkedList<>();
  private LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();

  private ResourceConfigExtension[] resourceConfigExtensions;
  private ClassLoaderResourceOption classLoaderResourceOption;
  private DocumentRootOption documentRootOption;
  private JaxRSOption jaxRSOption;
  private SpringSupportOption springSupportOption;
  private WebSocketOption webSocketOption;
  private SSLInfo sslInfo;
  private String host;
  private String contextPath = "/context";
  private String soapPath = "/soap";
  private Integer maxHttpHeaderSize;
  private Integer initialWorkerPoolSize;
  private Integer maximumWorkerPoolSize;
  private int port = 80;
  private boolean allowInsecure = true;
  private boolean debug = false;

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setClassLoaderResourceOption (ClassLoaderResourceOption classLoaderResourceOption) {

    this.classLoaderResourceOption = classLoaderResourceOption;
  }

  public void setDocumentRootOption (DocumentRootOption documentRootOption) {

    this.documentRootOption = documentRootOption;
  }

  public void setJaxRSOption (JaxRSOption jaxRSOption) {

    this.jaxRSOption = jaxRSOption;
  }

  public void setSpringSupportOption (SpringSupportOption springSupportOption) {

    this.springSupportOption = springSupportOption;
  }

  public void setWebSocketOption (WebSocketOption webSocketOption) {

    this.webSocketOption = webSocketOption;
  }

  public void setSslInfo (SSLInfo sslInfo) {

    this.sslInfo = sslInfo;
  }

  public void setContextPath (String contextPath) {

    this.contextPath = normalizePath(contextPath);
  }

  public void setSoapPath (String soapPath) {

    this.soapPath = normalizePath(soapPath);
  }

  public void setResourceConfigExtensions (ResourceConfigExtension[] resourceConfigExtensions) {

    this.resourceConfigExtensions = resourceConfigExtensions;
  }

  public void setInitialWorkerPoolSize (Integer initialWorkerPoolSize) {

    this.initialWorkerPoolSize = initialWorkerPoolSize;
  }

  public void setMaximumWorkerPoolSize (Integer maximumWorkerPoolSize) {

    this.maximumWorkerPoolSize = maximumWorkerPoolSize;
  }

  public void setMaxHttpHeaderSize (Integer maxHttpHeaderSize) {

    this.maxHttpHeaderSize = maxHttpHeaderSize;
  }

  public void setAllowInsecure (boolean allowInsecure) {

    this.allowInsecure = allowInsecure;
  }

  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  @Override
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

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
      SslContextFactory sslContextFactory = new SslContextFactory.Server();
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

    if (classLoaderResourceOption != null) {

      ContextHandler staticContextHandler = new ContextHandler(combinePaths(contextPath, classLoaderResourceOption.getStaticPath()));
      ResourceHandler staticResourceHandler = new ResourceHandler();

      staticResourceHandler.setBaseResource(Resource.newClassPathResource("/"));
      staticContextHandler.setHandler(staticResourceHandler);
      contextHandlerCollection.addHandler(staticContextHandler);
    }

    if (documentRootOption != null) {
      for (Map.Entry<String, String> documentRootEntry : documentRootOption.getDocumentRoots().entrySet()) {

        ContextHandler documentContextHandler = new ContextHandler(combinePaths(combinePaths(contextPath, documentRootOption.getDocumentPath()), normalizePath(documentRootEntry.getKey())));
        ResourceHandler documentResourceHandler = new ResourceHandler();

        documentResourceHandler.setBaseResource(new PathResource(Paths.get(documentRootEntry.getValue())));
        documentContextHandler.setHandler(documentContextHandler);

        contextHandlerCollection.addHandler(documentContextHandler);
      }
    }

    if (!webServiceInstallerList.isEmpty()) {

      JettyHttpServer jettyHttpServer = new JettyHttpServer(server, true);

      for (WebServiceInstaller webServiceInstaller : webServiceInstallerList) {

        HttpContext httpContext = jettyHttpServer.createContext(combinePaths(combinePaths(contextPath, soapPath), normalizePath(webServiceInstaller.getPath())));
        Endpoint endpoint = Endpoint.create(webServiceInstaller.getService());

        endpoint.publish(httpContext);
      }
    }

    if ((!listenerInstallerList.isEmpty()) || (!filterInstallerList.isEmpty()) || (!servletInstallerList.isEmpty()) || (jaxRSOption != null) || (springSupportOption != null) || (webSocketOption != null)) {

      ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);

      servletContextHandler.setContextPath(contextPath);

      for (ListenerInstaller listenerInstaller : listenerInstallerList) {
        try {
          servletContextHandler.addEventListener(listenerInstaller.getListener());
        } catch (IllegalAccessException | InstantiationException exception) {
          throw new JettyInitializationException(exception);
        }
      }
      for (FilterInstaller filterInstaller : filterInstallerList) {
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
      for (ServletInstaller servletInstaller : servletInstallerList) {
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

      if (jaxRSOption != null) {

        ServletHolder jerseyServletHolder = new ServletHolder(new ServletContainer(new JerseyResourceConfig(ExposedApplicationContext.getApplicationContext(), resourceConfigExtensions)));
        FilterHolder jerseyFilterHolder = new FilterHolder(new PerApplicationContextFilter());

        jerseyServletHolder.setName("JAX-RS Application");
        jerseyServletHolder.setDisplayName("JAX-RS Application");

        jerseyFilterHolder.setName("per-application-data");
        jerseyFilterHolder.setDisplayName("per-application-data");

        servletContextHandler.addServlet(jerseyServletHolder, jaxRSOption.getRestPath() + "/*");
        servletContextHandler.addFilter(jerseyFilterHolder, jaxRSOption.getRestPath() + "/*", EnumSet.of(DispatcherType.REQUEST));
      }

      if (springSupportOption != null) {
        servletContextHandler.addEventListener(new JettyRequestContextListener());
      }

      if (webSocketOption != null) {
        try {
          WebSocketServerContainerInitializer.initialize(servletContextHandler).setDefaultMaxSessionIdleTimeout(webSocketOption.getMaxSessionIdleTimeout());
        } catch (ServletException servletException) {
          throw new JettyInitializationException(servletException);
        }
      }

      contextHandlerCollection.addHandler(servletContextHandler);
    }

    try {
      server.start();

      LoggerManager.getLogger(JettyInitializingBean.class).info("Jetty service started...");
    } catch (Exception exception) {
      throw new JettyInitializationException(exception);
    }
  }

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

  private String combinePaths (String contextPath, String extensionPath) {

    return ((extensionPath == null) || (extensionPath.isEmpty()) || "/".equals(extensionPath)) ? contextPath : ((contextPath == null) || (contextPath.isEmpty()) || "/".equals(contextPath)) ? extensionPath : contextPath + extensionPath;
  }

  @Override
  public Object postProcessAfterInitialization (Object bean, String beanName) {

    ServicePath servicePath;

    if (bean instanceof ListenerInstaller) {
      listenerInstallerList.add((ListenerInstaller)bean);
    } else if (bean instanceof FilterInstaller) {
      filterInstallerList.add((FilterInstaller)bean);
    } else if (bean instanceof ServletInstaller) {
      servletInstallerList.add((ServletInstaller)bean);
    } else if ((servicePath = bean.getClass().getAnnotation(ServicePath.class)) != null) {
      webServiceInstallerList.add(new WebServiceInstaller(servicePath.value(), bean));
    }

    return bean;
  }

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