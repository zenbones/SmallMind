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
package org.smallmind.web.grizzly;

import java.util.HashMap;
import java.util.Map;
import org.glassfish.grizzly.servlet.WebappContext;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class GrizzlyWebAppStateLocatorTest {

  public static class MapBackedLocator implements GrizzlyWebAppStateLocator {

    private final Map<String, GrizzlyWebAppState> stateMap = new HashMap<>();

    public void register (String context, GrizzlyWebAppState state) {

      stateMap.put(context, state);
    }

    @Override
    public GrizzlyWebAppState webAppStateFor (String context) {

      return stateMap.get(context);
    }
  }

  public void testLocatorResolvesRegisteredState () {

    MapBackedLocator locator = new MapBackedLocator();
    GrizzlyWebAppState state = new GrizzlyWebAppState(new WebappContext("Test Context", "/context"));

    locator.register("/context", state);

    Assert.assertSame(locator.webAppStateFor("/context"), state);
  }

  public void testLocatorReturnsNullForUnknownContext () {

    Assert.assertNull(new MapBackedLocator().webAppStateFor("/missing"));
  }
}
