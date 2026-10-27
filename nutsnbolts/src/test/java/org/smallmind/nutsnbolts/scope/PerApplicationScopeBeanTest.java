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
package org.smallmind.nutsnbolts.scope;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeBeanTest {

  private static final String CONFIG_LOCATION = "classpath:org/smallmind/nutsnbolts/scope/per-application-scope-bean.xml";

  public void testFactoryMethodBeanIsTheScopeEnclosingTheRefresh () {

    PerApplicationScope enclosing = new PerApplicationScope();
    ClassPathXmlApplicationContext context = enclosing.call(() -> new ClassPathXmlApplicationContext(CONFIG_LOCATION));

    try {
      Assert.assertSame(context.getBean("perApplicationScope"), enclosing);
      Assert.assertSame(context.getBean("perApplicationScope", PerApplicationScope.class), enclosing);
      Assert.assertSame(context.getBean(PerApplicationScope.class), enclosing, "the bound scope is also reachable by type");
      Assert.assertSame(context.getBean(ScopeHolder.class).getPerApplicationScope(), enclosing, "a ref injects the bound scope");
    } finally {
      context.close();
    }
  }

  public void testFactoryMethodBeanIsNullWhenNothingEnclosesTheRefresh () {

    ClassPathXmlApplicationContext context;
    AtomicReference<Object> byName = new AtomicReference<>();
    AtomicReference<Object> byType = new AtomicReference<>();

    Assert.assertFalse(PerApplicationScope.isBound(), "the refreshing thread must carry no scope");
    context = new ClassPathXmlApplicationContext(CONFIG_LOCATION);
    try {
      Assert.assertNull(context.getBean(ScopeHolder.class).getPerApplicationScope(), "a ref injects null when nothing was bound around the refresh");

      try {
        byName.set(context.getBean("perApplicationScope"));
      } catch (RuntimeException runtimeException) {
        byName.set(runtimeException);
      }
      try {
        byType.set(context.getBean(PerApplicationScope.class));
      } catch (NoSuchBeanDefinitionException noSuchBeanDefinitionException) {
        byType.set(noSuchBeanDefinitionException);
      }

      // Spring stores a null factory result as its internal NullBean placeholder, which a by-name lookup hands back
      // as-is and a by-type lookup does not match.
      Assert.assertEquals(byName.get().getClass().getName(), "org.springframework.beans.factory.support.NullBean", "getBean(name) observed: " + byName.get());
      Assert.assertTrue(byType.get() instanceof NoSuchBeanDefinitionException, "getBean(type) observed: " + byType.get());
    } finally {
      context.close();
    }
  }

  public static class ScopeHolder {

    private PerApplicationScope perApplicationScope;

    public PerApplicationScope getPerApplicationScope () {

      return perApplicationScope;
    }

    public void setPerApplicationScope (PerApplicationScope perApplicationScope) {

      this.perApplicationScope = perApplicationScope;
    }
  }
}
