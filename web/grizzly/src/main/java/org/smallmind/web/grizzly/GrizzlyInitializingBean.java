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
package org.smallmind.web.grizzly;

import java.io.IOException;
import java.net.BindException;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.DispatcherType;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.jaxws.JaxwsHandler;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.web.grizzly.option.ClassLoaderResourceOption;
import org.smallmind.web.grizzly.option.DocumentRootOption;
import org.smallmind.web.grizzly.option.JaxRSOption;
import org.smallmind.web.grizzly.option.SpringSupportOption;
import org.smallmind.web.grizzly.option.WebSocketOption;
import org.smallmind.web.grizzly.tyrus.TyrusWebSocketAddOn;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.smallmind.web.jersey.spring.JerseyResourceConfig;
import org.smallmind.web.jersey.spring.ResourceConfigExtension;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class GrizzlyInitializingBean implements DisposableBean, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, BeanPostProcessor {

  private final LinkedList<WebSocketExtensionInstaller> webSocketExtensionInstallerList = new LinkedList<>();
  private final LinkedList<WebServiceInstaller> webServiceInstallerList = new LinkedList<>();
  private final LinkedList<ListenerInstaller> listenerInstallerList = new LinkedList<>();
  private final LinkedList<FilterInstaller> filterInstallerList = new LinkedList<>();
  private final LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();
  private HttpServer httpServer;
  private TyrusWebSocketAddOn tyrusWebSocketAddOn;
  private IOStrategy ioStrategy;
  private ResourceConfigExtension[] resourceConfigExtensions;
  private AddOn[] addOns;
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

  public void setIoStrategy (IOStrategy ioStrategy) {

    this.ioStrategy = ioStrategy;
  }

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setMaxHttpHeaderSize (Integer maxHttpHeaderSize) {

    this.maxHttpHeaderSize = maxHttpHeaderSize;
  }

  public void setInitialWorkerPoolSize (Integer initialWorkerPoolSize) {

    this.initialWorkerPoolSize = initialWorkerPoolSize;
  }

  public void setMaximumWorkerPoolSize (Integer maximumWorkerPoolSize) {

    this.maximumWorkerPoolSize = maximumWorkerPoolSize;
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

  public void setAddOns (AddOn[] addOns) {

    this.addOns = addOns;
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

  public void setAllowInsecure (boolean allowInsecure) {

    this.allowInsecure = allowInsecure;
  }

  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  @Override
  public synchronized void onApplicationEvent (ContextRefreshedEvent event) {

    NetworkListener insecureNetworkListener = null;
    NetworkListener secureNetworkListener = null;
    WebappContext webappContext;

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

    if ((addOns != null) && (addOns.length > 0)) {
      for (AddOn addOn : addOns) {
        for (NetworkListener networkListener : httpServer.getListeners()) {
          networkListener.registerAddOn(addOn);
        }
      }
    }

    webappContext = new WebappContext("Grizzly Application Context", contextPath);

    if (webSocketOption != null) {

      NetworkListener configuredNetworkListener = (secureNetworkListener != null) ? secureNetworkListener : insecureNetworkListener;

      configuredNetworkListener.registerAddOn(tyrusWebSocketAddOn = new TyrusWebSocketAddOn(httpServer.getServerConfiguration(), webappContext, combinePaths(contextPath, webSocketOption.getWebSocketPath()), true, null, webSocketExtensionInstallerList.toArray(new WebSocketExtensionInstaller[0])));
    }

    if (classLoaderResourceOption != null) {
      httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(classLoaderResourceOption.getStaticClassLoader(), "/"), combinePaths(contextPath, classLoaderResourceOption.getStaticPath()));
    }

    if (documentRootOption != null) {
      for (Map.Entry<String, String> documentRootEntry : documentRootOption.getDocumentRoots().entrySet()) {
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(PathUtility.asResourceString(Paths.get(documentRootEntry.getValue())), combinePaths(combinePaths(contextPath, documentRootOption.getDocumentPath()), normalizePath(documentRootEntry.getKey()))));
      }
    }

    for (WebServiceInstaller webServiceInstaller : webServiceInstallerList) {

      HttpHandler httpHandler = new JaxwsHandler(webServiceInstaller.getService(), Boolean.TRUE.equals(webServiceInstaller.getAsyncSupported()));

      httpServer.getServerConfiguration().addHttpHandler(httpHandler, combinePaths(combinePaths(contextPath, soapPath), normalizePath(webServiceInstaller.getPath())));
      LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed web service(%s)", webServiceInstaller.getService().getClass().getName());
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

    if (jaxRSOption != null) {
      webappContext.addServlet("JAX-RS Application", new ServletContainer(new JerseyResourceConfig(ExposedApplicationContext.getApplicationContext(), resourceConfigExtensions))).addMapping(jaxRSOption.getRestPath() + "/*");
      webappContext.addFilter("per-application-data", new PerApplicationContextFilter()).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), jaxRSOption.getRestPath() + "/*");
    }

    if (springSupportOption != null) {
      webappContext.addListener(new GrizzlyRequestContextListener());
    }

    for (ListenerInstaller listenerInstaller : listenerInstallerList) {
      try {

        Map<String, String> contextParameters;

        webappContext.addListener(listenerInstaller.getListener());
        if ((contextParameters = listenerInstaller.getContextParameters()) != null) {
          for (Map.Entry<String, String> parameterEntry : contextParameters.entrySet()) {
            webappContext.addContextInitParameter(parameterEntry.getKey(), parameterEntry.getValue());
          }
        }

        LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly installed listener(%s)", listenerInstaller.getListener().getClass().getName());
      } catch (Exception exception) {
        throw new GrizzlyInitializationException(exception);
      }
    }
    for (FilterInstaller filterInstaller : filterInstallerList) {
      try {

        FilterRegistration filterRegistration;
        Map<String, String> initParameters;
        String urlPattern;

        filterRegistration = webappContext.addFilter(filterInstaller.getDisplayName(), filterInstaller.getFilter());
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
    for (ServletInstaller servletInstaller : servletInstallerList) {
      try {

        ServletRegistration servletRegistration;
        Map<String, String> initParameters;
        String urlPattern;

        servletRegistration = webappContext.addServlet(servletInstaller.getDisplayName(), servletInstaller.getServlet());
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

    webappContext.deploy(httpServer);

    if (tyrusWebSocketAddOn != null) {
      tyrusWebSocketAddOn.doneDeployment();
      try {
        tyrusWebSocketAddOn.start(port);
      } catch (Exception exception) {
        throw new GrizzlyInitializationException(exception);
      }
    }

    LoggerManager.getLogger(GrizzlyInitializingBean.class).info("Grizzly service started...");
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
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

  @Override
  public Object postProcessAfterInitialization (Object bean, String beanName) {

    ServicePath servicePath;

    if (bean instanceof WebSocketExtensionInstaller) {
      webSocketExtensionInstallerList.add((WebSocketExtensionInstaller)bean);
    } else if (bean instanceof ListenerInstaller) {
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

    if (tyrusWebSocketAddOn != null) {
      tyrusWebSocketAddOn.stop();
    }
    if (httpServer != null) {
      httpServer.shutdown();
    }
  }

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

    return networkListener;
  }

  private NetworkListener generateSecureNetworkListener (SSLInfo sslInfo)
    throws IOException, ResourceException {

    NetworkListener secureListener = new NetworkListener("grizzly2Secure", host, sslInfo.getPort());
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
     *  authenticate to client - which we never want
     */
    SSLEngineConfigurator sslEngineConfig = new SSLEngineConfigurator(sslContextConfigurator.createSSLContext(true), false, sslInfo.isRequireClientAuth(), true);
    secureListener.setSSLEngineConfig(sslEngineConfig);

    return secureListener;
  }
}
