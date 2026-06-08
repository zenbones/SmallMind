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
package org.smallmind.persistence.database.mysql;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import org.smallmind.persistence.database.SequenceManager;
import org.smallmind.persistence.sql.DriverManagerDataSource;
import org.smallmind.persistence.sql.testbench.DataSourceAvailableTestCondition;
import org.smallmind.testbench.condition.TestConditions;
import org.smallmind.testbench.docker.DockerApplication;
import org.smallmind.testbench.groundwater.AbstractGroundwaterTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Integration test for {@link SimulatedSequence} against a real MySQL container started through the
 * docker testbench. The class relies on MySQL-specific {@code LAST_INSERT_ID(expr)} and {@code INSERT IGNORE}
 * semantics that cannot be reproduced by an in-memory database, so it exercises the block-allocation algorithm
 * end to end: monotonicity, per-name isolation, contiguity across block refreshes, and uniqueness under
 * concurrent draws.
 *
 * <p>Requires a running Docker daemon; the {@code mysql:latest} image is pulled on first run.
 */
@Test(groups = "integration")
public class SimulatedSequenceIntegrationTest extends AbstractGroundwaterTest {

  private static final String DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
  private static final String JDBC_URL = "jdbc:mysql://localhost:3306/groundwater?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&sslMode=DISABLED";
  private static final String USER_NAME = "root";
  private static final String PASSWORD = "secret";
  private static final String SEQUENCE_TABLE = "sequence_value";

  private DriverManagerDataSource dataSource;

  public SimulatedSequenceIntegrationTest () {

    super(DockerApplication.MYSQL);
  }

  @BeforeClass
  @Override
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    TestConditions.serial(120, new DataSourceAvailableTestCondition(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD));

    dataSource = new DriverManagerDataSource(DRIVER_CLASS_NAME, JDBC_URL, USER_NAME, PASSWORD);

    try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + SEQUENCE_TABLE + " (name VARCHAR(64) NOT NULL PRIMARY KEY, next_val BIGINT NOT NULL)");
    }
  }

  @AfterClass
  @Override
  public void afterClass ()
    throws Exception {

    super.afterClass();
  }

  private SimulatedSequence sequence (int incrementBy) {

    return new SimulatedSequence(dataSource, SEQUENCE_TABLE, incrementBy);
  }

  public void testValuesAreStrictlyIncreasingAndContiguousForUnitIncrement () {

    SimulatedSequence simulatedSequence = sequence(1);
    long previous = simulatedSequence.nextLong("unit");

    Assert.assertTrue(previous > 0);

    for (int index = 0; index < 20; index++) {

      long current = simulatedSequence.nextLong("unit");

      Assert.assertEquals(current, previous + 1);
      previous = current;
    }
  }

  public void testCountersAreIsolatedByName () {

    SimulatedSequence simulatedSequence = sequence(1);

    long firstOrders = simulatedSequence.nextLong("orders");
    long firstInvoices = simulatedSequence.nextLong("invoices");

    Assert.assertEquals(simulatedSequence.nextLong("orders"), firstOrders + 1);
    Assert.assertEquals(simulatedSequence.nextLong("invoices"), firstInvoices + 1);
    Assert.assertEquals(simulatedSequence.nextLong("orders"), firstOrders + 2);
  }

  public void testBlockAllocationYieldsContiguousValuesAcrossRefreshBoundaries () {

    SimulatedSequence simulatedSequence = sequence(10);
    long previous = simulatedSequence.nextLong("blocked");

    Assert.assertTrue(previous > 0);

    // Draw well past a single block so the offset-exhaustion refresh path is crossed more than once.
    for (int index = 0; index < 25; index++) {

      long current = simulatedSequence.nextLong("blocked");

      Assert.assertEquals(current, previous + 1, "block allocation skipped or repeated a value at a refresh boundary");
      previous = current;
    }
  }

  public void testConcurrentAllocationProducesNoDuplicates ()
    throws InterruptedException {

    final SimulatedSequence simulatedSequence = sequence(50);
    final Set<Long> drawn = ConcurrentHashMap.newKeySet();
    final int threadCount = 8;
    final int drawsPerThread = 200;
    final CountDownLatch ready = new CountDownLatch(threadCount);
    final CountDownLatch go = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(threadCount);
    final Set<Long> collisions = ConcurrentHashMap.newKeySet();

    for (int thread = 0; thread < threadCount; thread++) {
      new Thread(() -> {

        ready.countDown();
        try {
          go.await();
          for (int index = 0; index < drawsPerThread; index++) {

            long value = simulatedSequence.nextLong("concurrent");

            if (!drawn.add(value)) {
              collisions.add(value);
            }
          }
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      }).start();
    }

    ready.await();
    go.countDown();
    done.await();

    Assert.assertEquals(collisions.size(), 0, "concurrent draws handed out duplicate values: " + collisions);
    Assert.assertEquals(drawn.size(), threadCount * drawsPerThread);
  }

  public void testRegisteredSequenceIsReachableThroughSequenceManager () {

    sequence(1).register();

    long first = SequenceManager.nextLong("managed");

    Assert.assertTrue(first > 0);
    Assert.assertEquals(SequenceManager.nextLong("managed"), first + 1);
  }

  public void testLongRunCrossesManyBlockBoundariesWithoutGapsOrCollisions () {

    SimulatedSequence simulatedSequence = sequence(10);
    Set<Long> drawn = new HashSet<>();
    long previous = simulatedSequence.nextLong("longrun");

    Assert.assertTrue(previous > 0);
    Assert.assertTrue(drawn.add(previous));

    // Draw far past a single block so the offset-exhaustion refresh path is crossed many times over.
    for (int index = 0; index < 250; index++) {

      long current = simulatedSequence.nextLong("longrun");

      Assert.assertEquals(current, previous + 1, "block allocation skipped or repeated a value at a refresh boundary");
      Assert.assertTrue(drawn.add(current), "block allocation handed out a duplicate value: " + current);
      previous = current;
    }

    Assert.assertEquals(drawn.size(), 251);
  }

  public void testFreshSequenceIsCreatedAndResumedByASecondInstance () {

    SimulatedSequence firstInstance = sequence(1);

    // A brand new name forces the insert-and-prime constructor path with no pre-existing row.
    long first = firstInstance.nextLong("fresh");

    Assert.assertTrue(first > 0);
    Assert.assertEquals(firstInstance.nextLong("fresh"), first + 1);
    Assert.assertEquals(firstInstance.nextLong("fresh"), first + 2);

    // A second instance carries its own SequenceData, which must re-prime from the persisted boundary
    // rather than restarting the counter, so it never reissues a value the first instance already handed out.
    SimulatedSequence secondInstance = sequence(1);
    long resumed = secondInstance.nextLong("fresh");

    Assert.assertTrue(resumed > first + 2, "a second instance reissued an already-allocated value");
    Assert.assertEquals(secondInstance.nextLong("fresh"), resumed + 1);
  }

  public void testConcurrentAllocationProducesNoDuplicatesForUnitIncrement ()
    throws InterruptedException {

    final SimulatedSequence simulatedSequence = sequence(1);
    final Set<Long> drawn = ConcurrentHashMap.newKeySet();
    final int threadCount = 8;
    final int drawsPerThread = 100;
    final CountDownLatch ready = new CountDownLatch(threadCount);
    final CountDownLatch go = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(threadCount);
    final Set<Long> collisions = ConcurrentHashMap.newKeySet();

    for (int thread = 0; thread < threadCount; thread++) {
      new Thread(() -> {

        ready.countDown();
        try {
          go.await();
          for (int index = 0; index < drawsPerThread; index++) {

            long value = simulatedSequence.nextLong("concurrentUnit");

            if (!drawn.add(value)) {
              collisions.add(value);
            }
          }
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      }).start();
    }

    ready.await();
    go.countDown();
    done.await();

    Assert.assertEquals(collisions.size(), 0, "concurrent unit-increment draws handed out duplicate values: " + collisions);
    Assert.assertEquals(drawn.size(), threadCount * drawsPerThread);
  }
}
