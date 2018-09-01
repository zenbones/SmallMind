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
package org.smallmind.persistence.orm.spring.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import org.springframework.beans.BeanUtils;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.ClassUtils;

public class EntitySeekingEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

  private AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor;
  private DataSource dataSource;
  private DataSource jtaDataSource;
  private SharedCacheMode sharedCacheMode;
  private ValidationMode validationMode;
  private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;
  private MutablePersistenceUnitInfo persistenceUnitInfo;
  private String sessionSourceKey;
  private boolean excludeUnlistedClasses = false;

  public void setAnnotationSeekingBeanFactoryPostProcessor (AnnotationSeekingBeanFactoryPostProcessor annotationSeekingBeanFactoryPostProcessor) {

    this.annotationSeekingBeanFactoryPostProcessor = annotationSeekingBeanFactoryPostProcessor;
  }

  public void setSessionSourceKey (String sessionSourceKey) {

    this.sessionSourceKey = sessionSourceKey;
  }

  public void setDataSource (DataSource dataSource) {

    this.dataSource = dataSource;
  }

  public void setJtaDataSource (DataSource jtaDataSource) {

    this.jtaDataSource = jtaDataSource;
  }

  public void setSharedCacheMode (SharedCacheMode sharedCacheMode) {

    this.sharedCacheMode = sharedCacheMode;
  }

  public void setValidationMode (ValidationMode validationMode) {

    this.validationMode = validationMode;
  }

  public void setPersistenceUnitInfo (MutablePersistenceUnitInfo persistenceUnitInfo) {

    this.persistenceUnitInfo = persistenceUnitInfo;
  }

  public void setExcludeUnlistedClasses (boolean excludeUnlistedClasses) {

    this.excludeUnlistedClasses = excludeUnlistedClasses;
  }

  public void setPersistenceUnitPostProcessors (PersistenceUnitPostProcessor... persistenceUnitPostProcessors) {

    this.persistenceUnitPostProcessors = persistenceUnitPostProcessors;
  }

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
