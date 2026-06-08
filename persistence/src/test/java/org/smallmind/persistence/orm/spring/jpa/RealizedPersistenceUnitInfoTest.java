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
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.mockito.Mockito;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link RealizedPersistenceUnitInfo}, the immutable-after-construction snapshot of a Spring
 * {@link MutablePersistenceUnitInfo}. The tests exercise the constructor copy from a populated mutable info and
 * then round-trip every mutable property through its setter and getter, including the transaction-type conversion
 * from the Jakarta {@link PersistenceUnitTransactionType} to the SPI enum reported to providers. No database is
 * involved.
 */
@Test(groups = "unit")
public class RealizedPersistenceUnitInfoTest {

  private RealizedPersistenceUnitInfo unitInfo;

  @BeforeMethod
  public void setUp () {

    MutablePersistenceUnitInfo mutablePersistenceUnitInfo = new MutablePersistenceUnitInfo();

    mutablePersistenceUnitInfo.setPersistenceUnitName("seed-unit");
    mutablePersistenceUnitInfo.setTransactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL);

    unitInfo = new RealizedPersistenceUnitInfo(mutablePersistenceUnitInfo);
  }

  public void testConstructorCopiesNameAndTransactionTypeFromTheMutableInfo () {

    Assert.assertEquals(unitInfo.getPersistenceUnitName(), "seed-unit", "the constructor should copy the persistence unit name");
    Assert.assertEquals(unitInfo.getTransactionType(), jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL, "the constructor should copy the transaction type as the SPI enum");
  }

  public void testPersistenceUnitNameRoundTrip () {

    unitInfo.setPersistenceUnitName("orders-unit");

    Assert.assertEquals(unitInfo.getPersistenceUnitName(), "orders-unit");
  }

  public void testPersistenceProviderClassNameRoundTrip () {

    unitInfo.setPersistenceProviderClassName("org.hibernate.jpa.HibernatePersistenceProvider");

    Assert.assertEquals(unitInfo.getPersistenceProviderClassName(), "org.hibernate.jpa.HibernatePersistenceProvider");
  }

  public void testPersistenceProviderPackageNameRoundTrip () {

    unitInfo.setPersistenceProviderPackageName("org.hibernate.jpa");

    Assert.assertEquals(unitInfo.getPersistenceProviderPackageName(), "org.hibernate.jpa");
  }

  public void testTransactionTypeRoundTripConvertsToTheSpiEnum () {

    unitInfo.setTransactionType(PersistenceUnitTransactionType.JTA);

    Assert.assertEquals(unitInfo.getTransactionType(), jakarta.persistence.spi.PersistenceUnitTransactionType.JTA, "the transaction type getter should report the matching SPI enum");
  }

  public void testJtaDataSourceRoundTrip () {

    DataSource dataSource = Mockito.mock(DataSource.class);

    unitInfo.setJtaDataSource(dataSource);

    Assert.assertSame(unitInfo.getJtaDataSource(), dataSource);
  }

  public void testNonJtaDataSourceRoundTrip () {

    DataSource dataSource = Mockito.mock(DataSource.class);

    unitInfo.setNonJtaDataSource(dataSource);

    Assert.assertSame(unitInfo.getNonJtaDataSource(), dataSource);
  }

  public void testMappingFileNamesRoundTrip () {

    List<String> mappingFileNames = new ArrayList<>();

    mappingFileNames.add("orm.xml");

    unitInfo.setMappingFileNames(mappingFileNames);

    Assert.assertSame(unitInfo.getMappingFileNames(), mappingFileNames);
  }

  public void testManagedClassNamesRoundTrip () {

    List<String> managedClassNames = new ArrayList<>();

    managedClassNames.add("com.example.Widget");

    unitInfo.setManagedClassNames(managedClassNames);

    Assert.assertSame(unitInfo.getManagedClassNames(), managedClassNames);
  }

  public void testJarFileUrlsRoundTrip ()
    throws Exception {

    List<URL> jarFileUrls = new ArrayList<>();

    jarFileUrls.add(new URL("file:/tmp/persistence.jar"));

    unitInfo.setJarFileUrls(jarFileUrls);

    Assert.assertSame(unitInfo.getJarFileUrls(), jarFileUrls);
  }

  public void testPersistenceUnitRootUrlRoundTrip ()
    throws Exception {

    URL rootUrl = new URL("file:/tmp/unit-root/");

    unitInfo.setPersistenceUnitRootUrl(rootUrl);

    Assert.assertSame(unitInfo.getPersistenceUnitRootUrl(), rootUrl);
  }

  public void testSharedCacheModeRoundTrip () {

    unitInfo.setSharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE);

    Assert.assertEquals(unitInfo.getSharedCacheMode(), SharedCacheMode.ENABLE_SELECTIVE);
  }

  public void testValidationModeRoundTrip () {

    unitInfo.setValidationMode(ValidationMode.CALLBACK);

    Assert.assertEquals(unitInfo.getValidationMode(), ValidationMode.CALLBACK);
  }

  public void testPropertiesRoundTrip () {

    Properties properties = new Properties();

    properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

    unitInfo.setProperties(properties);

    Assert.assertSame(unitInfo.getProperties(), properties);
    Assert.assertEquals(unitInfo.getProperties().getProperty("hibernate.dialect"), "org.hibernate.dialect.MySQLDialect");
  }

  public void testPersistenceXMLSchemaVersionRoundTrip () {

    unitInfo.setPersistenceXMLSchemaVersion("3.1");

    Assert.assertEquals(unitInfo.getPersistenceXMLSchemaVersion(), "3.1");
  }
}
