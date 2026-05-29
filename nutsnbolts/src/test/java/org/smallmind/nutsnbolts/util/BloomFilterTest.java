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
package org.smallmind.nutsnbolts.util;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BloomFilterTest {

  public void testAddedElementIsReportedAsPresent ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(0.01d, 1000);

    filter.add("hello".getBytes(StandardCharsets.UTF_8));

    Assert.assertTrue(filter.contains("hello".getBytes(StandardCharsets.UTF_8)));
  }

  public void testAbsentElementIsDefinitelyReportedMissingWhenNothingAdded ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(0.001d, 1000);

    Assert.assertFalse(filter.contains("absent".getBytes(StandardCharsets.UTF_8)));
  }

  public void testSizeReflectsNumberOfAdds ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(0.01d, 1000);

    filter.add("a".getBytes(StandardCharsets.UTF_8));
    filter.add("b".getBytes(StandardCharsets.UTF_8));
    filter.add("c".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals(filter.size(), 3);
  }

  public void testClearResetsSizeAndMembership ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(0.01d, 1000);

    filter.add("a".getBytes(StandardCharsets.UTF_8));
    filter.add("b".getBytes(StandardCharsets.UTF_8));
    filter.clear();

    Assert.assertEquals(filter.size(), 0);
    Assert.assertFalse(filter.contains("a".getBytes(StandardCharsets.UTF_8)));
    Assert.assertFalse(filter.contains("b".getBytes(StandardCharsets.UTF_8)));
  }

  public void testMostNonMembersAreCorrectlyRejected ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(0.001d, 1000);
    int falsePositives = 0;

    for (int i = 0; i < 500; i++) {
      filter.add(("member-" + i).getBytes(StandardCharsets.UTF_8));
    }

    for (int i = 0; i < 500; i++) {
      if (filter.contains(("nonmember-" + i).getBytes(StandardCharsets.UTF_8))) {
        falsePositives++;
      }
    }

    Assert.assertTrue(falsePositives < 25, "Too many false positives: " + falsePositives);
  }

  public void testConfiguredParametersAreExposed ()
    throws NoSuchAlgorithmException {

    BloomFilter<BloomFilterElement> filter = new BloomFilter<>(8.0d, 100, 5);

    Assert.assertEquals(filter.getMaxElements(), 100);
    Assert.assertEquals(filter.getHashCount(), 5);
    Assert.assertEquals(filter.getBitsPerElement(), 8.0d);
    Assert.assertEquals(filter.length(), 800);
  }
}
