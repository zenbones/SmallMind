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

import java.util.List;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import or.smallmind.testbench.logger.TestLoggerConfiguration;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.mongodb.throng.query.Sort;
import org.smallmind.mongodb.throng.query.Updates;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ThrongClient} that exercise the full encode/decode pipeline,
 * filtering, sorting, paging, update, and delete operations against a live MongoDB instance
 * started via Docker.
 */
@Test(groups = "integration")
public class ThrongClientIntegrationTest extends AbstractGroundwaterTest {

  private ThrongClient throngClient;

  public ThrongClientIntegrationTest () {

    super(DockerApplication.MONGODB);
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    super.beforeClass();

    MongoClient mongoClient;

    mongoClient = MongoClients.create("mongodb://root:secret@localhost:27017/?authSource=admin");
    throngClient = new ThrongClient(mongoClient, "throng_test", new ThrongOptions(false, false, false), Widget.class);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  @BeforeMethod
  public void clearWidgets () {

    throngClient.delete(Widget.class, Filter.empty(), new DeleteOptions());
  }

  public void testInsertAndFindOneRoundtrip () {

    throngClient.insert(new Widget("w1", "Hammer", 5, "tools"), new InsertOneOptions());

    Widget found = throngClient.findOne(Widget.class, Query.with().filter(Filter.where("_id").eq("w1")));

    Assert.assertNotNull(found);
    Assert.assertEquals(found.getId(), "w1");
    Assert.assertEquals(found.getName(), "Hammer");
    Assert.assertEquals(found.getQuantity(), Integer.valueOf(5));
    Assert.assertEquals(found.getCategory(), "tools");
  }

  public void testFindOneReturnsNullWhenNoDocumentMatches () {

    Widget found = throngClient.findOne(Widget.class, Query.with().filter(Filter.where("_id").eq("nonexistent")));

    Assert.assertNull(found);
  }

  public void testCountWithEqualsFilterMatchesCorrectSubset () {

    throngClient.insert(new Widget("w1", "Hammer", 5, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w2", "Drill", 3, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w3", "Bolt", 100, "parts"), new InsertOneOptions());

    Assert.assertEquals(throngClient.count(Widget.class, Filter.where("category").eq("tools")), 2L);
    Assert.assertEquals(throngClient.count(Widget.class, Filter.empty()), 3L);
  }

  public void testFindAllSortedByQuantityAscending () {

    throngClient.insert(new Widget("w1", "C-Widget", 30, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("w2", "A-Widget", 10, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("w3", "B-Widget", 20, "x"), new InsertOneOptions());

    List<Widget> results = throngClient.find(Widget.class, Query.with().filter(Filter.empty()).sort(Sort.on().asc("quantity"))).asList();

    Assert.assertEquals(results.size(), 3);
    Assert.assertEquals(results.get(0).getQuantity(), Integer.valueOf(10));
    Assert.assertEquals(results.get(1).getQuantity(), Integer.valueOf(20));
    Assert.assertEquals(results.get(2).getQuantity(), Integer.valueOf(30));
  }

  public void testFindWithSkipAndLimitReturnsCorrectPage () {

    throngClient.insert(new Widget("a", "Alpha", 1, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("b", "Beta", 2, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("c", "Gamma", 3, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("d", "Delta", 4, "x"), new InsertOneOptions());
    throngClient.insert(new Widget("e", "Epsilon", 5, "x"), new InsertOneOptions());

    List<Widget> results = throngClient.find(Widget.class,
      Query.with().filter(Filter.empty()).sort(Sort.on().asc("_id")).skip(1).limit(2)).asList();

    Assert.assertEquals(results.size(), 2);
    Assert.assertEquals(results.get(0).getId(), "b");
    Assert.assertEquals(results.get(1).getId(), "c");
  }

  public void testUpdateSetsFieldValueOnMatchingDocuments () {

    throngClient.insert(new Widget("w1", "OldName", 5, "tools"), new InsertOneOptions());

    throngClient.update(Widget.class, Filter.where("_id").eq("w1"), Updates.of().set("name", "NewName"), new UpdateOptions());

    Widget updated = throngClient.findOne(Widget.class, Query.with().filter(Filter.where("_id").eq("w1")));

    Assert.assertNotNull(updated);
    Assert.assertEquals(updated.getName(), "NewName");
    Assert.assertEquals(updated.getQuantity(), Integer.valueOf(5));
  }

  public void testDeleteByFilterRemovesOnlyMatchingDocuments () {

    throngClient.insert(new Widget("w1", "Hammer", 5, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w2", "Drill", 3, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w3", "Bolt", 100, "parts"), new InsertOneOptions());

    throngClient.delete(Widget.class, Filter.where("category").eq("tools"), new DeleteOptions());

    Assert.assertEquals(throngClient.count(Widget.class, Filter.empty()), 1L);
    Assert.assertNotNull(throngClient.findOne(Widget.class, Query.with().filter(Filter.where("_id").eq("w3"))));
  }

  public void testFindWithInFilterMatchesMultipleValues () {

    throngClient.insert(new Widget("w1", "Hammer", 5, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w2", "Bolt", 100, "parts"), new InsertOneOptions());
    throngClient.insert(new Widget("w3", "Paint", 20, "consumables"), new InsertOneOptions());

    List<Widget> results = throngClient.find(Widget.class,
      Query.with().filter(Filter.where("category").in("tools", "parts"))).asList();

    Assert.assertEquals(results.size(), 2);
  }

  public void testUpsertCreatesDocumentWhenNoneMatchesFilter () {

    UpdateResult result = throngClient.update(Widget.class,
      Filter.where("_id").eq("upserted"),
      Updates.of().set("name", "Created").set("quantity", 1).set("category", "new"),
      new UpdateOptions().upsert(true));

    Assert.assertEquals(result.getModifiedPlusInsertedCount(), 1L);
    Assert.assertEquals(throngClient.count(Widget.class, Filter.empty()), 1L);
  }

  public void testUpdateReturnsModifiedCountForMatchingDocuments () {

    throngClient.insert(new Widget("w1", "Alpha", 1, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w2", "Beta", 2, "tools"), new InsertOneOptions());
    throngClient.insert(new Widget("w3", "Gamma", 3, "parts"), new InsertOneOptions());

    UpdateResult result = throngClient.update(Widget.class,
      Filter.where("category").eq("tools"),
      Updates.of().set("category", "updated-tools"),
      new UpdateOptions());

    Assert.assertEquals(result.getModifiedCount(), 2L);
    Assert.assertEquals(throngClient.count(Widget.class, Filter.where("category").eq("updated-tools")), 2L);
  }

  /**
   * Maps to the {@code widgets} collection.
   */
  @Entity("widgets")
  public static class Widget {

    @Id
    private String id;

    @Property
    private String name;

    @Property
    private Integer quantity;

    @Property
    private String category;

    public Widget () {

    }

    public Widget (String id, String name, Integer quantity, String category) {

      this.id = id;
      this.name = name;
      this.quantity = quantity;
      this.category = category;
    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getName () {

      return name;
    }

    public void setName (String name) {

      this.name = name;
    }

    public Integer getQuantity () {

      return quantity;
    }

    public void setQuantity (Integer quantity) {

      this.quantity = quantity;
    }

    public String getCategory () {

      return category;
    }

    public void setCategory (String category) {

      this.category = category;
    }
  }
}
