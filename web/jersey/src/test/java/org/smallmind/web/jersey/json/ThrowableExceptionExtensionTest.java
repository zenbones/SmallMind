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
package org.smallmind.web.jersey.json;

import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives {@link ThrowableExceptionExtension#apply} against a real Jersey {@link ResourceConfig} and asserts that it
 * sets the {@code setStatusOverSendError} property and registers a {@link ThrowableExceptionMapper} instance. Also
 * confirms the inherited priority accessor and that the configured mappers/logging flags do not affect registration.
 */
@Test(groups = "unit")
public class ThrowableExceptionExtensionTest {

  private ThrowableExceptionMapper findRegisteredMapper (ResourceConfig resourceConfig) {

    for (Object instance : resourceConfig.getInstances()) {
      if (instance instanceof ThrowableExceptionMapper) {

        return (ThrowableExceptionMapper)instance;
      }
    }

    return null;
  }

  public void testApplyRegistersMapperAndSetsProperty () {

    ThrowableExceptionExtension extension = new ThrowableExceptionExtension();
    ResourceConfig resourceConfig = new ResourceConfig();

    extension.apply(resourceConfig);

    Assert.assertEquals(resourceConfig.getProperty("jersey.config.server.response.setStatusOverSendError"), "true");
    Assert.assertNotNull(findRegisteredMapper(resourceConfig));
  }

  public void testApplyHonorsConfiguredPriority () {

    ThrowableExceptionExtension extension = new ThrowableExceptionExtension();

    extension.setPriority(73);

    Assert.assertEquals(extension.getPriority(), 73);

    ResourceConfig resourceConfig = new ResourceConfig();

    extension.apply(resourceConfig);

    Assert.assertNotNull(findRegisteredMapper(resourceConfig));
  }

  public void testApplyWithMappersAndLoggingFlag () {

    ThrowableExceptionExtension extension = new ThrowableExceptionExtension();

    extension.setLogUnclassifiedErrors(true);
    extension.setMappers(new ExceptionMapper[] {new FaultTeapotMapper()});

    ResourceConfig resourceConfig = new ResourceConfig();

    extension.apply(resourceConfig);

    Assert.assertEquals(resourceConfig.getProperty("jersey.config.server.response.setStatusOverSendError"), "true");
    Assert.assertNotNull(findRegisteredMapper(resourceConfig));
  }

  private static final class FaultTeapotMapper extends ConcreteExceptionMapper<IllegalStateException> {

    @Override
    public jakarta.ws.rs.core.Response toResponse (IllegalStateException exception) {

      return jakarta.ws.rs.core.Response.status(418).build();
    }
  }
}
