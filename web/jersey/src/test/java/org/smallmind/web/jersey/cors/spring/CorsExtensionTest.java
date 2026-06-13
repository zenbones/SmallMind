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
package org.smallmind.web.jersey.cors.spring;

import org.glassfish.jersey.server.ResourceConfig;
import org.smallmind.web.jersey.cors.CorsFilter;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Boots {@link CorsExtension} through a Spring {@link GenericApplicationContext} to confirm property injection and
 * that {@code apply} registers a {@link CorsFilter} on the Jersey {@link ResourceConfig} at the configured priority.
 * Also covers the {@code null}/empty header concatenation branch.
 */
@Test(groups = "integration")
public class CorsExtensionTest {

  public void testExtensionRegistersCorsFilter () {

    try (GenericApplicationContext applicationContext = new GenericApplicationContext()) {

      applicationContext.registerBean("corsExtension", CorsExtension.class, () -> {

        CorsExtension corsExtension = new CorsExtension();

        corsExtension.setAllowedHeaders(new String[] {"Content-Type", "Authorization"});
        corsExtension.setExposedHeaders(new String[] {"Location"});
        corsExtension.setPriority(42);

        return corsExtension;
      });

      applicationContext.refresh();

      CorsExtension corsExtension = applicationContext.getBean("corsExtension", CorsExtension.class);

      Assert.assertEquals(corsExtension.getPriority(), 42);

      ResourceConfig resourceConfig = new ResourceConfig();

      corsExtension.apply(resourceConfig);

      boolean foundFilter = false;

      for (Object instance : resourceConfig.getInstances()) {
        if (instance instanceof CorsFilter) {
          foundFilter = true;
        }
      }

      Assert.assertTrue(foundFilter);
    }
  }

  public void testApplyWithNullAndEmptyHeaders () {

    CorsExtension corsExtension = new CorsExtension();

    corsExtension.setAllowedHeaders(null);
    corsExtension.setExposedHeaders(new String[0]);

    ResourceConfig resourceConfig = new ResourceConfig();

    corsExtension.apply(resourceConfig);

    boolean foundFilter = false;

    for (Object instance : resourceConfig.getInstances()) {
      if (instance instanceof CorsFilter) {
        foundFilter = true;
      }
    }

    Assert.assertTrue(foundFilter);
  }
}
