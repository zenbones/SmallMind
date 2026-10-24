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
package org.smallmind.web.jersey.proxy;

import java.lang.reflect.Proxy;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JsonEntityResourceProxyScopeManagerTest {

  private static Proxy proxyOf (Class<?> resourceInterface) {

    return (Proxy)Proxy.newProxyInstance(resourceInterface.getClassLoader(), new Class<?>[] {resourceInterface}, (proxy, method, args) -> null);
  }

  public interface WidgetResource {

    String name ();
  }

  public void testRegisterAndGetProxy () {

    PerApplicationScope scope = new PerApplicationScope();
    Proxy proxy = proxyOf(WidgetResource.class);

    scope.run(() -> {
      JsonEntityResourceProxyScopeManager.register(WidgetResource.class, proxy);

      Assert.assertSame(JsonEntityResourceProxyScopeManager.getProxy(WidgetResource.class), proxy);
    });
  }

  public void testSeparateScopesIsolateProxies () {

    PerApplicationScope first = new PerApplicationScope();
    PerApplicationScope second = new PerApplicationScope();
    Proxy proxy = proxyOf(WidgetResource.class);

    first.run(() -> JsonEntityResourceProxyScopeManager.register(WidgetResource.class, proxy));

    Assert.assertSame(first.call(() -> JsonEntityResourceProxyScopeManager.getProxy(WidgetResource.class)), proxy);
    Assert.assertNull(second.call(() -> PerApplicationScope.getPerApplicationData(JsonEntityResourceProxyScopeManager.class, Object.class)));
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testGetProxyWithoutScopeThrows () {

    JsonEntityResourceProxyScopeManager.getProxy(WidgetResource.class);
  }
}
