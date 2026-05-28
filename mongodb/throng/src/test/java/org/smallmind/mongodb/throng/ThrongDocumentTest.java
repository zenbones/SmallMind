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
package org.smallmind.mongodb.throng;

import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ThrongDocumentTest {

  public void testAddReturnsThisForFluentChaining () {

    ThrongDocument document = new ThrongDocument();
    ThrongDocument returned = document.add("x", new BsonInt32(1));

    Assert.assertSame(returned, document);
  }

  public void testAddedFieldAppearsInUnderlyingBsonDocument () {

    ThrongDocument document = new ThrongDocument();
    document.add("color", new BsonString("blue"));

    Assert.assertTrue(document.getBsonDocument().containsKey("color"));
    Assert.assertEquals(document.getBsonDocument().getString("color").getValue(), "blue");
  }

  public void testMultipleAddCallsAccumulateInUnderlyingDocument () {

    ThrongDocument document = new ThrongDocument();
    document.add("a", new BsonInt32(1)).add("b", new BsonInt32(2)).add("c", new BsonInt32(3));

    Assert.assertEquals(document.getBsonDocument().size(), 3);
  }

  public void testConstructorWrappingExistingBsonDocumentReturnsSameInstance () {

    BsonDocument backing = new BsonDocument("existing", new BsonString("value"));
    ThrongDocument document = new ThrongDocument(backing);

    Assert.assertSame(document.getBsonDocument(), backing);
  }

  public void testAddMutatesWrappedBsonDocument () {

    BsonDocument backing = new BsonDocument();
    ThrongDocument document = new ThrongDocument(backing);
    document.add("k", new BsonInt32(42));

    Assert.assertTrue(backing.containsKey("k"));
    Assert.assertEquals(backing.getInt32("k").getValue(), 42);
  }
}
