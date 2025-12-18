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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

public class RealizedPersistenceUnitInfo implements PersistenceUnitInfo {

  private Properties properties = new Properties();
  private SharedCacheMode sharedCacheMode = SharedCacheMode.UNSPECIFIED;
  private ValidationMode validationMode = ValidationMode.AUTO;
  private PersistenceUnitTransactionType transactionType;
  private DataSource jtaDataSource;
  private DataSource nonJtaDataSource;
  private URL persistenceUnitRootUrl;
  private List<URL> jarFileUrls = new ArrayList<>();
  private List<String> mappingFileNames = new ArrayList<>();
  private List<String> managedClassNames = new ArrayList<>();
  private String persistenceUnitName;
  private String persistenceProviderClassName;
  private String persistenceProviderPackageName;
  private String persistenceXMLSchemaVersion = "4.0";
  private boolean excludeUnlistedClass;

  public RealizedPersistenceUnitInfo (MutablePersistenceUnitInfo mutablePersistenceUnitInfo) {

    persistenceUnitName = mutablePersistenceUnitInfo.getPersistenceUnitName();
    persistenceProviderClassName = mutablePersistenceUnitInfo.getPersistenceProviderClassName();
    persistenceProviderPackageName = mutablePersistenceUnitInfo.getPersistenceProviderPackageName();
    transactionType = mutablePersistenceUnitInfo.getTransactionType();
    jtaDataSource = mutablePersistenceUnitInfo.getJtaDataSource();
    nonJtaDataSource = mutablePersistenceUnitInfo.getNonJtaDataSource();
    persistenceUnitRootUrl = mutablePersistenceUnitInfo.getPersistenceUnitRootUrl();
    excludeUnlistedClass = mutablePersistenceUnitInfo.excludeUnlistedClasses();
    sharedCacheMode = mutablePersistenceUnitInfo.getSharedCacheMode();
    validationMode = mutablePersistenceUnitInfo.getValidationMode();
    properties = mutablePersistenceUnitInfo.getProperties();
    persistenceXMLSchemaVersion = mutablePersistenceUnitInfo.getPersistenceXMLSchemaVersion();
    persistenceProviderPackageName = mutablePersistenceUnitInfo.getPersistenceProviderPackageName();
    jarFileUrls = mutablePersistenceUnitInfo.getJarFileUrls();
    mappingFileNames = mutablePersistenceUnitInfo.getMappingFileNames();
    managedClassNames = mutablePersistenceUnitInfo.getManagedClassNames();
  }

  @Override
  public String getPersistenceUnitName () {

    return persistenceUnitName;
  }

  public void setPersistenceUnitName (String persistenceUnitName) {

    this.persistenceUnitName = persistenceUnitName;
  }

  @Override
  public String getPersistenceProviderClassName () {

    return persistenceProviderClassName;
  }

  public void setPersistenceProviderClassName (String persistenceProviderClassName) {

    this.persistenceProviderClassName = persistenceProviderClassName;
  }

  public String getPersistenceProviderPackageName () {

    return persistenceProviderPackageName;
  }

  public void setPersistenceProviderPackageName (String persistenceProviderPackageName) {

    this.persistenceProviderPackageName = persistenceProviderPackageName;
  }

  @Override
  public String getScopeAnnotationName () {

    return null;
  }

  @Override
  public List<String> getQualifierAnnotationNames () {

    return Collections.emptyList();
  }

  @Override
  public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType () {

    return jakarta.persistence.spi.PersistenceUnitTransactionType.valueOf(transactionType.name());
  }

  public void setTransactionType (PersistenceUnitTransactionType transactionType) {

    this.transactionType = transactionType;
  }

  @Override
  public DataSource getJtaDataSource () {

    return jtaDataSource;
  }

  public void setJtaDataSource (DataSource jtaDataSource) {

    this.jtaDataSource = jtaDataSource;
  }

  @Override
  public DataSource getNonJtaDataSource () {

    return nonJtaDataSource;
  }

  public void setNonJtaDataSource (DataSource nonJtaDataSource) {

    this.nonJtaDataSource = nonJtaDataSource;
  }

  @Override
  public List<String> getMappingFileNames () {

    return mappingFileNames;
  }

  public void setMappingFileNames (List<String> mappingFileNames) {

    this.mappingFileNames = mappingFileNames;
  }

  @Override
  public List<URL> getJarFileUrls () {

    return jarFileUrls;
  }

  public void setJarFileUrls (List<URL> jarFileUrls) {

    this.jarFileUrls = jarFileUrls;
  }

  @Override
  public URL getPersistenceUnitRootUrl () {

    return persistenceUnitRootUrl;
  }

  public void setPersistenceUnitRootUrl (URL persistenceUnitRootUrl) {

    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  @Override
  public List<String> getManagedClassNames () {

    return managedClassNames;
  }

  public void setManagedClassNames (List<String> managedClassNames) {

    this.managedClassNames = managedClassNames;
  }

  @Override
  public boolean excludeUnlistedClasses () {

    return excludeUnlistedClass;
  }

  public void setExcludeUnlistedClass (boolean excludeUnlistedClass) {

    this.excludeUnlistedClass = excludeUnlistedClass;
  }

  @Override
  public SharedCacheMode getSharedCacheMode () {

    return sharedCacheMode;
  }

  public void setSharedCacheMode (SharedCacheMode sharedCacheMode) {

    this.sharedCacheMode = sharedCacheMode;
  }

  @Override
  public ValidationMode getValidationMode () {

    return validationMode;
  }

  public void setValidationMode (ValidationMode validationMode) {

    this.validationMode = validationMode;
  }

  @Override
  public Properties getProperties () {

    return properties;
  }

  public void setProperties (Properties properties) {

    this.properties = properties;
  }

  @Override
  public String getPersistenceXMLSchemaVersion () {

    return persistenceXMLSchemaVersion;
  }

  public void setPersistenceXMLSchemaVersion (String persistenceXMLSchemaVersion) {

    this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
  }

  @Override
  public ClassLoader getClassLoader () {

    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public void addTransformer (ClassTransformer classTransformer) {

  }

  @Override
  public ClassLoader getNewTempClassLoader () {

    return null;
  }
}
