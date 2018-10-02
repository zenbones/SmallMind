/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.persistence.orm.spring.jdo.antique;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.jdo.JDOException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.util.CollectionUtils;

public class LocalPersistenceManagerFactoryBean implements FactoryBean<PersistenceManagerFactory>,
                                                             BeanClassLoaderAware, InitializingBean, DisposableBean, PersistenceExceptionTranslator {

  private final Map<String, Object> jdoPropertyMap = new HashMap<String, Object>();
  protected final Log logger = LogFactory.getLog(getClass());
  private String persistenceManagerFactoryName;
  private Resource configLocation;
  private ClassLoader beanClassLoader;

  private PersistenceManagerFactory persistenceManagerFactory;

  private JdoDialect jdoDialect;

  /**
   * Specify the name of the desired PersistenceManagerFactory.
   * <p>This may either be a properties resource in the classpath if such a resource
   * exists, or a PMF definition with that name from "META-INF/jdoconfig.xml",
   * or a JPA EntityManagerFactory cast to a PersistenceManagerFactory based on the
   * persistence-unit name from "META-INF/persistence.xml" (JPA).
   * <p>Default is none: Either 'persistenceManagerFactoryName' or 'configLocation'
   * or 'jdoProperties' needs to be specified.
   */
  public void setPersistenceManagerFactoryName (String persistenceManagerFactoryName) {

    this.persistenceManagerFactoryName = persistenceManagerFactoryName;
  }

  /**
   * Set the location of the JDO properties config file, for example
   * as classpath resource "classpath:kodo.properties".
   * <p>Note: Can be omitted when all necessary properties are
   * specified locally via this bean.
   */
  public void setConfigLocation (Resource configLocation) {

    this.configLocation = configLocation;
  }

  /**
   * Set JDO properties, such as"javax.jdo.PersistenceManagerFactoryClass".
   * <p>Can be used to override values in a JDO properties config file,
   * or to specify all necessary properties locally.
   * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
   * or a "props" element in XML bean definitions.
   */
  public void setJdoProperties (Properties jdoProperties) {

    CollectionUtils.mergePropertiesIntoMap(jdoProperties, this.jdoPropertyMap);
  }

  /**
   * Allow Map access to the JDO properties to be passed to the JDOHelper,
   * with the option to add or override specific entries.
   * <p>Useful for specifying entries directly, for example via
   * "jdoPropertyMap[myKey]".
   */
  public Map<String, Object> getJdoPropertyMap () {

    return this.jdoPropertyMap;
  }

  /**
   * Specify JDO properties as a Map, to be passed into
   * {@code JDOHelper.getPersistenceManagerFactory} (if any).
   * <p>Can be populated with a "map" or "props" element in XML bean definitions.
   */
  public void setJdoPropertyMap (Map<String, Object> jdoProperties) {

    if (jdoProperties != null) {
      this.jdoPropertyMap.putAll(jdoProperties);
    }
  }

  /**
   * Set the JDO dialect to use for the PersistenceExceptionTranslator
   * functionality of this factory.
   * <p>Default is a DefaultJdoDialect based on the PersistenceManagerFactory's
   * underlying DataSource, if any.
   */
  public void setJdoDialect (JdoDialect jdoDialect) {

    this.jdoDialect = jdoDialect;
  }

  @Override
  public void setBeanClassLoader (ClassLoader beanClassLoader) {

    this.beanClassLoader = beanClassLoader;
  }

  /**
   * Initialize the PersistenceManagerFactory for the given location.
   */
  @Override
  public void afterPropertiesSet () throws IllegalArgumentException, IOException, JDOException {

    if (this.persistenceManagerFactoryName != null) {
      if (this.configLocation != null || !this.jdoPropertyMap.isEmpty()) {
        throw new IllegalStateException("'configLocation'/'jdoProperties' not supported in " +
                                          "combination with 'persistenceManagerFactoryName' - specify one or the other, not both");
      }
      if (logger.isInfoEnabled()) {
        logger.info("Building new JDO PersistenceManagerFactory for name '" +
                      this.persistenceManagerFactoryName + "'");
      }
      this.persistenceManagerFactory = newPersistenceManagerFactory(this.persistenceManagerFactoryName);
    } else {
      Map<String, Object> mergedProps = new HashMap<String, Object>();
      if (this.configLocation != null) {
        if (logger.isInfoEnabled()) {
          logger.info("Loading JDO config from [" + this.configLocation + "]");
        }
        CollectionUtils.mergePropertiesIntoMap(
          PropertiesLoaderUtils.loadProperties(this.configLocation), mergedProps);
      }
      mergedProps.putAll(this.jdoPropertyMap);
      logger.info("Building new JDO PersistenceManagerFactory");
      this.persistenceManagerFactory = newPersistenceManagerFactory(mergedProps);
    }

    // Build default JdoDialect if none explicitly specified.
    if (this.jdoDialect == null) {
      this.jdoDialect = new DefaultJdoDialect(this.persistenceManagerFactory.getConnectionFactory());
    }
  }

  /**
   * Subclasses can override this to perform custom initialization of the
   * PersistenceManagerFactory instance, creating it for the specified name.
   * <p>The default implementation invokes JDOHelper's
   * {@code getPersistenceManagerFactory(String)} method.
   * A custom implementation could prepare the instance in a specific way,
   * or use a custom PersistenceManagerFactory implementation.
   */
  protected PersistenceManagerFactory newPersistenceManagerFactory (String name) {

    return JDOHelper.getPersistenceManagerFactory(name, this.beanClassLoader);
  }

  /**
   * Subclasses can override this to perform custom initialization of the
   * PersistenceManagerFactory instance, creating it via the given Properties
   * that got prepared by this LocalPersistenceManagerFactoryBean.
   * <p>The default implementation invokes JDOHelper's
   * {@code getPersistenceManagerFactory(Map)} method.
   * A custom implementation could prepare the instance in a specific way,
   * or use a custom PersistenceManagerFactory implementation.
   */
  protected PersistenceManagerFactory newPersistenceManagerFactory (Map<?, ?> props) {

    return JDOHelper.getPersistenceManagerFactory(props, this.beanClassLoader);
  }

  /**
   * Return the singleton PersistenceManagerFactory.
   */
  @Override
  public PersistenceManagerFactory getObject () {

    return this.persistenceManagerFactory;
  }

  @Override
  public Class<? extends PersistenceManagerFactory> getObjectType () {

    return (this.persistenceManagerFactory != null ?
              this.persistenceManagerFactory.getClass() : PersistenceManagerFactory.class);
  }

  @Override
  public boolean isSingleton () {

    return true;
  }

  /**
   * Implementation of the PersistenceExceptionTranslator interface,
   * as autodetected by Spring's PersistenceExceptionTranslationPostProcessor.
   * <p>Converts the exception if it is a JDOException, preferably using a specified
   * JdoDialect. Else returns {@code null} to indicate an unknown exception.
   */
  @Override
  public DataAccessException translateExceptionIfPossible (RuntimeException ex) {

    if (ex instanceof JDOException) {
      if (this.jdoDialect != null) {
        return this.jdoDialect.translateException((JDOException)ex);
      } else {
        return PersistenceManagerFactoryUtils.convertJdoAccessException((JDOException)ex);
      }
    }
    return null;
  }

  /**
   * Close the PersistenceManagerFactory on bean factory shutdown.
   */
  @Override
  public void destroy () {

    logger.info("Closing JDO PersistenceManagerFactory");
    this.persistenceManagerFactory.close();
  }
}
