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
package org.smallmind.persistence.sql.pool.spring;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.nutsnbolts.spring.RuntimeBeansException;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessor;
import org.smallmind.nutsnbolts.spring.SpringPropertyAccessorManager;
import org.smallmind.nutsnbolts.util.Option;
import org.smallmind.persistence.sql.pool.DataSourceFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DynamicPooledDataSourceInitializingBeanTest {

  /**
   * Installs a per-application context on the current thread so that
   * {@link SpringPropertyAccessorManager#register} (which writes to {@link PerApplicationContext})
   * does not fail. Runs before every test method.
   */
  @BeforeMethod
  public void installPerApplicationContext () {

    new PerApplicationContext();
  }

  /**
   * Builds a Mockito-mocked {@link SpringPropertyAccessor} backed by the supplied property map:
   * {@code getKeySet()} returns the keys and {@code asString} returns each value. The numeric/boolean
   * accessors fall back to {@link Option#none()} for absent keys, matching the real accessor's
   * contract for the optional pool-tuning properties.
   */
  private static SpringPropertyAccessor mockAccessor (Map<String, String> properties) {

    SpringPropertyAccessor accessor = Mockito.mock(SpringPropertyAccessor.class);
    Set<String> keySet = new LinkedHashSet<>(properties.keySet());

    Mockito.when(accessor.getKeySet()).thenReturn(keySet);
    Mockito.when(accessor.asString(Mockito.anyString())).thenAnswer(invocation -> properties.get(invocation.getArgument(0)));
    Mockito.when(accessor.asInt(Mockito.anyString())).thenReturn(Option.none());
    Mockito.when(accessor.asLong(Mockito.anyString())).thenReturn(Option.none());
    Mockito.when(accessor.asBoolean(Mockito.anyString())).thenReturn(Option.none());

    return accessor;
  }

  /**
   * Constructs the bean wired with a single mocked factory for the given pool name and no prefix,
   * registering the supplied property accessor for the current thread.
   */
  private static DynamicPooledDataSourceInitializingBean beanFor (String poolName, SpringPropertyAccessor accessor) {

    SpringPropertyAccessorManager.register(accessor);

    Map<String, DataSourceFactory<?, ?>> factoryMap = new HashMap<>();
    factoryMap.put(poolName, Mockito.mock(DataSourceFactory.class));

    DynamicPooledDataSourceInitializingBean bean = new DynamicPooledDataSourceInitializingBean();
    bean.setFactoryMap(factoryMap);
    bean.setPrefix(null);

    return bean;
  }

  @Test(groups = "unit", expectedExceptions = RuntimeBeansException.class)
  public void testPoolWithNoDefinedConnectionsFails ()
    throws Exception {

    // factoryMap names "mypool" but no jdbc.url/user/password keys exist for it.
    DynamicPooledDataSourceInitializingBean bean = beanFor("mypool", mockAccessor(new HashMap<>()));

    bean.afterPropertiesSet();
  }

  @Test(groups = "unit", expectedExceptions = RuntimeBeansException.class)
  public void testSingleNonNullContextMustBeContextless ()
    throws Exception {

    // A lone context ("ctx") with no contextless definition is rejected before any pool is built.
    HashMap<String, String> properties = new HashMap<>();
    properties.put("jdbc.url.mypool.ctx.0", "jdbc:fake://host/db");

    DynamicPooledDataSourceInitializingBean bean = beanFor("mypool", mockAccessor(properties));

    bean.afterPropertiesSet();
  }

  @Test(groups = "unit", expectedExceptions = RuntimeBeansException.class)
  public void testIncompleteContextlessConnectionFails ()
    throws Exception {

    // A single contextless connection with only a URL (no user/password) is incomplete.
    HashMap<String, String> properties = new HashMap<>();
    properties.put("jdbc.url.mypool.0", "jdbc:fake://host/db");

    DynamicPooledDataSourceInitializingBean bean = beanFor("mypool", mockAccessor(properties));

    bean.afterPropertiesSet();
  }

  @Test(groups = "unit", expectedExceptions = RuntimeBeansException.class)
  public void testMissingConnectionIndexFails ()
    throws Exception {

    // Contextless indices 0 (complete) and 2 (present) but index 1 absent -> gap detected.
    HashMap<String, String> properties = new HashMap<>();
    properties.put("jdbc.url.mypool.0", "jdbc:fake://host/db");
    properties.put("jdbc.user.mypool.0", "user0");
    properties.put("jdbc.password.mypool.0", "password0");
    properties.put("jdbc.url.mypool.2", "jdbc:fake://host/db2");
    properties.put("jdbc.user.mypool.2", "user2");
    properties.put("jdbc.password.mypool.2", "password2");

    DynamicPooledDataSourceInitializingBean bean = beanFor("mypool", mockAccessor(properties));

    bean.afterPropertiesSet();
  }

  @Test(groups = "unit", expectedExceptions = NumberFormatException.class)
  public void testMalformedConnectionIndexFails ()
    throws Exception {

    // The suffix after the url prefix must parse as an integer index.
    HashMap<String, String> properties = new HashMap<>();
    properties.put("jdbc.url.mypool.notanumber", "jdbc:fake://host/db");

    DynamicPooledDataSourceInitializingBean bean = beanFor("mypool", mockAccessor(properties));

    bean.afterPropertiesSet();
  }

  public void testGetDataSourceWithUnknownKeyFails () {

    // No mapping has been parsed, so any lookup key is unmapped.
    DynamicPooledDataSourceInitializingBean bean = new DynamicPooledDataSourceInitializingBean();

    try {
      bean.getDataSource("nope");
      Assert.fail("Expected a RuntimeBeansException");
    } catch (RuntimeBeansException runtimeBeansException) {
      // expected: no mapping definition provided for the key
    }
  }
}
