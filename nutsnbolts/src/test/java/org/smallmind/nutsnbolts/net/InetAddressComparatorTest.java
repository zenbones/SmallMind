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
package org.smallmind.nutsnbolts.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class InetAddressComparatorTest {

  public void testIPv4ByteOrderBeatsLexicographic ()
    throws UnknownHostException {

    InetAddressComparator comparator = new InetAddressComparator();
    InetAddress lower = InetAddress.getByName("2.0.0.1");
    InetAddress higher = InetAddress.getByName("10.0.0.1");

    Assert.assertTrue(comparator.compare(lower, higher) < 0);
    Assert.assertTrue(comparator.compare(higher, lower) > 0);
  }

  public void testEqualAddressesCompareZero ()
    throws UnknownHostException {

    InetAddressComparator comparator = new InetAddressComparator();
    InetAddress address = InetAddress.getByName("192.168.1.1");

    Assert.assertEquals(comparator.compare(address, address), 0);
  }

  public void testSortOrderMatchesNumericByteOrdering ()
    throws UnknownHostException {

    InetAddressComparator comparator = new InetAddressComparator();
    InetAddress[] values = new InetAddress[] {
      InetAddress.getByName("172.16.0.1"),
      InetAddress.getByName("10.0.0.1"),
      InetAddress.getByName("192.168.0.1"),
      InetAddress.getByName("2.0.0.1")
    };

    Arrays.sort(values, comparator);

    Assert.assertEquals(values[0].getHostAddress(), "2.0.0.1");
    Assert.assertEquals(values[1].getHostAddress(), "10.0.0.1");
    Assert.assertEquals(values[2].getHostAddress(), "172.16.0.1");
    Assert.assertEquals(values[3].getHostAddress(), "192.168.0.1");
  }

  public void testIPv4OrderedBeforeMatchingIPv6 ()
    throws UnknownHostException {

    InetAddressComparator comparator = new InetAddressComparator();
    InetAddress fourByte = InetAddress.getByName("0.0.0.1");
    InetAddress sixteenByte = InetAddress.getByName("::1");

    Assert.assertEquals(comparator.compare(fourByte, sixteenByte), 0);
  }

  public void testHighByteIPv6BeatsLowByteIPv4 ()
    throws UnknownHostException {

    InetAddressComparator comparator = new InetAddressComparator();
    InetAddress small = InetAddress.getByName("192.168.0.1");
    InetAddress huge = InetAddress.getByName("2001::1");

    Assert.assertTrue(comparator.compare(small, huge) < 0);
  }
}
