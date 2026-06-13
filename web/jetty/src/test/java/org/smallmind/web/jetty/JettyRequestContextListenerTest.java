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
package org.smallmind.web.jetty;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Integration coverage for {@link JettyRequestContextListener}, verifying that request initialization binds
 * Spring request attributes and the request locale to the current thread, that destruction unbinds them, and
 * that a non-HTTP request is rejected.
 */
@Test(groups = "integration")
public class JettyRequestContextListenerTest {

  private static ServletContext servletContextProxy () {

    return (ServletContext)Proxy.newProxyInstance(
      JettyRequestContextListenerTest.class.getClassLoader(),
      new Class<?>[] {ServletContext.class},
      (proxy, method, args) -> {

        if ("toString".equals(method.getName())) {

          return "ServletContextProxy";
        }

        return boolean.class.equals(method.getReturnType()) ? Boolean.FALSE : null;
      });
  }

  private static HttpServletRequest httpRequestProxy (Locale locale) {

    Map<String, Object> attributes = new HashMap<>();

    return (HttpServletRequest)Proxy.newProxyInstance(
      JettyRequestContextListenerTest.class.getClassLoader(),
      new Class<?>[] {HttpServletRequest.class},
      new InvocationHandler() {

        @Override
        public Object invoke (Object proxy, Method method, Object[] args) {

          switch (method.getName()) {
            case "getLocale":
              return locale;
            case "setAttribute":
              attributes.put((String)args[0], args[1]);

              return null;
            case "getAttribute":
              return attributes.get(args[0]);
            case "removeAttribute":
              attributes.remove(args[0]);

              return null;
            case "getServletContext":
              return null;
            case "hashCode":
              return System.identityHashCode(proxy);
            case "equals":
              return proxy == args[0];
            case "toString":
              return "HttpServletRequestProxy";
            default:
              Class<?> returnType = method.getReturnType();

              return boolean.class.equals(returnType) ? Boolean.FALSE : null;
          }
        }
      });
  }

  @AfterMethod
  public void clearContext () {

    RequestContextHolder.resetRequestAttributes();
    LocaleContextHolder.resetLocaleContext();
  }

  public void testRequestInitializedBindsAttributesAndLocale () {

    JettyRequestContextListener listener = new JettyRequestContextListener();
    HttpServletRequest request = httpRequestProxy(Locale.CANADA);

    listener.requestInitialized(new ServletRequestEvent(servletContextProxy(), request));

    Assert.assertNotNull(RequestContextHolder.getRequestAttributes());
    Assert.assertTrue(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes);
    Assert.assertSame(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest(), request);
    Assert.assertEquals(LocaleContextHolder.getLocale(), Locale.CANADA);
  }

  public void testRequestDestroyedUnbindsAttributesAndLocale () {

    JettyRequestContextListener listener = new JettyRequestContextListener();
    HttpServletRequest request = httpRequestProxy(Locale.FRANCE);
    ServletRequestEvent event = new ServletRequestEvent(servletContextProxy(), request);

    listener.requestInitialized(event);
    Assert.assertNotNull(RequestContextHolder.getRequestAttributes());

    listener.requestDestroyed(event);

    Assert.assertNull(RequestContextHolder.getRequestAttributes());
    Assert.assertEquals(LocaleContextHolder.getLocale(), Locale.getDefault());
  }

  public void testRequestDestroyedWithoutInitializationIsSafe () {

    JettyRequestContextListener listener = new JettyRequestContextListener();
    HttpServletRequest request = httpRequestProxy(Locale.US);

    // No prior requestInitialized; there are no bound attributes to clear.
    listener.requestDestroyed(new ServletRequestEvent(servletContextProxy(), request));

    Assert.assertNull(RequestContextHolder.getRequestAttributes());
  }

  public void testRequestInitializedRejectsNonHttpRequest () {

    JettyRequestContextListener listener = new JettyRequestContextListener();
    ServletRequest plainRequest = (ServletRequest)Proxy.newProxyInstance(
      JettyRequestContextListenerTest.class.getClassLoader(),
      new Class<?>[] {ServletRequest.class},
      (proxy, method, args) -> {

        if ("toString".equals(method.getName())) {

          return "ServletRequestProxy";
        }

        return boolean.class.equals(method.getReturnType()) ? Boolean.FALSE : null;
      });

    Assert.assertThrows(IllegalArgumentException.class, () -> listener.requestInitialized(new ServletRequestEvent(servletContextProxy(), plainRequest)));
  }
}
