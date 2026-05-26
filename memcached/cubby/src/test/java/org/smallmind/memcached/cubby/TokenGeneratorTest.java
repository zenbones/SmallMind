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
package org.smallmind.memcached.cubby;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class TokenGeneratorTest {

  public void testFirstTokenIsNonEmptyPrintableAscii () {

    TokenGenerator generator = new TokenGenerator();
    String first = generator.next();

    Assert.assertNotNull(first);
    Assert.assertFalse(first.isEmpty());
    for (int index = 0; index < first.length(); index++) {

      char ch = first.charAt(index);

      Assert.assertTrue(ch > 0x20 && ch < 0x7F, "Token char outside printable ASCII: 0x" + Integer.toHexString(ch));
    }
  }

  public void testSuccessiveTokensAreDistinctAcrossAlphabetWrap () {

    TokenGenerator generator = new TokenGenerator();
    HashSet<String> seen = new HashSet<>();

    for (int loop = 0; loop < 2_000; loop++) {

      String token = generator.next();

      Assert.assertTrue(seen.add(token), "Duplicate token after " + seen.size() + " calls: " + token);
    }
  }

  public void testTokensGrowToTwoCharactersAfterFirstDigitWraps () {

    TokenGenerator generator = new TokenGenerator();

    for (int loop = 0; loop < 90; loop++) {

      String token = generator.next();

      Assert.assertEquals(token.length(), 1, "Single-character tokens expected for first 90 calls (call " + loop + ")");
    }

    String afterWrap = generator.next();

    Assert.assertEquals(afterWrap.length(), 2);
  }

  public void testConcurrentCallersReceiveDistinctTokens ()
    throws Exception {

    final TokenGenerator generator = new TokenGenerator();
    final int threadCount = 8;
    final int perThread = 500;
    final Set<String> collected = Collections.synchronizedSet(new HashSet<>());
    final CountDownLatch ready = new CountDownLatch(threadCount);
    final CountDownLatch go = new CountDownLatch(1);
    final CountDownLatch done = new CountDownLatch(threadCount);
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);

    try {
      for (int worker = 0; worker < threadCount; worker++) {
        pool.submit(() -> {
          ready.countDown();
          try {
            go.await();
            for (int loop = 0; loop < perThread; loop++) {
              collected.add(generator.next());
            }
          } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
          } finally {
            done.countDown();
          }
        });
      }

      ready.await(5, TimeUnit.SECONDS);
      go.countDown();
      Assert.assertTrue(done.await(10, TimeUnit.SECONDS), "Workers did not complete in time");
    } finally {
      pool.shutdownNow();
    }

    Assert.assertEquals(collected.size(), threadCount * perThread, "Duplicates seen under concurrent access");
  }
}
