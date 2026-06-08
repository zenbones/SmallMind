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
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Pipeline;
import org.smallmind.mongodb.throng.query.Projections;
import org.smallmind.mongodb.throng.query.Sort;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for {@link ThrongClient#aggregate} covering shape-preserving pipelines
 * (match, sort, project, skip, limit, unwind), shape-changing pipelines that reshape into a
 * different result entity ({@code $group}), and the raw-stage escape hatch.
 */
@Test(groups = "integration")
public class ThrongClientPipelineIntegrationTest extends AbstractGroundwaterTest {

  private MongoClient mongoClient;
  private ThrongClient throngClient;

  public ThrongClientPipelineIntegrationTest () {

    super(DockerApplication.MONGODB);
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    super.beforeClass();

    mongoClient = MongoClients.create("mongodb://root:secret@localhost:27017/?authSource=admin");
    throngClient = new ThrongClient(mongoClient, "throng_pipeline_test", new ThrongOptions(false, false, false), Sale.class, CategoryRollup.class);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (mongoClient != null) {
      mongoClient.close();
    }

    super.afterClass();
  }

  @BeforeMethod
  public void clearSales () {

    throngClient.delete(Sale.class, Filter.empty(), new DeleteOptions());
  }

  public void testMatchPipelineReturnsOnlyMatchingEntities () {

    throngClient.insert(new Sale("S1", "tools", 100), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "parts", 50), new InsertOneOptions());
    throngClient.insert(new Sale("S3", "tools", 75), new InsertOneOptions());

    List<Sale> results = throngClient.aggregate(Sale.class, Pipeline.begin().match(Filter.where("category").eq("tools"))).asList();

    Assert.assertEquals(results.size(), 2);
    for (Sale sale : results) {
      Assert.assertEquals(sale.getCategory(), "tools");
    }
  }

  public void testMatchSortLimitPipelineReturnsCorrectTopN () {

    throngClient.insert(new Sale("S1", "tools", 100), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "tools", 200), new InsertOneOptions());
    throngClient.insert(new Sale("S3", "tools", 50), new InsertOneOptions());
    throngClient.insert(new Sale("S4", "parts", 999), new InsertOneOptions());

    List<Sale> topTwo = throngClient.aggregate(
      Sale.class,
      Pipeline.begin()
        .match(Filter.where("category").eq("tools"))
        .sort(Sort.on().desc("amount"))
        .limit(2)).asList();

    Assert.assertEquals(topTwo.size(), 2);
    Assert.assertEquals(topTwo.get(0).getAmount(), Integer.valueOf(200));
    Assert.assertEquals(topTwo.get(1).getAmount(), Integer.valueOf(100));
  }

  public void testSkipAndLimitPipelinePagesThroughSortedResults () {

    throngClient.insert(new Sale("S1", "x", 1), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "x", 2), new InsertOneOptions());
    throngClient.insert(new Sale("S3", "x", 3), new InsertOneOptions());
    throngClient.insert(new Sale("S4", "x", 4), new InsertOneOptions());
    throngClient.insert(new Sale("S5", "x", 5), new InsertOneOptions());

    List<Sale> page = throngClient.aggregate(
      Sale.class,
      Pipeline.begin()
        .sort(Sort.on().asc("amount"))
        .skip(2)
        .limit(2)).asList();

    Assert.assertEquals(page.size(), 2);
    Assert.assertEquals(page.get(0).getAmount(), Integer.valueOf(3));
    Assert.assertEquals(page.get(1).getAmount(), Integer.valueOf(4));
  }

  public void testProjectionPipelineOmitsExcludedFields () {

    throngClient.insert(new Sale("S1", "tools", 100), new InsertOneOptions());

    List<Sale> results = throngClient.aggregate(
      Sale.class,
      Pipeline.begin().project(Projections.with().include("category"))).asList();

    Assert.assertEquals(results.size(), 1);
    Assert.assertEquals(results.get(0).getCategory(), "tools");
    Assert.assertNull(results.get(0).getAmount());
  }

  public void testGroupPipelineWithTwoClassVariantRollsUpByCategory () {

    throngClient.insert(new Sale("S1", "tools", 100), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "tools", 200), new InsertOneOptions());
    throngClient.insert(new Sale("S3", "parts", 50), new InsertOneOptions());
    throngClient.insert(new Sale("S4", "parts", 25), new InsertOneOptions());
    throngClient.insert(new Sale("S5", "consumables", 10), new InsertOneOptions());

    List<CategoryRollup> rollups = throngClient.aggregate(
      Sale.class, CategoryRollup.class,
      Pipeline.begin()
        .group("$category",
          Accumulators.sum("total", "$amount"),
          Accumulators.sum("count", 1))
        .sort(Sort.on().desc("total"))).asList();

    Assert.assertEquals(rollups.size(), 3);
    Assert.assertEquals(rollups.get(0).getId(), "tools");
    Assert.assertEquals(rollups.get(0).getTotal(), Integer.valueOf(300));
    Assert.assertEquals(rollups.get(0).getCount(), Integer.valueOf(2));
    Assert.assertEquals(rollups.get(1).getId(), "parts");
    Assert.assertEquals(rollups.get(1).getTotal(), Integer.valueOf(75));
    Assert.assertEquals(rollups.get(2).getId(), "consumables");
  }

  public void testGroupPipelineWithNullIdProducesSingleRollup () {

    throngClient.insert(new Sale("S1", "x", 10), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "y", 20), new InsertOneOptions());
    throngClient.insert(new Sale("S3", "z", 30), new InsertOneOptions());

    List<CategoryRollup> rollups = throngClient.aggregate(
      Sale.class, CategoryRollup.class,
      Pipeline.begin().group(null,
        Accumulators.sum("total", "$amount"),
        Accumulators.sum("count", 1))).asList();

    Assert.assertEquals(rollups.size(), 1);
    Assert.assertNull(rollups.get(0).getId());
    Assert.assertEquals(rollups.get(0).getTotal(), Integer.valueOf(60));
    Assert.assertEquals(rollups.get(0).getCount(), Integer.valueOf(3));
  }

  public void testRawStageEscapeHatchExecutesAgainstCollection () {

    throngClient.insert(new Sale("S1", "tools", 100), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "tools", 200), new InsertOneOptions());

    List<Sale> results = throngClient.aggregate(
      Sale.class,
      Pipeline.begin()
        .stage(org.bson.BsonDocument.parse("{ \"$match\": { \"category\": \"tools\" } }"))
        .sort(Sort.on().asc("amount"))).asList();

    Assert.assertEquals(results.size(), 2);
    Assert.assertEquals(results.get(0).getAmount(), Integer.valueOf(100));
    Assert.assertEquals(results.get(1).getAmount(), Integer.valueOf(200));
  }

  public void testEmptyPipelineReturnsAllSourceDocumentsUnchanged () {

    throngClient.insert(new Sale("S1", "x", 1), new InsertOneOptions());
    throngClient.insert(new Sale("S2", "y", 2), new InsertOneOptions());

    List<Sale> results = throngClient.aggregate(Sale.class, Pipeline.begin()).asList();

    Assert.assertEquals(results.size(), 2);
  }

  @Entity("sales")
  public static class Sale {

    @Id
    private String id;

    @Property
    private String category;

    @Property
    private Integer amount;

    public Sale () {

    }

    public Sale (String id, String category, Integer amount) {

      this.id = id;
      this.category = category;
      this.amount = amount;
    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getCategory () {

      return category;
    }

    public void setCategory (String category) {

      this.category = category;
    }

    public Integer getAmount () {

      return amount;
    }

    public void setAmount (Integer amount) {

      this.amount = amount;
    }
  }

  @Entity("category_rollups")
  public static class CategoryRollup {

    @Id
    private String id;

    @Property
    private Integer total;

    @Property
    private Integer count;

    public CategoryRollup () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public Integer getTotal () {

      return total;
    }

    public void setTotal (Integer total) {

      this.total = total;
    }

    public Integer getCount () {

      return count;
    }

    public void setCount (Integer count) {

      this.count = count;
    }
  }
}
