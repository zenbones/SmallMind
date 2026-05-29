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
package org.smallmind.nutsnbolts.spring.property;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PropertyHandlersTest {

  public void testPropertyFileTypeForExtensionMatchesPropertiesAndYaml () {

    Assert.assertEquals(PropertyFileType.forExtension("properties"), PropertyFileType.PROPERTIES);
    Assert.assertEquals(PropertyFileType.forExtension("yaml"), PropertyFileType.YAML);
    Assert.assertEquals(PropertyFileType.forExtension("yml"), PropertyFileType.YAML);
    Assert.assertNull(PropertyFileType.forExtension("toml"));
  }

  public void testPropertiesHandlerIteratesAllEntries ()
    throws Exception {

    String body = "first=alpha\nsecond=beta\n";
    PropertyHandler<?> handler = PropertyFileType.PROPERTIES.getPropertyHandler(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    Map<String, Object> seen = new HashMap<>();

    for (Object entry : handler) {

      PropertyEntry typed = (PropertyEntry)entry;

      seen.put(typed.getKey(), typed.getValue());
    }

    Assert.assertEquals(seen.get("first"), "alpha");
    Assert.assertEquals(seen.get("second"), "beta");
  }

  public void testYamlHandlerFlattensNestedKeys ()
    throws Exception {

    String body = "service:\n  name: api\n  port: 8080\n";
    PropertyHandler<?> handler = PropertyFileType.YAML.getPropertyHandler(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
    Map<String, Object> seen = new HashMap<>();

    for (Object entry : handler) {

      PropertyEntry typed = (PropertyEntry)entry;

      seen.put(typed.getKey(), typed.getValue());
    }

    Assert.assertEquals(String.valueOf(seen.get("service.name")), "api");
    Assert.assertEquals(String.valueOf(seen.get("service.port")), "8080");
  }
}
