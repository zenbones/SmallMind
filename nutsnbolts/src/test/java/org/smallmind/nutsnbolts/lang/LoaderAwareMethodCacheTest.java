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
package org.smallmind.nutsnbolts.lang;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LoaderAwareMethodCacheTest {

  private static Method method (String name, Class<?>... parameters)
    throws NoSuchMethodException {

    return Fixture.class.getDeclaredMethod(name, parameters);
  }

  public void testGetOnEmptyCacheReturnsNull ()
    throws NoSuchMethodException {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();

    Assert.assertNull(cache.get(method("noArgs")));
  }

  public void testPutThenGetRoundTripsValue ()
    throws NoSuchMethodException {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();
    Method method = method("noArgs");

    Assert.assertNull(cache.put(method, "first"));
    Assert.assertEquals(cache.get(method), "first");
  }

  public void testPutReplacesExistingValueAndReturnsPriorValue ()
    throws NoSuchMethodException {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();
    Method method = method("noArgs");

    cache.put(method, "first");

    Assert.assertEquals(cache.put(method, "second"), "first");
    Assert.assertEquals(cache.get(method), "second");
  }

  public void testPutIfAbsentDoesNotOverwriteExistingValue ()
    throws NoSuchMethodException {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();
    Method method = method("noArgs");

    Assert.assertNull(cache.putIfAbsent(method, "first"));
    Assert.assertEquals(cache.putIfAbsent(method, "second"), "first");
    Assert.assertEquals(cache.get(method), "first");
  }

  public void testDistinctMethodsOnSameLoaderShareSegment ()
    throws NoSuchMethodException {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();
    Method first = method("noArgs");
    Method second = method("oneArg", String.class);

    cache.put(first, "a");
    cache.put(second, "b");

    Assert.assertEquals(cache.get(first), "a");
    Assert.assertEquals(cache.get(second), "b");
  }

  public void testCacheDoesNotKeepClassLoaderReachable ()
    throws Exception {

    LoaderAwareMethodCache<String> cache = new LoaderAwareMethodCache<>();
    WeakReference<ClassLoader> loaderReference = cacheMethodFromDiscardedLoader(cache);

    for (int attempt = 0; (attempt < 50) && (loaderReference.get() != null); attempt++) {
      System.gc();
      Thread.sleep(20);
    }

    Assert.assertNull(loaderReference.get(), "The cache kept a discarded class loader reachable");
    Assert.assertNull(cache.get(method("noArgs")));
  }

  private static WeakReference<ClassLoader> cacheMethodFromDiscardedLoader (LoaderAwareMethodCache<String> cache)
    throws Exception {

    Path classDirectory = Files.createTempDirectory("loader-aware-method-cache");
    Path packageDirectory = Files.createDirectories(classDirectory.resolve("discarded"));
    Path transientSource = Files.writeString(packageDirectory.resolve("Transient.java"), "package discarded; public class Transient { public String name () { return \"transient\"; } }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), transientSource.toString()), 0);

    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {classDirectory.toUri().toURL()}, LoaderAwareMethodCacheTest.class.getClassLoader())) {

      Method transientMethod = classLoader.loadClass("discarded.Transient").getMethod("name");

      cache.put(transientMethod, "transient");
      Assert.assertEquals(cache.get(transientMethod), "transient");

      return new WeakReference<>(classLoader);
    }
  }

  static class Fixture {

    public void noArgs () {

    }

    public void oneArg (String value) {

    }
  }
}
