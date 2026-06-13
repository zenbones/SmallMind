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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Covers the byte-array, stream, parser-token, and tree-node read/write overloads of {@link JsonCodec}
 * left untouched by {@link JsonCodecTest}, plus the array-node branch of the deep {@code copy}.
 */
@Test(groups = "unit")
public class JsonCodecBranchTest {

  public void testReadAsJsonNodeFromBytes ()
    throws IOException {

    JsonNode node = JsonCodec.readAsJsonNode("{\"x\":1}".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(node.get("x").intValue(), 1);
  }

  public void testReadAsJsonNodeFromStream ()
    throws IOException {

    JsonNode node = JsonCodec.readAsJsonNode(new ByteArrayInputStream("{\"x\":2}".getBytes(StandardCharsets.UTF_8)));

    Assert.assertEquals(node.get("x").intValue(), 2);
  }

  public void testReadFromBytes ()
    throws IOException {

    JsonCodecTest.Point point = JsonCodec.read("{\"x\":3,\"y\":4}".getBytes(StandardCharsets.UTF_8), JsonCodecTest.Point.class);

    Assert.assertEquals(point.getX(), 3);
    Assert.assertEquals(point.getY(), 4);
  }

  public void testReadFromByteSlice ()
    throws IOException {

    byte[] padded = ("XX" + "{\"x\":5,\"y\":6}").getBytes(StandardCharsets.UTF_8);

    JsonCodecTest.Point point = JsonCodec.read(padded, 2, padded.length - 2, JsonCodecTest.Point.class);

    Assert.assertEquals(point.getX(), 5);
    Assert.assertEquals(point.getY(), 6);
  }

  public void testReadFromStream ()
    throws IOException {

    JsonCodecTest.Point point = JsonCodec.read(new ByteArrayInputStream("{\"x\":7,\"y\":8}".getBytes(StandardCharsets.UTF_8)), JsonCodecTest.Point.class);

    Assert.assertEquals(point.getX(), 7);
    Assert.assertEquals(point.getY(), 8);
  }

  public void testReadFromJsonNode () {

    JsonNode node = JsonCodec.readAsJsonNode("{\"x\":9,\"y\":10}");
    JsonCodecTest.Point point = JsonCodec.read(node, JsonCodecTest.Point.class);

    Assert.assertEquals(point.getX(), 9);
    Assert.assertEquals(point.getY(), 10);
  }

  public void testWriteAsJsonNode () {

    JsonCodecTest.Point point = new JsonCodecTest.Point();
    point.setX(11);
    point.setY(12);

    JsonNode node = JsonCodec.writeAsJsonNode(point);

    Assert.assertEquals(node.get("x").intValue(), 11);
    Assert.assertEquals(node.get("y").intValue(), 12);
  }

  public void testWriteToStream ()
    throws IOException {

    JsonCodecTest.Point point = new JsonCodecTest.Point();
    point.setX(13);
    point.setY(14);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    JsonCodec.writeToStream(outputStream, point);

    JsonCodecTest.Point recovered = JsonCodec.read(outputStream.toByteArray(), JsonCodecTest.Point.class);

    Assert.assertEquals(recovered.getX(), 13);
    Assert.assertEquals(recovered.getY(), 14);
  }

  public void testCopyArrayNode () {

    JsonNode original = JsonCodec.readAsJsonNode("[1,2,3]");
    JsonNode copy = JsonCodec.copy(original);

    Assert.assertEquals(copy, original);
    Assert.assertNotSame(copy, original);
  }
}
