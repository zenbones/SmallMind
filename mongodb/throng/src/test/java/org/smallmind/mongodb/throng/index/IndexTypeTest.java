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
package org.smallmind.mongodb.throng.index;

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class IndexTypeTest {

  public void testAscendingIndexAssignsValueOneToField () {

    BsonDocument doc = IndexType.ASCENDING.construct("createdAt").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getInt32("createdAt").getValue(), 1);
  }

  public void testDescendingIndexAssignsValueNegativeOneToField () {

    BsonDocument doc = IndexType.DESCENDING.construct("createdAt").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getInt32("createdAt").getValue(), -1);
  }

  public void testHashedIndexAssignsHashedMarkerToField () {

    BsonDocument doc = IndexType.HASHED.construct("partitionKey").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getString("partitionKey").getValue(), "hashed");
  }

  public void testTextIndexAssignsTextMarkerToField () {

    BsonDocument doc = IndexType.TEXT.construct("body").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getString("body").getValue(), "text");
  }

  public void testGeo2dIndexAssignsGeo2dMarkerToField () {

    BsonDocument doc = IndexType.GEO2D.construct("location").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getString("location").getValue(), "2d");
  }

  public void testGeo2dSphereIndexAssignsGeo2dSphereMarkerToField () {

    BsonDocument doc = IndexType.GEO2DSPHERE.construct("location").toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry());

    Assert.assertEquals(doc.getString("location").getValue(), "2dsphere");
  }
}
