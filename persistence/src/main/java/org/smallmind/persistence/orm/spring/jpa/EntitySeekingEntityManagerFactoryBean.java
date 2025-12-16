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
package org.smallmind.persistence.orm.spring.jpa;

import javax.sql.DataSource;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import org.springframework.beans.BeanUtils;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.ClassUtils;

/**
 * {@link AbstractEntityManagerFactoryBean} that populates a {@link MutablePersistenceUnitInfo} with
 * entity classes discovered by {@link AnnotationSeekingBeanFactoryPostProcessor}. This allows
 * programmatic configuration of a JPA {@link EntityManagerFactory} without a persistence.xml file.
 */
public class EntitySeekingEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

  private AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor;
  private DataSource dataSource;
  private DataSource jtaDataSource;
  private SharedCacheMode sharedCacheMode;
  private CacheStoreMode cacheStoreMode;
  private CacheRetrieveMode cacheRetrieveMode;
  private ValidationMode validationMode;
  private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;
  private MutablePersistenceUnitInfo persistenceUnitInfo;
  private String sessionSourceKey;
  private boolean excludeUnlistedClasses = false;

  /**
   * Supplies the helper that collects annotated entity classes from DAO definitions.
   *
   * @param annotationSeekingBeanFactoryPostProcessor the post-processor that caches discovered entities
   */
  public void setAnnotationSeekingBeanFactoryPostProcessor (AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor) {

    this.annotationSeekingBeanFactoryPostProcessor = annotationSeekingBeanFactoryPostProcessor;
  }

  /**
   * Restricts discovery to entities associated with a specific {@link org.smallmind.persistence.orm.SessionSource}.
   *
   * @param sessionSourceKey session source key or {@code null} for the default set
   */
  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  /**
   * Sets the non-JTA {@link DataSource} for RESOURCE_LOCAL transactions.
   *
   * @param dataSource JDBC data source
   */
  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  /**
   * Sets the JTA {@link DataSource} for container-managed transactions.
   *
   * @param jtaDataSource JTA data source
   */
  public void setJtaDataSource (DataSource jtaDataSource) {

    this.jtaDataSource = jtaDataSource;
  }

  /**
   * Sets the shared cache mode applied to the persistence unit.
   *
   * @param sharedCacheMode shared cache mode value
   */
  public void setSharedCacheMode (SharedCacheMode sharedCacheMode) {

    this.sharedCacheMode = sharedCacheMode;
  }

  /**
   * Configures the default cache store mode for the persistence unit.
   *
   * @param cacheStoreMode cache store mode
   */
  public void setCacheStoreMode (CacheStoreMode cacheStoreMode) {

    this.cacheStoreMode = cacheStoreMode;
  }

  /**
   * Configures the default cache retrieve mode for the persistence unit.
   *
   * @param cacheRetrieveMode cache retrieve mode
   */
  public void setCacheRetrieveMode (CacheRetrieveMode cacheRetrieveMode) {

    this.cacheRetrieveMode = cacheRetrieveMode;
  }

  /**
   * Sets the bean validation mode.
   *
   * @param validationMode validation mode
   */
  public void setValidationMode (ValidationMode validationMode) {

    this.validationMode = validationMode;
  }

  /**
   * Provides a pre-built {@link MutablePersistenceUnitInfo} to populate. If not set one is
   * created during initialization.
   *
   * @param persistenceUnitInfo mutable persistence unit descriptor
   */
  public void setPersistenceUnitInfo (MutablePersistenceUnitInfo persistenceUnitInfo) {

    this.persistenceUnitInfo = persistenceUnitInfo;
  }

  /**
   * Indicates whether to ignore classes found on the classpath that are not explicitly listed.
   *
   * @param excludeUnlistedClasses flag matching the persistence.xml attribute
   */
  public void setExcludeUnlistedClasses (boolean excludeUnlistedClasses) {

    this.excludeUnlistedClasses = excludeUnlistedClasses;
  }

  /**
   * Registers additional {@link PersistenceUnitPostProcessor}s to customize the persistence unit.
   *
   * @param persistenceUnitPostProcessors post-processors to apply
   */
  public void setPersistenceUnitPostProcessors (PersistenceUnitPostProcessor... persistenceUnitPostProcessors) {

    this.persistenceUnitPostProcessors = persistenceUnitPostProcessors;
  }

  /**
   * Builds the {@link EntityManagerFactory}, enriching the persistence unit with discovered entity
   * classes and supplied configuration such as data sources, cache settings, and validation mode.
   *
   * @return the initialized entity manager factory
   * @throws PersistenceException if creation fails or no provider can be resolved
   */
  @Override
  protected EntityManagerFactory createNativeEntityManagerFactory ()
    throws PersistenceException {

    PersistenceProvider provider;

    if (persistenceUnitInfo == null) {
      persistenceUnitInfo = new MutablePersistenceUnitInfo();
    }

    persistenceUnitInfo.setExcludeUnlistedClasses(excludeUnlistedClasses);
    if (annotationSeekingBeanFactoryPostProcessor != null) {
      for (Class<?> entityClass : annotationSeekingBeanFactoryPostProcessor.getAnnotatedClasses(sessionSourceKey)) {
        persistenceUnitInfo.addManagedClassName(entityClass.getName());
      }
    }

    if (dataSource != null) {
      persistenceUnitInfo.setNonJtaDataSource(dataSource);
      persistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    }
    if (jtaDataSource != null) {
      persistenceUnitInfo.setJtaDataSource(dataSource);
      persistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.JTA);
    }
    if (sharedCacheMode != null) {
      persistenceUnitInfo.setSharedCacheMode(sharedCacheMode);
    }
    if (cacheStoreMode != null) {
      persistenceUnitInfo.getProperties().setProperty("jakarta.persistence.cache.storeMode", cacheStoreMode.name());
    }
    if (cacheRetrieveMode != null) {
      persistenceUnitInfo.getProperties().setProperty("jakarta.persistence.cache.retrieveMode", cacheRetrieveMode.name());
    }
    if (validationMode != null) {
      persistenceUnitInfo.setValidationMode(validationMode);
    }

    if (getJpaVendorAdapter() != null) {
      persistenceUnitInfo.setPersistenceProviderPackageName(getJpaVendorAdapter().getPersistenceProviderRootPackage());
    }

    if (persistenceUnitPostProcessors != null) {
      for (PersistenceUnitPostProcessor persistenceUnitPostProcessor : persistenceUnitPostProcessors) {
        persistenceUnitPostProcessor.postProcessPersistenceUnitInfo(persistenceUnitInfo);
      }
    }

    if ((provider = getPersistenceProvider()) == null) {

      String providerClassName;

      if ((providerClassName = persistenceUnitInfo.getPersistenceProviderClassName()) == null) {
        throw new IllegalArgumentException("No PersistenceProvider specified in EntityManagerFactory configuration, and chosen PersistenceUnitInfo does not specify a provider class name either");
      } else {
        provider = (PersistenceProvider)BeanUtils.instantiateClass(ClassUtils.resolveClassName(providerClassName, getBeanClassLoader()));
      }
    }

    if (persistenceUnitInfo.getPersistenceUnitName() != null) {
      setPersistenceUnitName(persistenceUnitInfo.getPersistenceUnitName());
    }

    if (logger.isInfoEnabled()) {
      logger.info("Building JPA container EntityManagerFactory for persistence unit '" + persistenceUnitInfo.getPersistenceUnitName() + "'");
    }

    return provider.createContainerEntityManagerFactory(persistenceUnitInfo, getJpaPropertyMap());
  }
}
