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
package org.smallmind.web.grizzly.option;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class WebApplicationOptionTest {

  public void testDefaults () {

    WebApplicationOption option = new WebApplicationOption();

    Assert.assertEquals(option.getContextPath(), "/context");
    Assert.assertEquals(option.getSoapPath(), "/soap");
    Assert.assertNull(option.getClassLoaderResourceOption());
    Assert.assertNull(option.getDocumentRootOption());
    Assert.assertNull(option.getJaxRSOption());
    Assert.assertNull(option.getSpringSupportOption());
    Assert.assertNull(option.getWebSocketOption());
  }

  public void testAccessorRoundTrip () {

    WebApplicationOption option = new WebApplicationOption();
    ClassLoaderResourceOption classLoaderResourceOption = new ClassLoaderResourceOption();
    DocumentRootOption documentRootOption = new DocumentRootOption();
    JaxRSOption jaxRSOption = new JaxRSOption();
    SpringSupportOption springSupportOption = new SpringSupportOption();
    WebSocketOption webSocketOption = new WebSocketOption();

    option.setContextPath("/app");
    option.setSoapPath("/services");
    option.setClassLoaderResourceOption(classLoaderResourceOption);
    option.setDocumentRootOption(documentRootOption);
    option.setJaxRSOption(jaxRSOption);
    option.setSpringSupportOption(springSupportOption);
    option.setWebSocketOption(webSocketOption);

    Assert.assertEquals(option.getContextPath(), "/app");
    Assert.assertEquals(option.getSoapPath(), "/services");
    Assert.assertSame(option.getClassLoaderResourceOption(), classLoaderResourceOption);
    Assert.assertSame(option.getDocumentRootOption(), documentRootOption);
    Assert.assertSame(option.getJaxRSOption(), jaxRSOption);
    Assert.assertSame(option.getSpringSupportOption(), springSupportOption);
    Assert.assertSame(option.getWebSocketOption(), webSocketOption);
  }
}
