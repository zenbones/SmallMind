/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
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
package org.smallmind.web.jersey.ssl;

import java.net.URI;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

public class SSLJerseyTest {

  private static final Logger LOGGER = Logger.getLogger(JerseyTest.class.getName());
  private static Class<? extends TestContainerFactory> defaultTestContainerFactoryClass;
  private final DeploymentContext context;
  private final AtomicReference<Client> client = new AtomicReference((Object)null);
  private final Map<String, String> propertyMap = new HashMap();
  private final Map<String, String> forcedPropertyMap = new HashMap();
  private TestContainerFactory testContainerFactory;
  private TestContainer testContainer;

  public SSLJerseyTest () {

    this.context = this.configureDeployment();
    this.testContainerFactory = this.getTestContainerFactory();
  }

  public SSLJerseyTest (TestContainerFactory testContainerFactory) {

    this.context = this.configureDeployment();
    this.testContainerFactory = testContainerFactory;
  }

  public SSLJerseyTest (Application jaxrsApplication) {

    this.context = DeploymentContext.newInstance(jaxrsApplication);
    this.testContainerFactory = this.getTestContainerFactory();
  }

  private static String getSystemProperty (String propertyName) {

    Properties systemProperties = (Properties)AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
    return systemProperties.getProperty(propertyName);
  }

  private static synchronized TestContainerFactory getDefaultTestContainerFactory () {

    if (defaultTestContainerFactoryClass == null) {
      String factoryClassName = getSystemProperty("jersey.config.test.container.factory");
      if (factoryClassName != null) {
        LOGGER.log(Level.CONFIG, "Loading test container factory '{0}' specified in the '{1}' system property.", new Object[] {factoryClassName, "jersey.config.test.container.factory"});
        defaultTestContainerFactoryClass = loadFactoryClass(factoryClassName);
      } else {
        TestContainerFactory[] factories = (TestContainerFactory[])ServiceFinder.find(TestContainerFactory.class).toArray();
        if (factories.length > 0) {
          if (factories.length == 1) {
            defaultTestContainerFactoryClass = factories[0].getClass();
            LOGGER.log(Level.CONFIG, "Using the single found TestContainerFactory service provider '{0}'", defaultTestContainerFactoryClass.getName());
            return factories[0];
          }

          TestContainerFactory[] var2 = factories;
          int var3 = factories.length;

          for (int var4 = 0; var4 < var3; ++var4) {
            TestContainerFactory tcf = var2[var4];
            if ("org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory".equals(tcf.getClass().getName())) {
              defaultTestContainerFactoryClass = tcf.getClass();
              LOGGER.log(Level.CONFIG, "Found multiple TestContainerFactory service providers, using the default found '{0}'", "org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory");
              return tcf;
            }
          }

          defaultTestContainerFactoryClass = factories[0].getClass();
          LOGGER.log(Level.WARNING, "Found multiple TestContainerFactory service providers, using the first found '{0}'", defaultTestContainerFactoryClass.getName());
          return factories[0];
        }

        LOGGER.log(Level.CONFIG, "No TestContainerFactory configured, trying to load and instantiate the default implementation '{0}'", "org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory");
        defaultTestContainerFactoryClass = loadFactoryClass("org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory");
      }
    }

    try {
      return (TestContainerFactory)defaultTestContainerFactoryClass.newInstance();
    } catch (Exception var6) {
      throw new TestContainerException(String.format("Could not instantiate test container factory '%s'", defaultTestContainerFactoryClass.getName()), var6);
    }
  }

  private static Class<? extends TestContainerFactory> loadFactoryClass (String factoryClassName) {

    Class<Object> loadedClass = (Class)AccessController.doPrivileged(ReflectionHelper.classForNamePA(factoryClassName, (ClassLoader)null));
    if (loadedClass == null) {
      throw new TestContainerException(String.format("Test container factory class '%s' cannot be loaded", factoryClassName));
    } else {
      try {
        return loadedClass.asSubclass(TestContainerFactory.class);
      } catch (ClassCastException var4) {
        throw new TestContainerException(String.format("Class '%s' does not implement TestContainerFactory SPI.", factoryClassName), var4);
      }
    }
  }

  public static void closeIfNotNull (Client... clients) {

    if (clients != null && clients.length != 0) {
      Client[] var1 = clients;
      int var2 = clients.length;

      for (int var3 = 0; var3 < var2; ++var3) {
        Client c = var1[var3];
        if (c != null) {
          try {
            c.close();
          } catch (Throwable var6) {
            LOGGER.log(Level.WARNING, "Error closing a client instance.", var6);
          }
        }
      }
    }
  }

  TestContainer getTestContainer () {

    return this.testContainer;
  }

  TestContainer setTestContainer (TestContainer testContainer) {

    TestContainer old = this.testContainer;
    this.testContainer = testContainer;
    return old;
  }

  private TestContainer createTestContainer (DeploymentContext context) {

    return this.getTestContainerFactory().create(this.getBaseUri(), context);
  }

  protected final void enable (String featureName) {

    this.propertyMap.put(featureName, Boolean.TRUE.toString());
  }

  protected final void disable (String featureName) {

    this.propertyMap.put(featureName, Boolean.FALSE.toString());
  }

  protected final void forceEnable (String featureName) {

    this.forcedPropertyMap.put(featureName, Boolean.TRUE.toString());
  }

  protected final void forceDisable (String featureName) {

    this.forcedPropertyMap.put(featureName, Boolean.FALSE.toString());
  }

  protected final void set (String propertyName, Object value) {

    this.set(propertyName, value.toString());
  }

  protected final void set (String propertyName, String value) {

    this.propertyMap.put(propertyName, value);
  }

  protected final void forceSet (String propertyName, String value) {

    this.forcedPropertyMap.put(propertyName, value);
  }

  protected final boolean isEnabled (String propertyName) {

    return Boolean.valueOf(this.getProperty(propertyName));
  }

  private String getProperty (String propertyName) {

    if (this.forcedPropertyMap.containsKey(propertyName)) {
      return this.forcedPropertyMap.get(propertyName);
    } else {
      Properties systemProperties = AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
      if (systemProperties.containsKey(propertyName)) {
        return systemProperties.getProperty(propertyName);
      } else {
        return this.propertyMap.containsKey(propertyName) ? this.propertyMap.get(propertyName) : null;
      }
    }
  }

  protected Application configure () {

    throw new UnsupportedOperationException("The configure method must be implemented by the extending class");
  }

  protected DeploymentContext configureDeployment () {

    return DeploymentContext.builder(this.configure()).build();
  }

  protected TestContainerFactory getTestContainerFactory () throws TestContainerException {

    if (this.testContainerFactory == null) {
      this.testContainerFactory = getDefaultTestContainerFactory();
    }

    return this.testContainerFactory;
  }

  public final WebTarget target () {

    return this.client().target(this.getTestContainer().getBaseUri());
  }

  public final WebTarget target (String path) {

    return this.target().path(path);
  }

  public final Client client () {

    return this.getClient();
  }

  public void setUp () throws Exception {

    TestContainer testContainer = this.createTestContainer(this.context);
    this.setTestContainer(testContainer);
    testContainer.start();
    this.setClient(this.getClient(testContainer.getClientConfig()));
  }

  public void tearDown () throws Exception {

    try {
      TestContainer oldContainer = this.setTestContainer((TestContainer)null);
      if (oldContainer != null) {
        oldContainer.stop();
      }
    } finally {
      closeIfNotNull(this.setClient((Client)null));
    }
  }

  protected Client getClient () {

    return (Client)this.client.get();
  }

  protected Client setClient (Client client) {

    return (Client)this.client.getAndSet(client);
  }

  private Client getClient (ClientConfig clientConfig) {

    if (clientConfig == null) {
      clientConfig = new ClientConfig();
    }

    if (this.isEnabled("jersey.config.test.logging.enable")) {
      clientConfig.register(new LoggingFeature(LOGGER, this.isEnabled("jersey.config.test.logging.dumpEntity") ? LoggingFeature.Verbosity.PAYLOAD_ANY : LoggingFeature.Verbosity.HEADERS_ONLY));
    }

    this.configureClient(clientConfig);
    return ClientBuilder.newClient(clientConfig);
  }

  protected void configureClient (ClientConfig config) {

  }

  protected URI getBaseUri () {

    TestContainer container = this.getTestContainer();
    return container != null ? container.getBaseUri() : UriBuilder.fromUri("http://localhost/").port(this.getPort()).build(new Object[0]);
  }

  protected final int getPort () {

    TestContainer container = this.getTestContainer();
    if (container != null) {
      return container.getBaseUri().getPort();
    } else {
      String value = this.getProperty("jersey.config.test.container.port");
      if (value != null) {
        try {
          int i = Integer.parseInt(value);
          if (i < 0) {
            throw new NumberFormatException("Value not positive.");
          }

          return i;
        } catch (NumberFormatException var4) {
          LOGGER.log(Level.CONFIG, "Value of jersey.config.test.container.port property is not a valid positive integer [" + value + "]. Reverting to default [" + 9998 + "].", var4);
        }
      }

      return 9998;
    }
  }

  public final void close (Response... responses) {

    if (responses != null && responses.length != 0) {
      Response[] var2 = responses;
      int var3 = responses.length;

      for (int var4 = 0; var4 < var3; ++var4) {
        Response response = var2[var4];
        if (response != null) {
          try {
            response.close();
          } catch (Throwable var7) {
            LOGGER.log(Level.WARNING, "Error closing a response.", var7);
          }
        }
      }
    }
  }
}
