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

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import org.smallmind.scribe.pen.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link JsonTarget} fluent builder surface: host parsing, the {@code path} prefix-composition semantics,
 * and the chaining contract of {@code header}, {@code query}, and {@code debug}. The network-bound request methods are
 * not exercised here.
 */
@Test(groups = "unit")
public class JsonTargetTest {

  public void testConstructorParsesHost ()
    throws Exception {

    Assert.assertNotNull(new JsonTarget("https://api.example.com"));
  }

  @Test(expectedExceptions = URISyntaxException.class)
  public void testConstructorRejectsBadHost ()
    throws URISyntaxException {

    new JsonTarget("not a valid uri");
  }

  public void testPathReturnsDistinctInstance ()
    throws Exception {

    JsonTarget target = new JsonTarget("https://api.example.com");
    JsonTarget pathed = target.path("/v1/service");

    Assert.assertNotSame(pathed, target);
  }

  public void testPathWithoutPrefixUsedAsIs ()
    throws Exception {

    Assert.assertEquals(pathOf(new JsonTarget("https://api.example.com").path("/v1/users")), "/v1/users");
  }

  public void testPathAppendsToContextPrefix ()
    throws Exception {

    JsonTarget base = JsonTargetFactory.manufacture(HttpProtocol.HTTPS, "api.example.com", 443, "/app");

    Assert.assertEquals(pathOf(base.path("/v1/users")), "/app/v1/users");
  }

  public void testPathCollapsesRootContextSlash ()
    throws Exception {

    JsonTarget base = JsonTargetFactory.manufacture(HttpProtocol.HTTPS, "api.example.com", 443, "/");

    Assert.assertEquals(pathOf(base.path("/v1/users")), "/v1/users");
  }

  public void testPathCollapsesTrailingPrefixSlash ()
    throws Exception {

    JsonTarget base = JsonTargetFactory.manufacture(HttpProtocol.HTTPS, "api.example.com", 443, "/app/");

    Assert.assertEquals(pathOf(base.path("/v1/users")), "/app/v1/users");
  }

  private static String pathOf (JsonTarget target)
    throws NoSuchFieldException, IllegalAccessException {

    Field pathField = JsonTarget.class.getDeclaredField("path");

    pathField.setAccessible(true);

    return (String)pathField.get(target);
  }

  public void testHeaderReturnsSameInstance ()
    throws Exception {

    JsonTarget target = new JsonTarget("https://api.example.com");

    Assert.assertSame(target.header("Authorization", "Bearer token"), target);
    Assert.assertSame(target.header("X-Extra", "value"), target);
  }

  public void testQueryReturnsSameInstance ()
    throws Exception {

    JsonTarget target = new JsonTarget("https://api.example.com");

    Assert.assertSame(target.query("page", "1"), target);
    Assert.assertSame(target.query("size", "20"), target);
  }

  public void testDebugReturnsSameInstance ()
    throws Exception {

    JsonTarget target = new JsonTarget("https://api.example.com");

    Assert.assertSame(target.debug(Level.DEBUG), target);
  }
}
