/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.jaxws.JaxwsHandler;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.servlet.ServletContainer;
import org.smallmind.nutsnbolts.lang.web.PerApplicationContextFilter;
import org.smallmind.nutsnbolts.resource.ResourceException;
import org.smallmind.web.jersey.jackson.JsonResourceConfig;
import org.smallmind.web.jersey.spring.ExposedApplicationContext;
import org.smallmind.web.jersey.spring.ResourceConfigExtension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class GrizzlyInitializingBean implements DisposableBean, ApplicationContextAware, ApplicationListener, BeanPostProcessor {

  private HttpServer httpServer;
  private LinkedList<WebService> serviceList = new LinkedList<>();
  private LinkedList<FilterInstaller> filterInstallerList = new LinkedList<>();
  private LinkedList<ListenerInstaller> listenerInstallerList = new LinkedList<>();
  private LinkedList<ServletInstaller> servletInstallerList = new LinkedList<>();
  private LinkedList<WebSocketApplicationInstaller> webSocketApplicationInstallerList = new LinkedList<>();
  private ResourceConfigExtension[] resourceConfigExtensions;
  private AddOn[] addOns;
  private File[] documentRoots;
  private SSLInfo sslInfo;
  private String host;
  private String contextPath = "/context";
  private String documentPath = "/document";
  private String staticPath = "/static";
  private String restPath = "/rest";
  private String soapPath = "/soap";
  private String webSocketPath = "/websocket";
  private int port = 80;
  private boolean allowInsecure = true;
  private boolean debug = false;

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setSslInfo (SSLInfo sslInfo) {

    this.sslInfo = sslInfo;
  }

  public void setContextPath (String contextPath) {

    this.contextPath = contextPath;
  }

  public void setDocumentPath (String documentPath) {

    this.documentPath = documentPath;
  }

  public void setStaticPath (String staticPath) {

    this.staticPath = staticPath;
  }

  public void setRestPath (String restPath) {

    this.restPath = restPath;
  }

  public void setSoapPath (String soapPath) {

    this.soapPath = soapPath;
  }

  public void setWebSocketPath (String webSocketPath) {

    this.webSocketPath = webSocketPath;
  }

  public void setDocumentRoots (File[] documentRoots) {

    this.documentRoots = documentRoots;
  }

  public void setResourceConfigExtensions (ResourceConfigExtension[] resourceConfigExtensions) {

    this.resourceConfigExtensions = resourceConfigExtensions;
  }

  public void setAddOns (AddOn[] addOns) {

    this.addOns = addOns;
  }

  public void setAllowInsecure (boolean allowInsecure) {

    this.allowInsecure = allowInsecure;
  }

  public void setDebug (boolean debug) {

    this.debug = debug;
  }

  @Override
  public synchronized void onApplicationEvent (ApplicationEvent event) {

    NetworkListener secureNetworkListener = null;

    if (event instanceof ContextRefreshedEvent) {

      if (debug) {
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
      }

      httpServer = new HttpServer();

      if (allowInsecure) {
        httpServer.addListener(configureNetworkListener(new NetworkListener("grizzly2", host, port)));
      }

      if (sslInfo != null) {
        try {
          httpServer.addListener(secureNetworkListener = configureNetworkListener(generateSecureNetworkListener(sslInfo)));
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }

      if ((addOns != null) && (addOns.length > 0)) {
        for (AddOn addOn : addOns)
          for (NetworkListener networkListener : httpServer.getListeners()) {
            networkListener.registerAddOn(addOn);
          }
      }

      httpServer.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(GrizzlyInitializingBean.class.getClassLoader(), "/"), staticPath);

      if ((documentRoots != null) && (documentRoots.length > 0)) {

        String[] absolutePaths = new String[documentRoots.length];

        for (int index = 0; index < documentRoots.length; index++) {
          absolutePaths[index] = documentRoots[index].getAbsolutePath();
        }

        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler(absolutePaths), documentPath);
      }

      WebappContext webappContext = new WebappContext("Grizzly Application Context", contextPath);
      webappContext.addServlet("JAX-RS Application", new ServletContainer(new JsonResourceConfig(ExposedApplicationContext.getApplicationContext(), resourceConfigExtensions))).addMapping(restPath + "/*");
      webappContext.addFilter("per-application-data", new PerApplicationContextFilter()).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), restPath + "/*");
      webappContext.addListener("org.springframework.web.context.request.RequestContextListener");

      for (FilterInstaller filterInstaller : filterInstallerList) {
        try {

          FilterRegistration filterRegistration;
          Map<String, String> initParameters;
          String urlPattern;

          filterRegistration = webappContext.addFilter(filterInstaller.getDisplayName(), filterInstaller.getFilter());
          filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), filterInstaller.isMatchAfter(), (urlPattern = filterInstaller.getUrlPattern()) == null ? "/" : urlPattern);
          if ((initParameters = filterInstaller.getInitParameters()) != null) {
            filterRegistration.setInitParameters(initParameters);
          }
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }
      for (ListenerInstaller listenerInstaller : listenerInstallerList) {
        try {
          webappContext.addListener(listenerInstaller.getListener());
          for (Map.Entry<String, String> parameterEntry : listenerInstaller.getContextParameters().entrySet()) {
            webappContext.addContextInitParameter(parameterEntry.getKey(), parameterEntry.getValue());
          }
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
          servletRegistration.addMapping((urlPattern = servletInstaller.getUrlPattern()) == null ? "/" : urlPattern);
          if ((initParameters = servletInstaller.getInitParameters()) != null) {
            servletRegistration.setInitParameters(initParameters);
          }
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }
      for (WebSocketApplicationInstaller webSocketApplicationInstaller : webSocketApplicationInstallerList) {
        try {
          WebSocketEngine.getEngine().register(webSocketPath, webSocketApplicationInstaller.getUrlPattern(), webSocketApplicationInstaller.getWebSocketApplication());
        } catch (Exception exception) {
          throw new GrizzlyInitializationException(exception);
        }
      }

      webappContext.deploy(httpServer);

      for (WebService webService : serviceList) {

        HttpHandler httpHandler = new JaxwsHandler(webService.getService(), false);

        httpServer.getServerConfiguration().addHttpHandler(httpHandler, soapPath + webService.getPath());
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
    }
  }

  @Override
  public void setApplicationContext (ApplicationContext applicationContext) {

    ExposedApplicationContext.register(applicationContext);
  }

  @Override
  public Object postProcessBeforeInitialization (Object bean, String beanName) throws BeansException {

    return bean;
  }

  @Override
  public Object postProcessAfterInitialization (Object bean, String beanName) throws BeansException {

    ServicePath servicePath;

    if (bean instanceof FilterInstaller) {
      filterInstallerList.add((FilterInstaller)bean);
    } else if (bean instanceof ListenerInstaller) {
      listenerInstallerList.add((ListenerInstaller)bean);
    } else if (bean instanceof ServletInstaller) {
      servletInstallerList.add((ServletInstaller)bean);
    } else if (bean instanceof WebSocketApplicationInstaller) {
      webSocketApplicationInstallerList.add((WebSocketApplicationInstaller)bean);
    } else if ((servicePath = bean.getClass().getAnnotation(ServicePath.class)) != null) {
      serviceList.add(new WebService(servicePath.value(), bean));
    }

    return bean;
  }

  @Override
  public synchronized void destroy () {

    if (httpServer != null) {
      httpServer.shutdown();
    }
  }

  private NetworkListener configureNetworkListener (NetworkListener networkListener) {

    Transport transport = networkListener.getTransport();

    transport.setIOStrategy(WorkerThreadIOStrategy.getInstance());
    transport.setWorkerThreadPoolConfig(ThreadPoolConfig.defaultConfig());

    return networkListener;
  }

  private NetworkListener generateSecureNetworkListener (SSLInfo sslInfo)
    throws IOException, ResourceException {

    NetworkListener secureListener = new NetworkListener("grizzly2Secure", host, sslInfo.getPort());
    SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();

    secureListener.setSecure(true);

    if (sslInfo.getKeySSLStore() != null) {
      sslContextConfigurator.setKeyStoreBytes(sslInfo.getKeySSLStore().getBytes());
      sslContextConfigurator.setKeyStorePass(sslInfo.getKeySSLStore().getPassword());
    }

    if (sslInfo.getTrustSSLStore() != null) {
      sslContextConfigurator.setTrustStoreBytes(sslInfo.getTrustSSLStore().getBytes());
      sslContextConfigurator.setTrustStorePass(sslInfo.getTrustSSLStore().getPassword());
    }

    if (!sslContextConfigurator.validateConfiguration(true)) {
      throw new GrizzlyInitializationException("Invalid ssl configuration");
    }

    /* Note: clientMode (2nd param) means server does not
    *  authenticate to client - which we never want
    */
    SSLEngineConfigurator sslEngineConfig = new SSLEngineConfigurator(sslContextConfigurator.createSSLContext(), false, sslInfo.isRequireClientAuth(), true);
    secureListener.setSSLEngineConfig(sslEngineConfig);

    return secureListener;
  }
}
