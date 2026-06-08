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
package org.smallmind.persistence.sql;

import java.sql.PreparedStatement;
import javax.sql.PooledConnection;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PooledPreparedStatementCacheTest {

  private static int statementCounter = 0;

  /**
   * Builds a Mockito-mocked {@link PooledPreparedStatement} with a unique statement id and its own
   * mocked {@link PreparedStatement} handle, so the cache can index and hand it back.
   */
  private static PooledPreparedStatement mockPooledStatement () {

    PooledPreparedStatement pooledStatement = Mockito.mock(PooledPreparedStatement.class);
    PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);

    Mockito.when(pooledStatement.getStatementId()).thenReturn("stmt-" + (statementCounter++));
    Mockito.when(pooledStatement.getPreparedStatement()).thenReturn(preparedStatement);

    return pooledStatement;
  }

  /**
   * Constructs a real {@link PooledPreparedStatementEvent} (the cache down-casts the JDBC
   * {@link javax.sql.StatementEvent} to this type to read its statement id) bound to the supplied
   * pooled statement's id.
   */
  private static PooledPreparedStatementEvent eventFor (PooledPreparedStatement pooledStatement) {

    return new PooledPreparedStatementEvent(Mockito.mock(PooledConnection.class), Mockito.mock(PreparedStatement.class), pooledStatement.getStatementId());
  }

  public void testCacheThenRetrieveFreeStatementForSameArgs ()
    throws InterruptedException {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement pooledStatement = mockPooledStatement();
    Object[] args = new Object[] {"select 1", 1, 2};

    PreparedStatement cached = cache.cachePreparedStatement(args, pooledStatement);

    Assert.assertSame(cached, pooledStatement.getPreparedStatement());

    // The freshly cached statement is in-use, so an immediate lookup finds nothing free.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select 1", 1, 2}));

    // After it is closed (returned), the same args retrieve the same free statement.
    cache.statementClosed(eventFor(pooledStatement));

    Assert.assertSame(cache.getPreparedStatement(new Object[] {"select 1", 1, 2}), pooledStatement.getPreparedStatement());
  }

  public void testInUseEntryIsSkipped () {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement pooledStatement = mockPooledStatement();
    Object[] args = new Object[] {"select 1"};

    // Caching marks the wrapper in-use; a concurrent request for the same key must not hand it back.
    cache.cachePreparedStatement(args, pooledStatement);

    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select 1"}));
  }

  public void testFreeingViaStatementClosedMakesEntryAvailableAgain () {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement pooledStatement = mockPooledStatement();
    Object[] args = new Object[] {"select 1"};

    cache.cachePreparedStatement(args, pooledStatement);
    cache.statementClosed(eventFor(pooledStatement));

    PreparedStatement first = cache.getPreparedStatement(new Object[] {"select 1"});

    Assert.assertSame(first, pooledStatement.getPreparedStatement());
    // Acquired again -> back in-use -> no longer free.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select 1"}));
  }

  public void testLruEvictionRemovesOldestEntryWhenFull ()
    throws InterruptedException {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(2);

    PooledPreparedStatement oldest = mockPooledStatement();
    PooledPreparedStatement middle = mockPooledStatement();
    PooledPreparedStatement newest = mockPooledStatement();

    // Distinct timestamps are required because TimeKey orders purely by last-access time.
    cache.cachePreparedStatement(new Object[] {"sql-a"}, oldest);
    Thread.sleep(5);
    cache.cachePreparedStatement(new Object[] {"sql-b"}, middle);
    Thread.sleep(5);
    // Reaching maxStatements (2) evicts the least-recently-used entry (oldest, "sql-a").
    cache.cachePreparedStatement(new Object[] {"sql-c"}, newest);

    cache.statementClosed(eventFor(oldest));
    cache.statementClosed(eventFor(middle));
    cache.statementClosed(eventFor(newest));

    // The evicted entry is no longer retrievable.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"sql-a"}));
    // The two most-recently-used entries survived.
    Assert.assertSame(cache.getPreparedStatement(new Object[] {"sql-b"}), middle.getPreparedStatement());
    Assert.assertSame(cache.getPreparedStatement(new Object[] {"sql-c"}), newest.getPreparedStatement());
  }

  public void testAcquireBumpsRecencySoBumpedEntrySurvivesEviction ()
    throws InterruptedException {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(2);

    PooledPreparedStatement first = mockPooledStatement();
    PooledPreparedStatement second = mockPooledStatement();
    PooledPreparedStatement third = mockPooledStatement();

    cache.cachePreparedStatement(new Object[] {"sql-a"}, first);
    Thread.sleep(5);
    cache.cachePreparedStatement(new Object[] {"sql-b"}, second);

    // Free and re-acquire "sql-a": acquire() re-inserts an updated TimeKey, bumping its recency
    // ahead of "sql-b".
    cache.statementClosed(eventFor(first));
    Thread.sleep(5);

    Assert.assertSame(cache.getPreparedStatement(new Object[] {"sql-a"}), first.getPreparedStatement());

    Thread.sleep(5);
    // Now caching a third entry should evict the least-recently-used, which is now "sql-b".
    cache.cachePreparedStatement(new Object[] {"sql-c"}, third);

    cache.statementClosed(eventFor(first));
    cache.statementClosed(eventFor(third));

    Assert.assertNull(cache.getPreparedStatement(new Object[] {"sql-b"}));
    Assert.assertSame(cache.getPreparedStatement(new Object[] {"sql-a"}), first.getPreparedStatement());
    Assert.assertSame(cache.getPreparedStatement(new Object[] {"sql-c"}), third.getPreparedStatement());
  }

  public void testStatementErrorOccurredRemovesEntryAndClosesStatement ()
    throws Exception {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement pooledStatement = mockPooledStatement();
    Object[] args = new Object[] {"select 1"};

    cache.cachePreparedStatement(args, pooledStatement);
    cache.statementClosed(eventFor(pooledStatement));

    cache.statementErrorOccurred(eventFor(pooledStatement));

    // The underlying pooled statement is permanently closed on error.
    Mockito.verify(pooledStatement).close();
    // And the entry (plus its argument/time indices) is gone, so nothing is retrievable.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select 1"}));
  }

  public void testStatementErrorOccurredForUnknownIdIsHarmless () {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement known = mockPooledStatement();
    PooledPreparedStatement unknown = mockPooledStatement();

    cache.cachePreparedStatement(new Object[] {"select 1"}, known);

    // An error event for an id that was never cached must be a no-op (guarded null check).
    cache.statementErrorOccurred(eventFor(unknown));

    cache.statementClosed(eventFor(known));
    Assert.assertSame(cache.getPreparedStatement(new Object[] {"select 1"}), known.getPreparedStatement());
  }

  public void testSameArgValuesShareCacheKeyDifferingArgsDoNot () {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement pooledStatement = mockPooledStatement();

    // Cached under a value-equal-but-distinct array; deep array equality on ArgumentKey means a
    // lookup with an equal-valued array still finds it.
    cache.cachePreparedStatement(new Object[] {"select ?", "x", 42}, pooledStatement);
    cache.statementClosed(eventFor(pooledStatement));

    Assert.assertSame(cache.getPreparedStatement(new Object[] {"select ?", "x", 42}), pooledStatement.getPreparedStatement());

    // Re-acquired -> in-use again.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select ?", "x", 42}));
    // A differing argument signature does not collide with the cached entry.
    Assert.assertNull(cache.getPreparedStatement(new Object[] {"select ?", "y", 42}));
  }

  public void testCloseClosesAllCachedStatements ()
    throws Exception {

    PooledPreparedStatementCache cache = new PooledPreparedStatementCache(8);
    PooledPreparedStatement one = mockPooledStatement();
    PooledPreparedStatement two = mockPooledStatement();

    cache.cachePreparedStatement(new Object[] {"sql-a"}, one);
    cache.cachePreparedStatement(new Object[] {"sql-b"}, two);

    cache.close();

    Mockito.verify(one).close();
    Mockito.verify(two).close();
  }
}
