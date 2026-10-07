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
public class ClassLoaderAwareCacheTest {

  public void testGetOnEmptyCacheReturnsNull () {

    ClassLoaderAwareCache<Class<?>, String> cache = new ClassLoaderAwareCache<>(clazz -> clazz);

    Assert.assertNull(cache.get(String.class));
  }

  public void testPutReplacesExistingValueAndReturnsPriorValue () {

    ClassLoaderAwareCache<Class<?>, String> cache = new ClassLoaderAwareCache<>(clazz -> clazz);

    Assert.assertNull(cache.put(Fixture.class, "first"));
    Assert.assertEquals(cache.put(Fixture.class, "second"), "first");
    Assert.assertEquals(cache.get(Fixture.class), "second");
  }

  public void testPutIfAbsentDoesNotOverwriteExistingValue () {

    ClassLoaderAwareCache<Class<?>, String> cache = new ClassLoaderAwareCache<>(clazz -> clazz);

    Assert.assertNull(cache.putIfAbsent(Fixture.class, "first"));
    Assert.assertEquals(cache.putIfAbsent(Fixture.class, "second"), "first");
    Assert.assertEquals(cache.get(Fixture.class), "first");
  }

  public void testKeysAreStoredInThePartitionOfTheirClass ()
    throws NoSuchMethodException {

    ClassLoaderAwareCache<Method, String> cache = new ClassLoaderAwareCache<>(Method::getDeclaringClass);
    Method fixtureMethod = Fixture.class.getMethod("name");
    Method objectMethod = Object.class.getMethod("toString");

    cache.put(fixtureMethod, "fixture");
    cache.put(objectMethod, "object");

    Assert.assertEquals(cache.get(fixtureMethod), "fixture");
    Assert.assertEquals(cache.get(objectMethod), "object");
  }

  public void testCacheDoesNotKeepClassLoaderReachable ()
    throws Exception {

    ClassLoaderAwareCache<Class<?>, Method> cache = new ClassLoaderAwareCache<>(clazz -> clazz);
    WeakReference<ClassLoader> loaderReference = cacheClassFromDiscardedLoader(cache);

    for (int attempt = 0; (attempt < 50) && (loaderReference.get() != null); attempt++) {
      System.gc();
      Thread.sleep(20);
    }

    Assert.assertNull(loaderReference.get(), "The cache kept a discarded class loader reachable");
    Assert.assertNull(cache.get(String.class));
  }

  private static WeakReference<ClassLoader> cacheClassFromDiscardedLoader (ClassLoaderAwareCache<Class<?>, Method> cache)
    throws Exception {

    Path classDirectory = Files.createTempDirectory("class-loader-aware-cache");
    Path packageDirectory = Files.createDirectories(classDirectory.resolve("discarded"));
    Path transientSource = Files.writeString(packageDirectory.resolve("Transient.java"), "package discarded; public class Transient { public String name () { return \"transient\"; } }");

    Assert.assertEquals(ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classDirectory.toString(), transientSource.toString()), 0);

    try (URLClassLoader classLoader = new URLClassLoader(new URL[] {classDirectory.toUri().toURL()}, ClassLoaderAwareCacheTest.class.getClassLoader())) {

      Class<?> transientClass = classLoader.loadClass("discarded.Transient");

      cache.put(transientClass, transientClass.getMethod("name"));
      Assert.assertNotNull(cache.get(transientClass));

      return new WeakReference<>(classLoader);
    }
  }

  public static class Fixture {

    public String name () {

      return "fixture";
    }
  }
}
