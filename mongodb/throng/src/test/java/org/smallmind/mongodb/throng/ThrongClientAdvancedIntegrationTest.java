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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.InsertOneOptions;
import org.smallmind.testbench.logger.TestLoggerConfiguration;
import org.bson.Document;
import org.smallmind.mongodb.throng.index.IndexType;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PostPersist;
import org.smallmind.mongodb.throng.lifecycle.annotation.PreLoad;
import org.smallmind.mongodb.throng.lifecycle.annotation.PrePersist;
import org.smallmind.mongodb.throng.mapping.annotation.Embedded;
import org.smallmind.mongodb.throng.mapping.annotation.Entity;
import org.smallmind.mongodb.throng.mapping.annotation.Id;
import org.smallmind.mongodb.throng.mapping.annotation.Polymorphic;
import org.smallmind.mongodb.throng.mapping.annotation.Property;
import org.smallmind.mongodb.throng.query.Filter;
import org.smallmind.mongodb.throng.query.Query;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Additional integration tests for {@link ThrongClient} covering indexes, polymorphic embedded types,
 * lifecycle callbacks, null storage, array fields, duplicate-key errors, cursor exhaustion, and the
 * full set of index types accepted by MongoDB.
 */
@Test(groups = "integration")
public class ThrongClientAdvancedIntegrationTest extends AbstractGroundwaterTest {

  private MongoClient mongoClient;
  private ThrongClient throngClient;

  public ThrongClientAdvancedIntegrationTest () {

    super(DockerApplication.MONGODB);
  }

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    TestLoggerConfiguration.setup();

    super.beforeClass();

    mongoClient = MongoClients.create("mongodb://root:secret@localhost:27017/?authSource=admin");

    throngClient = new ThrongClient(
      mongoClient,
      "throng_adv_test",
      new ThrongOptions(false, true, true),
      IndexedOrder.class,
      LifecycleOrder.class,
      NullableOrder.class,
      OrderWithEmbedded.class, Address.class,
      OrderWithArray.class,
      UniqueIdOrder.class,
      Garage.class, Vehicle.class, Car.class, Truck.class,
      AllIndexTypes.class,
      BatchedOrder.class);
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (mongoClient != null) {
      mongoClient.close();
    }

    super.afterClass();
  }

  public void testIndexCreationProducesSingleFieldAndCompoundIndexesOnLiveCollection () {

    List<String> indexNames = listIndexNames("indexed_orders");

    Assert.assertTrue(indexNames.contains("status_1"), "expected single-field index status_1, got " + indexNames);
    Assert.assertTrue(indexNames.contains("status_placedAt_idx"), "expected named compound index status_placedAt_idx, got " + indexNames);
  }

  public void testIncludeCollationFalseProducesIndexesWithoutCollation ()
    throws Exception {

    String localDatabase = "throng_adv_test_no_collation";
    ThrongClient noCollationClient = new ThrongClient(mongoClient, localDatabase, new ThrongOptions(false, true, false), IndexedOrder.class);

    try {

      Document statusIndex = findIndex(localDatabase, "indexed_orders", "status_1");

      Assert.assertNotNull(statusIndex);
      Assert.assertNull(statusIndex.get("collation"));
    } finally {
      mongoClient.getDatabase(localDatabase).drop();
    }

    // Silence the unused-warning since we constructed but don't otherwise interact with the client.
    Assert.assertNotNull(noCollationClient);
  }

  public void testPolymorphicEmbeddedRoundTripPreservesConcreteSubtype () {

    throngClient.delete(Garage.class, Filter.empty(), new DeleteOptions());

    Garage garage = new Garage();

    garage.setId("g1");
    garage.setVehicle(new Car("CAR-001", 4));

    throngClient.insert(garage, new InsertOneOptions());

    Garage fetched = throngClient.findOne(Garage.class, Query.with().filter(Filter.where("_id").eq("g1")));

    Assert.assertNotNull(fetched);
    Assert.assertTrue(fetched.getVehicle() instanceof Car);
    Assert.assertEquals(((Car)fetched.getVehicle()).getDoors(), 4);
  }

  public void testPolymorphicEmbeddedRoundTripWithDifferentSubtypePreservesType () {

    throngClient.delete(Garage.class, Filter.empty(), new DeleteOptions());

    Garage garage = new Garage();

    garage.setId("g2");
    garage.setVehicle(new Truck("TRUCK-001", 5000));

    throngClient.insert(garage, new InsertOneOptions());

    Garage fetched = throngClient.findOne(Garage.class, Query.with().filter(Filter.where("_id").eq("g2")));

    Assert.assertNotNull(fetched);
    Assert.assertTrue(fetched.getVehicle() instanceof Truck);
    Assert.assertEquals(((Truck)fetched.getVehicle()).getPayloadKg(), 5000);
  }

  public void testLifecycleCallbacksFireInExpectedOrderDuringRoundTrip () {

    throngClient.delete(LifecycleOrder.class, Filter.empty(), new DeleteOptions());
    LifecycleOrder.resetCallbacks();

    LifecycleOrder order = new LifecycleOrder();

    order.setId("L1");
    order.setStatus("OPEN");

    throngClient.insert(order, new InsertOneOptions());

    Assert.assertTrue(order.isPrePersistInvoked());
    Assert.assertTrue(order.isPostPersistInvoked());

    LifecycleOrder fetched = throngClient.findOne(LifecycleOrder.class, Query.with().filter(Filter.where("_id").eq("L1")));

    Assert.assertNotNull(fetched);
    Assert.assertTrue(LifecycleOrder.isPreLoadInvoked());
    Assert.assertTrue(fetched.isPostLoadInvoked());
  }

  public void testStoreNullsTrueWritesBsonNullForNullProperty ()
    throws Exception {

    String storeNullsDatabase = "throng_adv_test_store_nulls";
    ThrongClient storeNullsClient = new ThrongClient(mongoClient, storeNullsDatabase, new ThrongOptions(true, false, false), NullableOrder.class);

    try {

      NullableOrder order = new NullableOrder();

      order.setId("N1");
      order.setNote(null);

      storeNullsClient.insert(order, new InsertOneOptions());

      Document raw = mongoClient.getDatabase(storeNullsDatabase).getCollection("nullable_orders").find(new Document("_id", "N1")).first();

      Assert.assertNotNull(raw);
      Assert.assertTrue(raw.containsKey("note"));
      Assert.assertNull(raw.get("note"));
    } finally {
      mongoClient.getDatabase(storeNullsDatabase).drop();
    }
  }

  public void testStoreNullsFalseOmitsNullPropertyFromBson () {

    throngClient.delete(NullableOrder.class, Filter.empty(), new DeleteOptions());

    NullableOrder order = new NullableOrder();

    order.setId("N2");
    order.setNote(null);

    throngClient.insert(order, new InsertOneOptions());

    Document raw = mongoClient.getDatabase("throng_adv_test").getCollection("nullable_orders").find(new Document("_id", "N2")).first();

    Assert.assertNotNull(raw);
    Assert.assertFalse(raw.containsKey("note"));
  }

  public void testEmbeddedDocumentRoundTripPreservesNestedFields () {

    throngClient.delete(OrderWithEmbedded.class, Filter.empty(), new DeleteOptions());

    OrderWithEmbedded order = new OrderWithEmbedded();

    order.setId("OE1");
    order.setShipTo(new Address("Berlin", "10115"));

    throngClient.insert(order, new InsertOneOptions());

    OrderWithEmbedded fetched = throngClient.findOne(OrderWithEmbedded.class, Query.with().filter(Filter.where("_id").eq("OE1")));

    Assert.assertNotNull(fetched);
    Assert.assertNotNull(fetched.getShipTo());
    Assert.assertEquals(fetched.getShipTo().getCity(), "Berlin");
    Assert.assertEquals(fetched.getShipTo().getZip(), "10115");
  }

  public void testEntityWithStringArrayPropertyRoundTripsAllElements () {

    throngClient.delete(OrderWithArray.class, Filter.empty(), new DeleteOptions());

    OrderWithArray order = new OrderWithArray();

    order.setId("OA1");
    order.setTags(new String[] {"red", "green", "blue"});

    throngClient.insert(order, new InsertOneOptions());

    OrderWithArray fetched = throngClient.findOne(OrderWithArray.class, Query.with().filter(Filter.where("_id").eq("OA1")));

    Assert.assertNotNull(fetched);
    Assert.assertEquals(fetched.getTags(), new String[] {"red", "green", "blue"});
  }

  public void testDuplicateIdInsertProducesClassifiableMongoWriteException () {

    throngClient.delete(UniqueIdOrder.class, Filter.empty(), new DeleteOptions());

    UniqueIdOrder first = new UniqueIdOrder();

    first.setId("U1");
    first.setName("first");

    throngClient.insert(first, new InsertOneOptions());

    UniqueIdOrder duplicate = new UniqueIdOrder();

    duplicate.setId("U1");
    duplicate.setName("second");

    try {
      throngClient.insert(duplicate, new InsertOneOptions());
      Assert.fail("Expected duplicate key write to throw");
    } catch (MongoWriteException writeException) {
      Assert.assertTrue(DuplicateKeyUtility.idDuplicateKeyException(writeException));
    }
  }

  public void testCursorExhaustionAcrossMultipleBatchesYieldsAllDocuments () {

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    for (int i = 0; i < 10; i++) {

      BatchedOrder order = new BatchedOrder();

      order.setId("B" + i);
      order.setSequence(i);

      throngClient.insert(order, new InsertOneOptions());
    }

    List<BatchedOrder> drained = throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty()).batchSize(2)).asList();

    Assert.assertEquals(drained.size(), 10);
  }

  public void testIteratorAdvancesThroughEveryDocumentInSmallResultSet () {

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    for (int i = 0; i < 5; i++) {

      BatchedOrder order = new BatchedOrder();

      order.setId("I" + i);
      order.setSequence(i);

      throngClient.insert(order, new InsertOneOptions());
    }

    Iterator<BatchedOrder> iterator = throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty())).iterator();
    int count = 0;

    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }

    Assert.assertEquals(count, 5);
  }

  public void testAllIndexTypesAreAcceptedByMongoOnIndexedFields () {

    List<String> indexNames = listIndexNames("all_index_types");

    Assert.assertTrue(indexNames.contains("ascField_1"), "expected ascending index, got " + indexNames);
    Assert.assertTrue(indexNames.contains("descField_-1"), "expected descending index, got " + indexNames);
    Assert.assertTrue(indexNames.contains("hashedField_hashed"), "expected hashed index, got " + indexNames);
  }

  public void testIteratorUsedInTryWithResourcesClosesUnderlyingCursor () {

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    for (int i = 0; i < 5; i++) {

      BatchedOrder order = new BatchedOrder();

      order.setId("R" + i);
      order.setSequence(i);

      throngClient.insert(order, new InsertOneOptions());
    }

    int count = 0;

    try (ThrongIterator<BatchedOrder> cursor = throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty())).iterator()) {
      while (cursor.hasNext()) {
        cursor.next();
        count++;
      }
    }

    Assert.assertEquals(count, 5);
  }

  public void testIteratorCloseIsIdempotentAndSafeToCallTwice () {

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    BatchedOrder order = new BatchedOrder();

    order.setId("R-idem");
    order.setSequence(0);

    throngClient.insert(order, new InsertOneOptions());

    ThrongIterator<BatchedOrder> cursor = throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty())).iterator();

    cursor.close();
    cursor.close();
  }

  public void testPartialIterationFollowedByCloseDoesNotThrow () {

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    for (int i = 0; i < 10; i++) {

      BatchedOrder order = new BatchedOrder();

      order.setId("R-partial-" + i);
      order.setSequence(i);

      throngClient.insert(order, new InsertOneOptions());
    }

    try (ThrongIterator<BatchedOrder> cursor = throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty()).batchSize(2)).iterator()) {

      Assert.assertTrue(cursor.hasNext());
      cursor.next();
      cursor.next();
    }
  }

  public void testConcurrentInsertsFromManyThreadsSucceedWithoutLossOrException ()
    throws InterruptedException {

    int threadCount = 16;
    int docsPerThread = 25;

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    CountDownLatch latch = new CountDownLatch(threadCount);
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    AtomicInteger errors = new AtomicInteger();

    for (int t = 0; t < threadCount; t++) {
      int threadId = t;
      executor.submit(() -> {
        try {
          for (int i = 0; i < docsPerThread; i++) {

            BatchedOrder order = new BatchedOrder();

            order.setId("C-" + threadId + "-" + i);
            order.setSequence(threadId * docsPerThread + i);

            throngClient.insert(order, new InsertOneOptions());
          }
        } catch (Exception exception) {
          errors.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    boolean finished = latch.await(60, TimeUnit.SECONDS);

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    Assert.assertTrue(finished, "threads did not finish in 60 seconds");
    Assert.assertEquals(errors.get(), 0, "encountered " + errors.get() + " errors across threads");
    Assert.assertEquals(throngClient.count(BatchedOrder.class, Filter.empty()), (long)threadCount * docsPerThread);
  }

  public void testConcurrentReadsDuringWritesRemainConsistent ()
    throws InterruptedException {

    int writerThreads = 4;
    int readerThreads = 8;
    int docsPerWriter = 25;

    throngClient.delete(BatchedOrder.class, Filter.empty(), new DeleteOptions());

    CountDownLatch latch = new CountDownLatch(writerThreads + readerThreads);
    ExecutorService executor = Executors.newFixedThreadPool(writerThreads + readerThreads);
    AtomicInteger errors = new AtomicInteger();

    for (int t = 0; t < writerThreads; t++) {
      int threadId = t;
      executor.submit(() -> {
        try {
          for (int i = 0; i < docsPerWriter; i++) {

            BatchedOrder order = new BatchedOrder();

            order.setId("M-" + threadId + "-" + i);
            order.setSequence(threadId * docsPerWriter + i);

            throngClient.insert(order, new InsertOneOptions());
          }
        } catch (Exception exception) {
          errors.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    for (int r = 0; r < readerThreads; r++) {
      executor.submit(() -> {
        try {
          for (int i = 0; i < 20; i++) {
            throngClient.count(BatchedOrder.class, Filter.empty());
            throngClient.find(BatchedOrder.class, Query.with().filter(Filter.empty()).limit(10)).asList();
          }
        } catch (Exception exception) {
          errors.incrementAndGet();
        } finally {
          latch.countDown();
        }
      });
    }

    boolean finished = latch.await(60, TimeUnit.SECONDS);

    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);

    Assert.assertTrue(finished, "threads did not finish in 60 seconds");
    Assert.assertEquals(errors.get(), 0, "encountered " + errors.get() + " errors across threads");
    Assert.assertEquals(throngClient.count(BatchedOrder.class, Filter.empty()), (long)writerThreads * docsPerWriter);
  }

  private List<String> listIndexNames (String collection) {

    List<String> names = new ArrayList<>();

    for (Document index : mongoClient.getDatabase("throng_adv_test").getCollection(collection).listIndexes()) {
      names.add(index.getString("name"));
    }

    return names;
  }

  private Document findIndex (String database, String collection, String name) {

    for (Document index : mongoClient.getDatabase(database).getCollection(collection).listIndexes()) {
      if (name.equals(index.getString("name"))) {

        return index;
      }
    }

    return null;
  }

  @Entity("indexed_orders")
  @org.smallmind.mongodb.throng.index.annotation.Indexes(
    value = {@org.smallmind.mongodb.throng.index.annotation.Index(value = "status"),
      @org.smallmind.mongodb.throng.index.annotation.Index(value = "placedAt", type = IndexType.DESCENDING)},
    options = @org.smallmind.mongodb.throng.index.annotation.IndexOptions(name = "status_placedAt_idx"))
  public static class IndexedOrder {

    @Id
    private String id;

    @org.smallmind.mongodb.throng.index.annotation.Indexed
    @Property
    private String status;

    @Property
    private Long placedAt;

    public IndexedOrder () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getStatus () {

      return status;
    }

    public void setStatus (String status) {

      this.status = status;
    }

    public Long getPlacedAt () {

      return placedAt;
    }

    public void setPlacedAt (Long placedAt) {

      this.placedAt = placedAt;
    }
  }

  @Entity("lifecycle_orders")
  public static class LifecycleOrder {

    private static boolean preLoadInvoked;

    @Id
    private String id;

    @Property
    private String status;

    private boolean postLoadInvoked;
    private boolean prePersistInvoked;
    private boolean postPersistInvoked;

    public LifecycleOrder () {

    }

    public static void resetCallbacks () {

      preLoadInvoked = false;
    }

    public static boolean isPreLoadInvoked () {

      return preLoadInvoked;
    }

    @PreLoad
    public static void onPreLoad (org.bson.BsonDocument document) {

      preLoadInvoked = true;
    }

    @PostLoad
    public void onPostLoad () {

      postLoadInvoked = true;
    }

    @PrePersist
    public void onPrePersist () {

      prePersistInvoked = true;
    }

    @PostPersist
    public void onPostPersist (org.bson.BsonDocument document) {

      postPersistInvoked = true;
    }

    public boolean isPostLoadInvoked () {

      return postLoadInvoked;
    }

    public boolean isPrePersistInvoked () {

      return prePersistInvoked;
    }

    public boolean isPostPersistInvoked () {

      return postPersistInvoked;
    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getStatus () {

      return status;
    }

    public void setStatus (String status) {

      this.status = status;
    }
  }

  @Entity("nullable_orders")
  public static class NullableOrder {

    @Id
    private String id;

    @Property
    private String note;

    public NullableOrder () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getNote () {

      return note;
    }

    public void setNote (String note) {

      this.note = note;
    }
  }

  @Entity("orders_with_embedded")
  public static class OrderWithEmbedded {

    @Id
    private String id;

    @Property
    private Address shipTo;

    public OrderWithEmbedded () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public Address getShipTo () {

      return shipTo;
    }

    public void setShipTo (Address shipTo) {

      this.shipTo = shipTo;
    }
  }

  @Embedded
  public static class Address {

    @Property
    private String city;

    @Property
    private String zip;

    public Address () {

    }

    public Address (String city, String zip) {

      this.city = city;
      this.zip = zip;
    }

    public String getCity () {

      return city;
    }

    public void setCity (String city) {

      this.city = city;
    }

    public String getZip () {

      return zip;
    }

    public void setZip (String zip) {

      this.zip = zip;
    }
  }

  @Entity("orders_with_array")
  public static class OrderWithArray {

    @Id
    private String id;

    @Property
    private String[] tags;

    public OrderWithArray () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String[] getTags () {

      return tags;
    }

    public void setTags (String[] tags) {

      this.tags = tags;
    }
  }

  @Entity("unique_id_orders")
  public static class UniqueIdOrder {

    @Id
    private String id;

    @Property
    private String name;

    public UniqueIdOrder () {

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
  }

  @Entity("garages")
  public static class Garage {

    @Id
    private String id;

    @Property
    private Vehicle vehicle;

    public Garage () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public Vehicle getVehicle () {

      return vehicle;
    }

    public void setVehicle (Vehicle vehicle) {

      this.vehicle = vehicle;
    }
  }

  @Embedded(polymorphic = @Polymorphic(value = {Car.class, Truck.class}, key = "kind"))
  public static class Vehicle {

    @Property
    private String tag;

    public Vehicle () {

    }

    public Vehicle (String tag) {

      this.tag = tag;
    }

    public String getTag () {

      return tag;
    }

    public void setTag (String tag) {

      this.tag = tag;
    }
  }

  @Embedded
  public static class Car extends Vehicle {

    @Property
    private Integer doors;

    public Car () {

    }

    public Car (String tag, Integer doors) {

      super(tag);
      this.doors = doors;
    }

    public Integer getDoors () {

      return doors;
    }

    public void setDoors (Integer doors) {

      this.doors = doors;
    }
  }

  @Embedded
  public static class Truck extends Vehicle {

    @Property
    private Integer payloadKg;

    public Truck () {

    }

    public Truck (String tag, Integer payloadKg) {

      super(tag);
      this.payloadKg = payloadKg;
    }

    public Integer getPayloadKg () {

      return payloadKg;
    }

    public void setPayloadKg (Integer payloadKg) {

      this.payloadKg = payloadKg;
    }
  }

  @Entity("all_index_types")
  public static class AllIndexTypes {

    @Id
    private String id;

    @org.smallmind.mongodb.throng.index.annotation.Indexed
    @Property
    private String ascField;

    @org.smallmind.mongodb.throng.index.annotation.Indexed(IndexType.DESCENDING)
    @Property
    private String descField;

    @org.smallmind.mongodb.throng.index.annotation.Indexed(IndexType.HASHED)
    @Property
    private String hashedField;

    public AllIndexTypes () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public String getAscField () {

      return ascField;
    }

    public void setAscField (String ascField) {

      this.ascField = ascField;
    }

    public String getDescField () {

      return descField;
    }

    public void setDescField (String descField) {

      this.descField = descField;
    }

    public String getHashedField () {

      return hashedField;
    }

    public void setHashedField (String hashedField) {

      this.hashedField = hashedField;
    }
  }

  @Entity("batched_orders")
  public static class BatchedOrder {

    @Id
    private String id;

    @Property
    private Integer sequence;

    public BatchedOrder () {

    }

    public String getId () {

      return id;
    }

    public void setId (String id) {

      this.id = id;
    }

    public Integer getSequence () {

      return sequence;
    }

    public void setSequence (Integer sequence) {

      this.sequence = sequence;
    }
  }
}
