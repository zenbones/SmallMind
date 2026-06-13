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

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import org.smallmind.web.grizzly.option.ClassLoaderResourceOption;
import org.smallmind.web.grizzly.option.WebApplicationOption;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Integration scenarios exercising {@link GrizzlyInitializingBean} configuration branches that the existing boot tests
 * do not reach: duplicate-context and unknown-context validation failures, the class-loader static resource handler,
 * and the optional header-size, worker-pool, and debug-mode listener configuration branches driven through a real boot.
 */
@Test(groups = "integration")
public class GrizzlyInitializingBeanBranchTest {

  private static int freePort ()
    throws Exception {

    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }

  private static HttpResponse<String> get (int port, String path)
    throws Exception {

    return HttpClient.newHttpClient().send(
      HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
      HttpResponse.BodyHandlers.ofString());
  }

  public void testDuplicateContextPathFailsFast () {

    GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

    WebApplicationOption firstOption = new WebApplicationOption();
    firstOption.setContextPath("/dup");

    WebApplicationOption secondOption = new WebApplicationOption();
    secondOption.setContextPath("/dup");

    bean.setWebApplicationOptions(new WebApplicationOption[] {firstOption, secondOption});

    // Two options sharing a context path must be rejected during afterPropertiesSet, before any server is created.
    Assert.assertThrows(GrizzlyInitializationException.class, bean::afterPropertiesSet);
  }

  public void testWebAppStateForMissingContextFailsFast () {

    GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

    WebApplicationOption webApplicationOption = new WebApplicationOption();
    webApplicationOption.setContextPath("/known");

    bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});
    bean.afterPropertiesSet();

    // A null or empty context path is reported distinctly from a path that was never registered.
    Assert.assertThrows(GrizzlyInitializationException.class, () -> bean.webAppStateFor(null));
    Assert.assertThrows(GrizzlyInitializationException.class, () -> bean.webAppStateFor(""));
    Assert.assertThrows(GrizzlyInitializationException.class, () -> bean.webAppStateFor("/unregistered"));
  }

  public void testWebAppStateForKnownContextReturnsState () {

    GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

    WebApplicationOption webApplicationOption = new WebApplicationOption();
    webApplicationOption.setContextPath("/present");

    bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});
    bean.afterPropertiesSet();

    GrizzlyWebAppState webAppState = bean.webAppStateFor("/present");

    Assert.assertNotNull(webAppState);
    Assert.assertNotNull(webAppState.getWebAppContext());
  }

  public void testClassLoaderResourceHandlerServesClasspathResource ()
    throws Exception {

    int port = freePort();
    // Grizzly strips only the first path segment of a handler mapping as the request context, so a class-loader
    // resource handler mapped at contextPath + staticPath (/cl/assets) resolves resources against everything after the
    // first segment (/assets/...). The marker resource is staged beneath a private class loader root at that location
    // and is resolved when the request /cl/assets/<file> reaches the handler.
    java.nio.file.Path classLoaderRoot = Files.createTempDirectory("grizzly-classloader-root");
    java.nio.file.Path assetsDirectory = classLoaderRoot.resolve("assets");

    Files.createDirectories(assetsDirectory);
    Files.writeString(assetsDirectory.resolve("grizzly-classloader-resource.txt"), "classloader-resource-content");
    // Grizzly keeps the resource open on Windows until the server stops, so defer cleanup to JVM exit.
    classLoaderRoot.toFile().deleteOnExit();
    assetsDirectory.toFile().deleteOnExit();
    assetsDirectory.resolve("grizzly-classloader-resource.txt").toFile().deleteOnExit();

    java.net.URLClassLoader staticClassLoader = new java.net.URLClassLoader(new java.net.URL[] {classLoaderRoot.toUri().toURL()}, null);

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("grizzlyServer", GrizzlyInitializingBean.class, () -> {

        GrizzlyInitializingBean bean = new GrizzlyInitializingBean();

        bean.setHost("127.0.0.1");
        bean.setPort(port);
        // Exercising the optional listener-configuration branches alongside the class-loader resource handler.
        bean.setMaxHttpHeaderSize(16384);
        bean.setInitialWorkerPoolSize(2);
        bean.setMaximumWorkerPoolSize(8);
        bean.setDebug(true);

        ClassLoaderResourceOption classLoaderResourceOption = new ClassLoaderResourceOption();
        classLoaderResourceOption.setStaticClassLoader(staticClassLoader);
        classLoaderResourceOption.setStaticPath("/assets");

        WebApplicationOption webApplicationOption = new WebApplicationOption();
        webApplicationOption.setContextPath("/cl");
        webApplicationOption.setClassLoaderResourceOption(classLoaderResourceOption);

        bean.setWebApplicationOptions(new WebApplicationOption[] {webApplicationOption});

        return bean;
      });

      applicationContext.refresh();

      HttpResponse<String> mappedResponse = get(port, "/cl/assets/grizzly-classloader-resource.txt");
      HttpResponse<String> missingResponse = get(port, "/cl/assets/no-such-file.txt");

      Assert.assertEquals(mappedResponse.statusCode(), 200);
      Assert.assertEquals(mappedResponse.body().trim(), "classloader-resource-content");
      Assert.assertEquals(missingResponse.statusCode(), 404);
    }
  }
}
