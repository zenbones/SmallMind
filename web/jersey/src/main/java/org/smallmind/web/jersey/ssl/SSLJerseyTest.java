/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/*
  @Override
  protected Client getClient ()
    throws NoSuchAlgorithmException, KeyManagementException {

    SSLContext ctx = SSLContext.getInstance("TLS");

    ctx.init(null, new TrustManager[] {new NaiveTrustManager()}, new SecureRandom());

    HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

    ClientBuilder clientBuilder = ClientBuilder.newBuilder();

      clientBuilder.sslContext(ctx);
      clientBuilder.hostnameVerifier(new NaiveHostNameVerifier());


    JERSEY_HTTPS_CLIENT = clientBuilder
                            .withConfig(new ClientConfig())
                            .register(JacksonFeature.class).build();
  }
 */

public abstract class SSLJerseyTest {

  private static final Logger LOGGER = Logger.getLogger(JerseyTest.class.getName());

  private static Class<? extends TestContainerFactory> defaultTestContainerFactoryClass;

  private final DeploymentContext context;
  private final AtomicReference<Client> client = new AtomicReference<>(null);

  private final Map<String, String> propertyMap = new HashMap<>();

  private final Map<String, String> forcedPropertyMap = new HashMap<>();
  private final Map<Logger, Level> logLevelMap = new IdentityHashMap<>();

  private TestContainerFactory testContainerFactory;

  private TestContainer testContainer;

  public SSLJerseyTest () {
    // Note: this must be the first call in the constructor to allow setting config
    // properties (especially around logging) in the configure() or configureDeployment()
    // method overridden in subclass, otherwise the properties set in the subclass would
    // not be set soon enough
    this.context = configureDeployment();
    this.testContainerFactory = getTestContainerFactory();
  }

  public SSLJerseyTest (final TestContainerFactory testContainerFactory) {
    // Note: this must be the first call in the constructor to allow setting config
    // properties (especially around logging) in the configure() or configureDeployment()
    // method overridden in subclass, otherwise the properties set in the subclass would
    // not be set soon enough
    this.context = configureDeployment();
    this.testContainerFactory = testContainerFactory;
  }

  public SSLJerseyTest (final Application jaxrsApplication) {

    this.context = DeploymentContext.newInstance(jaxrsApplication);
    this.testContainerFactory = getTestContainerFactory();
  }

  private static String getSystemProperty (final String propertyName) {

    final Properties systemProperties = AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
    return systemProperties.getProperty(propertyName);
  }

  private static synchronized TestContainerFactory getDefaultTestContainerFactory () {

    if (defaultTestContainerFactoryClass == null) {
      final String factoryClassName = getSystemProperty(TestProperties.CONTAINER_FACTORY);
      if (factoryClassName != null) {
        LOGGER.log(Level.CONFIG,
          "Loading test container factory '{0}' specified in the '{1}' system property.",
          new Object[] {factoryClassName, TestProperties.CONTAINER_FACTORY});

        defaultTestContainerFactoryClass = loadFactoryClass(factoryClassName);
      } else {
        final TestContainerFactory[] factories = ServiceFinder.find(TestContainerFactory.class).toArray();
        if (factories.length > 0) {
          // if there is only one factory instance, just return it
          if (factories.length == 1) {
            // cache the class for future reuse
            defaultTestContainerFactoryClass = factories[0].getClass();
            LOGGER.log(
              Level.CONFIG,
              "Using the single found TestContainerFactory service provider '{0}'",
              defaultTestContainerFactoryClass.getName());
            return factories[0];
          }

          // if default factory is present, use it.
          for (final TestContainerFactory tcf : factories) {
            if (TestProperties.DEFAULT_CONTAINER_FACTORY.equals(tcf.getClass().getName())) {
              // cache the class for future reuse
              defaultTestContainerFactoryClass = tcf.getClass();
              LOGGER.log(
                Level.CONFIG,
                "Found multiple TestContainerFactory service providers, using the default found '{0}'",
                TestProperties.DEFAULT_CONTAINER_FACTORY);
              return tcf;
            }
          }

          // default factory is not in the list - log warning and return the first found factory instance
          // cache the class for future reuse
          defaultTestContainerFactoryClass = factories[0].getClass();
          LOGGER.log(
            Level.WARNING,
            "Found multiple TestContainerFactory service providers, using the first found '{0}'",
            defaultTestContainerFactoryClass.getName());
          return factories[0];
        }

        LOGGER.log(
          Level.CONFIG,
          "No TestContainerFactory configured, trying to load and instantiate the default implementation '{0}'",
          TestProperties.DEFAULT_CONTAINER_FACTORY);
        defaultTestContainerFactoryClass = loadFactoryClass(TestProperties.DEFAULT_CONTAINER_FACTORY);
      }
    }

    try {
      return defaultTestContainerFactoryClass.newInstance();
    } catch (final Exception ex) {
      throw new TestContainerException(String.format(
        "Could not instantiate test container factory '%s'", defaultTestContainerFactoryClass.getName()), ex);
    }
  }

  private static Class<? extends TestContainerFactory> loadFactoryClass (final String factoryClassName) {

    Class<? extends TestContainerFactory> factoryClass;
    final Class<Object> loadedClass = AccessController.doPrivileged(ReflectionHelper.classForNamePA(factoryClassName, null));
    if (loadedClass == null) {
      throw new TestContainerException(String.format(
        "Test container factory class '%s' cannot be loaded", factoryClassName));
    }
    try {
      return loadedClass.asSubclass(TestContainerFactory.class);
    } catch (final ClassCastException ex) {
      throw new TestContainerException(String.format(
        "Class '%s' does not implement TestContainerFactory SPI.", factoryClassName), ex);
    }
  }

  public static void closeIfNotNull (final Client... clients) {

    if (clients == null || clients.length == 0) {
      return;
    }

    for (final Client c : clients) {
      if (c == null) {
        continue;
      }
      try {
        c.close();
      } catch (final Throwable t) {
        LOGGER.log(Level.WARNING, "Error closing a client instance.", t);
      }
    }
  }

  /* package */ TestContainer getTestContainer () {

    return testContainer;
  }

  /* package */ TestContainer setTestContainer (final TestContainer testContainer) {

    final TestContainer old = this.testContainer;
    this.testContainer = testContainer;
    return old;
  }

  private TestContainer createTestContainer (final DeploymentContext context) {

    return getTestContainerFactory().create(getBaseUri(), context);
  }

  protected final void enable (final String featureName) {
    // TODO: perhaps we could reuse the resource config for the test properties?
    propertyMap.put(featureName, Boolean.TRUE.toString());
  }

  protected final void disable (final String featureName) {

    propertyMap.put(featureName, Boolean.FALSE.toString());
  }

  protected final void forceEnable (final String featureName) {

    forcedPropertyMap.put(featureName, Boolean.TRUE.toString());
  }

  protected final void forceDisable (final String featureName) {

    forcedPropertyMap.put(featureName, Boolean.FALSE.toString());
  }

  protected final void set (final String propertyName, final Object value) {

    set(propertyName, value.toString());
  }

  protected final void set (final String propertyName, final String value) {

    propertyMap.put(propertyName, value);
  }

  protected final void forceSet (final String propertyName, final String value) {

    forcedPropertyMap.put(propertyName, value);
  }

  protected final boolean isEnabled (final String propertyName) {

    return Boolean.valueOf(getProperty(propertyName));
  }

  private String getProperty (final String propertyName) {

    if (forcedPropertyMap.containsKey(propertyName)) {
      return forcedPropertyMap.get(propertyName);
    }

    final Properties systemProperties = AccessController.doPrivileged(PropertiesHelper.getSystemProperties());
    if (systemProperties.containsKey(propertyName)) {
      return systemProperties.getProperty(propertyName);
    }

    if (propertyMap.containsKey(propertyName)) {
      return propertyMap.get(propertyName);
    }

    return null;
  }

  protected Application configure () {

    throw new UnsupportedOperationException("The configure method must be implemented by the extending class");
  }

  protected DeploymentContext configureDeployment () {

    return DeploymentContext.builder(configure()).build();
  }

  protected TestContainerFactory getTestContainerFactory ()
    throws TestContainerException {

    if (testContainerFactory == null) {
      testContainerFactory = getDefaultTestContainerFactory();
    }
    return testContainerFactory;
  }

  public final WebTarget target () {

    return client().target(getTestContainer().getBaseUri());
  }

  public final WebTarget target (final String path) {

    return target().path(path);
  }

  public final Client client () {

    return getClient();
  }

  @BeforeClass
  public void setUp ()
    throws Exception {

    final TestContainer testContainer = createTestContainer(context);

    // Set current instance of test container and start it.
    setTestContainer(testContainer);
    testContainer.start();

    // Create an set new client.
    setClient(getClient(testContainer.getClientConfig()));
  }

  @AfterClass
  public void tearDown ()
    throws Exception {

    try {
      TestContainer oldContainer = setTestContainer(null);
      if (oldContainer != null) {
        oldContainer.stop();
      }
    } finally {
      closeIfNotNull(setClient(null));
    }
  }

  protected Client getClient () {

    return client.get();
  }

  protected Client setClient (final Client client) {

    return this.client.getAndSet(client);
  }

  private Client getClient (ClientConfig clientConfig) {

    if (clientConfig == null) {
      clientConfig = new ClientConfig();
    }

    //check if logging is required
    if (isEnabled(TestProperties.LOG_TRAFFIC)) {
      clientConfig.register(new LoggingFeature(LOGGER, isEnabled(TestProperties.DUMP_ENTITY)
                                                         ? LoggingFeature.Verbosity.PAYLOAD_ANY
                                                         : LoggingFeature.Verbosity.HEADERS_ONLY));
    }

    configureClient(clientConfig);

    return ClientBuilder.newClient(clientConfig);
  }

  protected void configureClient (final ClientConfig config) {
    // do nothing
  }

  // TODO: make final
  protected URI getBaseUri () {

    final TestContainer container = getTestContainer();

    if (container != null) {
      // called from outside the JerseyTest constructor
      return container.getBaseUri();
    }

    // called from within JerseyTest constructor
    return UriBuilder.fromUri("http://localhost/").port(getPort()).build();
  }

  protected final int getPort () {

    final TestContainer container = getTestContainer();

    if (container != null) {
      // called from outside the JerseyTest constructor
      return container.getBaseUri().getPort();
    }

    // called from within JerseyTest constructor
    final String value = getProperty(TestProperties.CONTAINER_PORT);
    if (value != null) {

      try {
        final int i = Integer.parseInt(value);
        if (i < 0) {
          throw new NumberFormatException("Value not positive.");
        }
        return i;
      } catch (final NumberFormatException e) {
        LOGGER.log(Level.CONFIG,
          "Value of " + TestProperties.CONTAINER_PORT
            + " property is not a valid positive integer [" + value + "]."
            + " Reverting to default [" + TestProperties.DEFAULT_CONTAINER_PORT + "].",
          e
        );
      }
    }
    return TestProperties.DEFAULT_CONTAINER_PORT;
  }

  public final void close (final Response... responses) {

    if (responses != null) {
      for (final Response response : responses) {
        if (response == null) {
          continue;
        }
        try {
          response.close();
        } catch (final Throwable t) {
          LOGGER.log(Level.WARNING, "Error closing a response.", t);
        }
      }
    }
  }
}
