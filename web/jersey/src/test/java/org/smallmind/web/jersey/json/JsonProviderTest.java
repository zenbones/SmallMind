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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.core.MediaType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the {@link JsonProvider} message body reader/writer: unconditional readability/writeability, round-trip
 * read, the size-then-write buffering contract, and the direct-write path when {@code getSize} was not called first.
 */
@Test(groups = "unit")
public class JsonProviderTest {

  public static class Bean {

    private String name;
    private int count;

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public int getCount () {

      return count;
    }

    public void setCount (int count) {

      this.count = count;
    }
  }

  public void testIsReadableAndWriteableAlwaysTrue () {

    JsonProvider provider = new JsonProvider();

    Assert.assertTrue(provider.isReadable(Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE));
    Assert.assertTrue(provider.isWriteable(Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE));
  }

  public void testReadFrom ()
    throws Exception {

    ByteArrayInputStream entityStream = new ByteArrayInputStream("{\"name\":\"abc\",\"count\":7}".getBytes(StandardCharsets.UTF_8));
    Object result = new JsonProvider().readFrom((Class)Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE, null, entityStream);

    Assert.assertTrue(result instanceof Bean);
    Assert.assertEquals(((Bean)result).getName(), "abc");
    Assert.assertEquals(((Bean)result).getCount(), 7);
  }

  public void testSizeThenWriteUsesBuffer ()
    throws Exception {

    JsonProvider provider = new JsonProvider();
    Bean bean = new Bean();

    bean.setName("xyz");
    bean.setCount(3);

    long size = provider.getSize(bean, Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE);
    Assert.assertTrue(size > 0);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    provider.writeTo(bean, Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE, null, outputStream);

    Assert.assertEquals(outputStream.size(), size);
    Assert.assertTrue(outputStream.toString(StandardCharsets.UTF_8).contains("xyz"));
  }

  public void testWriteWithoutPriorSizeStillSerializes ()
    throws Exception {

    JsonProvider provider = new JsonProvider();
    Bean bean = new Bean();

    bean.setName("direct");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    provider.writeTo(bean, Bean.class, Bean.class, null, MediaType.APPLICATION_JSON_TYPE, null, outputStream);

    Assert.assertTrue(outputStream.toString(StandardCharsets.UTF_8).contains("direct"));
  }
}
