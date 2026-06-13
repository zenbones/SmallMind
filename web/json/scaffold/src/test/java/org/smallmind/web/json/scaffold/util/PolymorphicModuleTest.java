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

import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers {@link PolymorphicModule} (name/version/registration) and the directly testable surface of
 * {@link PolymorphicValueInstantiator}: {@code canCreateUsingDefault} and the missing-thread-local
 * error path of {@code createUsingDefault}.
 */
@Test(groups = "unit")
public class PolymorphicModuleTest {

  public void testModuleNameAndVersion () {

    PolymorphicModule module = new PolymorphicModule();

    Assert.assertEquals(module.getModuleName(), PolymorphicModule.class.getName());
    Assert.assertNotNull(module.version());
  }

  public void testModuleRegisters () {

    JsonMapper.builder().addModule(new PolymorphicModule()).build();
  }

  public void testInstantiatorCanCreateUsingDefault () {

    PolymorphicValueInstantiator instantiator = newInstantiator();

    Assert.assertTrue(instantiator.canCreateUsingDefault());
  }

  @Test(expectedExceptions = JAXBProcessingException.class)
  public void testCreateUsingDefaultWithoutThreadLocalFails () {

    // Ensure no leftover thread-local instance from any prior marshalling on this thread.
    PolymorphicValueInstantiator.setPolymorphicInstance(null);

    newInstantiator().createUsingDefault(null);
  }

  private PolymorphicValueInstantiator newInstantiator () {

    JsonMapper mapper = JsonMapper.builder().build();
    DeserializationConfig config = mapper.deserializationConfig();
    JavaType javaType = mapper.constructType(PolymorphicAnimalCat.class);

    return new PolymorphicValueInstantiator(config, javaType, PolymorphicAnimalCat.class);
  }
}
