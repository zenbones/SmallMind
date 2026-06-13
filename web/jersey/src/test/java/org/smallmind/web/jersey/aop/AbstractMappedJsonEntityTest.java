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
package org.smallmind.web.jersey.aop;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers name-based parameter resolution in {@link AbstractMappedJsonEntity}, including conversion, the
 * absent-key {@code null} branch, and {@link XmlJavaTypeAdapter}-driven unmarshalling.
 */
@Test(groups = "unit")
public class AbstractMappedJsonEntityTest {

  private static class MappedEntity extends AbstractMappedJsonEntity {

    public MappedEntity () {

    }

    public MappedEntity (Map<String, Object> arguments) {

      super(arguments);
    }
  }

  @XmlJavaTypeAdapter(StringToLengthAdapter.class)
  private String adapted;

  private ParameterAnnotations plainAnnotations () {

    return new ParameterAnnotations(new Annotation[0]);
  }

  private ParameterAnnotations adapterAnnotations ()
    throws NoSuchFieldException {

    return new ParameterAnnotations(AbstractMappedJsonEntityTest.class.getDeclaredField("adapted").getAnnotations());
  }

  private Map<String, Object> sampleMap () {

    Map<String, Object> arguments = new HashMap<>();

    arguments.put("count", "9");
    arguments.put("word", "hello");

    return arguments;
  }

  public void testArgumentsAccessors () {

    MappedEntity entity = new MappedEntity();
    Map<String, Object> arguments = sampleMap();

    entity.setArguments(arguments);
    Assert.assertSame(entity.getArguments(), arguments);
  }

  public void testGetParameterConverts () {

    MappedEntity entity = new MappedEntity(sampleMap());

    Assert.assertEquals(entity.getParameter("count", Integer.class, plainAnnotations()), Integer.valueOf(9));
  }

  public void testAbsentKeyReturnsNull () {

    MappedEntity entity = new MappedEntity(sampleMap());

    Assert.assertNull(entity.getParameter("missing", String.class, plainAnnotations()));
  }

  public void testAdapterApplied ()
    throws Exception {

    MappedEntity entity = new MappedEntity(sampleMap());

    Assert.assertEquals(entity.getParameter("word", Integer.class, adapterAnnotations()), Integer.valueOf(5));
  }
}
