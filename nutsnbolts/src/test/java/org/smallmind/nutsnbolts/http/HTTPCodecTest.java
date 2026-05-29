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
package org.smallmind.nutsnbolts.http;

import java.io.UnsupportedEncodingException;
import org.smallmind.nutsnbolts.util.Tuple;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class HTTPCodecTest {

  public void testEncodeJoinsPairsWithAmpersand ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("a", "1");
    tuple.addPair("b", "2");

    Assert.assertEquals(HTTPCodec.urlEncode(tuple), "a=1&b=2");
  }

  public void testEncodeAppliesUrlEncodingToKeysAndValues ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("key one", "val/two");

    Assert.assertEquals(HTTPCodec.urlEncode(tuple), "key+one=val%2Ftwo");
  }

  public void testIgnoredKeysBypassEncodingOnBothKeyAndValue ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("safe", "raw value/with slash");
    tuple.addPair("encoded", "needs encoding");

    String result = HTTPCodec.urlEncode(tuple, "safe");

    Assert.assertTrue(result.contains("safe=raw value/with slash"));
    Assert.assertTrue(result.contains("encoded=needs+encoding"));
  }

  public void testDecodeReturnsKeyValueTupleInOrder ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = HTTPCodec.urlDecode("a=1&b=2&c=3");

    Assert.assertEquals(tuple.size(), 3);
    Assert.assertEquals(tuple.getKey(0), "a");
    Assert.assertEquals(tuple.getValue(0), "1");
    Assert.assertEquals(tuple.getKey(2), "c");
    Assert.assertEquals(tuple.getValue(2), "3");
  }

  public void testDecodeUnescapesKeysAndValues ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = HTTPCodec.urlDecode("key+one=val%2Ftwo");

    Assert.assertEquals(tuple.getKey(0), "key one");
    Assert.assertEquals(tuple.getValue(0), "val/two");
  }

  public void testEncodeThenDecodeRoundTripsAllPairs ()
    throws UnsupportedEncodingException {

    Tuple<String, String> tuple = new Tuple<>();

    tuple.addPair("name", "Caf\u00E9");
    tuple.addPair("path", "/usr/local/bin");

    Tuple<String, String> roundTrip = HTTPCodec.urlDecode(HTTPCodec.urlEncode(tuple));

    Assert.assertEquals(roundTrip.size(), 2);
    Assert.assertEquals(roundTrip.getValue("name"), "Caf\u00E9");
    Assert.assertEquals(roundTrip.getValue("path"), "/usr/local/bin");
  }

  @Test(expectedExceptions = UnsupportedEncodingException.class)
  public void testDecodeMissingEqualsIsRejected ()
    throws UnsupportedEncodingException {

    HTTPCodec.urlDecode("noequals");
  }
}
