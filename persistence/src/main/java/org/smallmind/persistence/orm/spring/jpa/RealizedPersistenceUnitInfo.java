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

  /**
   * Creates a snapshot of the provided {@link MutablePersistenceUnitInfo} as a {@link PersistenceUnitInfo} so it can
   * be safely handed to JPA providers after Spring has finished populating the metadata.
   *
   * @param mutablePersistenceUnitInfo the Spring-managed persistence unit description to copy
   */
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

  /**
   * Returns the name of the persistence unit as captured from the Spring metadata.
   *
   * @return the configured persistence unit name
   */
  @Override
  public String getPersistenceUnitName () {

    return persistenceUnitName;
  }

  /**
   * Updates the persistence unit name that will be reported to the JPA provider.
   *
   * @param persistenceUnitName the name to expose for this persistence unit
   */
  public void setPersistenceUnitName (String persistenceUnitName) {

    this.persistenceUnitName = persistenceUnitName;
  }

  /**
   * Provides the fully-qualified persistence provider class name responsible for bootstrapping the unit.
   *
   * @return the provider class name, or {@code null} if not specified
   */
  @Override
  public String getPersistenceProviderClassName () {

    return persistenceProviderClassName;
  }

  /**
   * Sets the persistence provider class name that this instance will report.
   *
   * @param persistenceProviderClassName the provider class name to expose
   */
  public void setPersistenceProviderClassName (String persistenceProviderClassName) {

    this.persistenceProviderClassName = persistenceProviderClassName;
  }

  /**
   * Returns the persistence provider package name used for provider-specific filtering.
   *
   * @return the provider package name, or {@code null} if none was supplied
   */
  public String getPersistenceProviderPackageName () {

    return persistenceProviderPackageName;
  }

  /**
   * Updates the persistence provider package name.
   *
   * @param persistenceProviderPackageName the package name to expose to the provider
   */
  public void setPersistenceProviderPackageName (String persistenceProviderPackageName) {

    this.persistenceProviderPackageName = persistenceProviderPackageName;
  }

  /**
   * Returns the CDI scope annotation name. This implementation does not track a scope and therefore always returns
   * {@code null}.
   *
   * @return always {@code null}; scope annotations are not supported
   */
  @Override
  public String getScopeAnnotationName () {

    return null;
  }

  /**
   * Returns CDI qualifier annotation names. This implementation does not track qualifiers and therefore returns an empty
   * list.
   *
   * @return an immutable empty list of qualifier annotation names
   */
  @Override
  public List<String> getQualifierAnnotationNames () {

    return Collections.emptyList();
  }

  /**
   * Retrieves the transaction type, converting the Spring {@link PersistenceUnitTransactionType} to the Jakarta SPI enum
   * required by providers.
   *
   * @return the Jakarta transaction type representing JTA or RESOURCE_LOCAL
   * @throws NullPointerException if the transaction type was not initialized before invocation
   */
  @Override
  public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType () {

    return jakarta.persistence.spi.PersistenceUnitTransactionType.valueOf(transactionType.name());
  }

  /**
   * Sets the persistence unit transaction type to be reported.
   *
   * @param transactionType the transaction type that should be exposed
   */
  public void setTransactionType (PersistenceUnitTransactionType transactionType) {

    this.transactionType = transactionType;
  }

  /**
   * Returns the JTA {@link DataSource} associated with this persistence unit.
   *
   * @return the JTA data source, or {@code null} if none was configured
   */
  @Override
  public DataSource getJtaDataSource () {

    return jtaDataSource;
  }

  /**
   * Sets the JTA {@link DataSource} that will be advertised to the provider.
   *
   * @param jtaDataSource the JTA data source to expose
   */
  public void setJtaDataSource (DataSource jtaDataSource) {

    this.jtaDataSource = jtaDataSource;
  }

  /**
   * Returns the non-JTA {@link DataSource} for resource-local transactions.
   *
   * @return the non-JTA data source, or {@code null} if none was configured
   */
  @Override
  public DataSource getNonJtaDataSource () {

    return nonJtaDataSource;
  }

  /**
   * Sets the non-JTA {@link DataSource} to expose to the provider.
   *
   * @param nonJtaDataSource the resource-local data source to use
   */
  public void setNonJtaDataSource (DataSource nonJtaDataSource) {

    this.nonJtaDataSource = nonJtaDataSource;
  }

  /**
   * Provides the list of mapping file names referenced by the persistence unit.
   *
   * @return the mapping file names as supplied by Spring
   */
  @Override
  public List<String> getMappingFileNames () {

    return mappingFileNames;
  }

  /**
   * Replaces the list of mapping file names.
   *
   * @param mappingFileNames mapping file names to expose; no defensive copy is made
   */
  public void setMappingFileNames (List<String> mappingFileNames) {

    this.mappingFileNames = mappingFileNames;
  }

  /**
   * Returns URLs of additional JARs that should be scanned for persistence artifacts.
   *
   * @return the JAR file URLs to scan
   */
  @Override
  public List<URL> getJarFileUrls () {

    return jarFileUrls;
  }

  /**
   * Sets the JAR URLs that will be scanned by the provider.
   *
   * @param jarFileUrls the URLs of JARs containing persistence resources
   */
  public void setJarFileUrls (List<URL> jarFileUrls) {

    this.jarFileUrls = jarFileUrls;
  }

  /**
   * Returns the root URL for the persistence unit, usually pointing to the location of {@code persistence.xml}.
   *
   * @return the persistence unit root URL, or {@code null} if none was provided
   */
  @Override
  public URL getPersistenceUnitRootUrl () {

    return persistenceUnitRootUrl;
  }

  /**
   * Sets the root URL for this persistence unit.
   *
   * @param persistenceUnitRootUrl the base URL used for resource resolution
   */
  public void setPersistenceUnitRootUrl (URL persistenceUnitRootUrl) {

    this.persistenceUnitRootUrl = persistenceUnitRootUrl;
  }

  /**
   * Returns the managed class names supplied by the Spring metadata.
   *
   * @return the fully-qualified names of managed entity classes
   */
  @Override
  public List<String> getManagedClassNames () {

    return managedClassNames;
  }

  /**
   * Sets the managed class names that will be reported to the provider.
   *
   * @param managedClassNames the fully-qualified managed class names
   */
  public void setManagedClassNames (List<String> managedClassNames) {

    this.managedClassNames = managedClassNames;
  }

  /**
   * Indicates whether classes not explicitly listed should be excluded from provider scanning.
   *
   * @return {@code true} if unlisted classes are excluded; {@code false} otherwise
   */
  @Override
  public boolean excludeUnlistedClasses () {

    return excludeUnlistedClass;
  }

  /**
   * Configures whether unlisted classes should be excluded from provider scanning.
   *
   * @param excludeUnlistedClass {@code true} to exclude classes not declared in metadata
   */
  public void setExcludeUnlistedClass (boolean excludeUnlistedClass) {

    this.excludeUnlistedClass = excludeUnlistedClass;
  }

  /**
   * Returns the shared cache mode as captured from the mutable persistence unit info.
   *
   * @return the configured shared cache mode
   */
  @Override
  public SharedCacheMode getSharedCacheMode () {

    return sharedCacheMode;
  }

  /**
   * Sets the shared cache mode that will be reported.
   *
   * @param sharedCacheMode the cache strategy to expose
   */
  public void setSharedCacheMode (SharedCacheMode sharedCacheMode) {

    this.sharedCacheMode = sharedCacheMode;
  }

  /**
   * Returns the validation mode used for entity validation.
   *
   * @return the validation mode
   */
  @Override
  public ValidationMode getValidationMode () {

    return validationMode;
  }

  /**
   * Updates the validation mode to be reported.
   *
   * @param validationMode the validation mode to expose
   */
  public void setValidationMode (ValidationMode validationMode) {

    this.validationMode = validationMode;
  }

  /**
   * Returns the persistence properties associated with this unit. Changes to the returned {@link Properties} instance
   * affect the stored state directly.
   *
   * @return the persistence unit properties
   */
  @Override
  public Properties getProperties () {

    return properties;
  }

  /**
   * Replaces the persistence properties that will be advertised to the provider.
   *
   * @param properties the persistence properties to store
   */
  public void setProperties (Properties properties) {

    this.properties = properties;
  }

  /**
   * Returns the persistence XML schema version associated with the configuration.
   *
   * @return the schema version string
   */
  @Override
  public String getPersistenceXMLSchemaVersion () {

    return persistenceXMLSchemaVersion;
  }

  /**
   * Sets the persistence XML schema version string to report.
   *
   * @param persistenceXMLSchemaVersion the schema version value
   */
  public void setPersistenceXMLSchemaVersion (String persistenceXMLSchemaVersion) {

    this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
  }

  /**
   * Provides the class loader used to load persistence classes. This implementation delegates to the current thread
   * context class loader to mimic typical container behavior.
   *
   * @return the current thread context class loader
   */
  @Override
  public ClassLoader getClassLoader () {

    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Registers a class transformer. This realized implementation does not apply transformers after construction, so the
   * method is a no-op and never throws.
   *
   * @param classTransformer the transformer to register (ignored)
   */
  @Override
  public void addTransformer (ClassTransformer classTransformer) {

  }

  /**
   * Provides a temporary class loader for provider use. This implementation does not supply one and returns {@code null}.
   *
   * @return always {@code null}, indicating no temporary class loader is provided
   */
  @Override
  public ClassLoader getNewTempClassLoader () {

    return null;
  }
}
