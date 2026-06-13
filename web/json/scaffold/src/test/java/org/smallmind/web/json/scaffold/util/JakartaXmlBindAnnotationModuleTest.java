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
package org.smallmind.web.json.scaffold.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers {@link JakartaXmlBindAnnotationModule}: default priority/inclusion, the chaining setters, the
 * module name and version, and registration through a {@link JsonMapper} at both the {@code PRIMARY}
 * (insert) and {@code SECONDARY} (append) priority branches of {@code setupModule}.
 */
@Test(groups = "unit")
public class JakartaXmlBindAnnotationModuleTest {

  public void testDefaults () {

    JakartaXmlBindAnnotationModule module = new JakartaXmlBindAnnotationModule();

    Assert.assertEquals(module.getPriority(), JakartaXmlBindAnnotationModule.Priority.PRIMARY);
    Assert.assertEquals(module.getNonNillableInclusion(), JsonInclude.Include.ALWAYS);
  }

  public void testChainingSetters () {

    JakartaXmlBindAnnotationModule module = new JakartaXmlBindAnnotationModule();

    Assert.assertSame(module.setPriority(JakartaXmlBindAnnotationModule.Priority.SECONDARY), module);
    Assert.assertSame(module.setNonNillableInclusion(JsonInclude.Include.NON_NULL), module);

    Assert.assertEquals(module.getPriority(), JakartaXmlBindAnnotationModule.Priority.SECONDARY);
    Assert.assertEquals(module.getNonNillableInclusion(), JsonInclude.Include.NON_NULL);
  }

  public void testModuleNameAndVersion () {

    JakartaXmlBindAnnotationModule module = new JakartaXmlBindAnnotationModule();

    Assert.assertEquals(module.getModuleName(), JakartaXmlBindAnnotationModule.class.getSimpleName());
    Assert.assertNotNull(module.version());
  }

  public void testRegistersAtPrimaryPriority () {

    JsonMapper.builder().addModule(new JakartaXmlBindAnnotationModule().setPriority(JakartaXmlBindAnnotationModule.Priority.PRIMARY)).build();
  }

  public void testRegistersAtSecondaryPriority () {

    JsonMapper.builder().addModule(new JakartaXmlBindAnnotationModule().setPriority(JakartaXmlBindAnnotationModule.Priority.SECONDARY)).build();
  }
}
