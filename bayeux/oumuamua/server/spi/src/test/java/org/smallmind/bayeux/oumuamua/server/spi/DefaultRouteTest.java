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
package org.smallmind.bayeux.oumuamua.server.spi;

import org.smallmind.bayeux.oumuamua.server.api.InvalidPathException;
import org.smallmind.bayeux.oumuamua.server.api.Segment;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DefaultRouteTest {

  public void testPathAndSize ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertEquals(route.getPath(), "/foo/bar/baz");
    Assert.assertEquals(route.size(), 3);
    Assert.assertEquals(route.lastIndex(), 2);
  }

  public void testSingleSegmentRoute ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo");

    Assert.assertEquals(route.size(), 1);
    Assert.assertEquals(route.lastIndex(), 0);
  }

  public void testGetSegmentText ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertEquals(route.getSegment(0).toString(), "foo");
    Assert.assertEquals(route.getSegment(1).toString(), "bar");
    Assert.assertEquals(route.getSegment(2).toString(), "baz");
  }

  public void testGetSegmentLengthAndCharAt ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/quux");
    Segment segment = route.getSegment(1);

    Assert.assertEquals(segment.length(), 4);
    Assert.assertEquals(segment.charAt(0), 'q');
    Assert.assertEquals(segment.charAt(3), 'x');
  }

  public void testGetSegmentMatchesPrefix ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar");

    Assert.assertTrue(route.getSegment(0).matches("foo"));
    Assert.assertFalse(route.getSegment(0).matches("bar"));
    Assert.assertFalse(route.getSegment(0).matches("fo"));
    Assert.assertFalse(route.getSegment(0).matches(null));
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetSegmentNegativeIndex ()
    throws InvalidPathException {

    new DefaultRoute("/foo/bar").getSegment(-1);
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void testGetSegmentBeyondLast ()
    throws InvalidPathException {

    new DefaultRoute("/foo/bar").getSegment(2);
  }

  public void testIsWild ()
    throws InvalidPathException {

    Assert.assertTrue(new DefaultRoute("/foo/*").isWild());
    Assert.assertFalse(new DefaultRoute("/foo/bar").isWild());
    Assert.assertFalse(new DefaultRoute("/foo/**").isWild());
  }

  public void testIsDeepWild ()
    throws InvalidPathException {

    Assert.assertTrue(new DefaultRoute("/foo/**").isDeepWild());
    Assert.assertFalse(new DefaultRoute("/foo/*").isDeepWild());
    Assert.assertFalse(new DefaultRoute("/foo/bar").isDeepWild());
  }

  public void testIsMeta ()
    throws InvalidPathException {

    Assert.assertTrue(new DefaultRoute("/meta/handshake").isMeta());
    Assert.assertFalse(new DefaultRoute("/service/echo").isMeta());
    Assert.assertFalse(new DefaultRoute("/metadata/info").isMeta());
  }

  public void testIsService ()
    throws InvalidPathException {

    Assert.assertTrue(new DefaultRoute("/service/echo").isService());
    Assert.assertFalse(new DefaultRoute("/meta/handshake").isService());
  }

  public void testIsDeliverable ()
    throws InvalidPathException {

    Assert.assertTrue(new DefaultRoute("/foo/bar").isDeliverable());
    Assert.assertFalse(new DefaultRoute("/meta/handshake").isDeliverable());
    Assert.assertFalse(new DefaultRoute("/service/echo").isDeliverable());
    Assert.assertFalse(new DefaultRoute("/foo/*").isDeliverable());
    Assert.assertFalse(new DefaultRoute("/foo/**").isDeliverable());
  }

  public void testMatchesPrefixExact ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertTrue(route.matchesPrefix("foo", "bar", "baz"));
    Assert.assertFalse(route.matchesPrefix("foo", "bar", "qux"));
    Assert.assertFalse(route.matchesPrefix("foo", "qux", "baz"));
  }

  public void testMatchesPrefixPrefixAccepted ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertTrue(route.matchesPrefix("foo", "bar"));
    Assert.assertTrue(route.matchesPrefix("foo"));
  }

  public void testMatchesPrefixSingleWildcard ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertTrue(route.matchesPrefix("foo", "*", "baz"));
    Assert.assertTrue(route.matchesPrefix("*", "bar", "baz"));
  }

  public void testMatchesPrefixDeepWildcard ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar/baz");

    Assert.assertTrue(route.matchesPrefix("foo", "**"));
    Assert.assertTrue(route.matchesPrefix("**"));
  }

  public void testMatchesPrefixTooManySegments ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar");

    Assert.assertFalse(route.matchesPrefix("foo", "bar", "baz"));
  }

  public void testMatchesPrefixNull ()
    throws InvalidPathException {

    Assert.assertFalse(new DefaultRoute("/foo").matchesPrefix((String[])null));
  }

  public void testEqualsAndHashCode ()
    throws InvalidPathException {

    DefaultRoute a = new DefaultRoute("/foo/bar");
    DefaultRoute b = new DefaultRoute("/foo/bar");
    DefaultRoute c = new DefaultRoute("/foo/baz");

    Assert.assertEquals(a, b);
    Assert.assertEquals(a.hashCode(), b.hashCode());
    Assert.assertNotEquals(a, c);
    Assert.assertNotEquals(a, "/foo/bar");
  }

  public void testPredefinedMetaRoutes () {

    Assert.assertTrue(DefaultRoute.HANDSHAKE_ROUTE.isMeta());
    Assert.assertTrue(DefaultRoute.CONNECT_ROUTE.isMeta());
    Assert.assertTrue(DefaultRoute.DISCONNECT_ROUTE.isMeta());
    Assert.assertTrue(DefaultRoute.SUBSCRIBE_ROUTE.isMeta());
    Assert.assertTrue(DefaultRoute.UNSUBSCRIBE_ROUTE.isMeta());

    Assert.assertEquals(DefaultRoute.HANDSHAKE_ROUTE.getPath(), "/meta/handshake");
    Assert.assertEquals(DefaultRoute.CONNECT_ROUTE.getPath(), "/meta/connect");
    Assert.assertEquals(DefaultRoute.DISCONNECT_ROUTE.getPath(), "/meta/disconnect");
    Assert.assertEquals(DefaultRoute.SUBSCRIBE_ROUTE.getPath(), "/meta/subscribe");
    Assert.assertEquals(DefaultRoute.UNSUBSCRIBE_ROUTE.getPath(), "/meta/unsubscribe");
  }

  @Test(expectedExceptions = InvalidPathException.class)
  public void testInvalidPathThrows ()
    throws InvalidPathException {

    new DefaultRoute("no-leading-slash");
  }

  public void testSegmentHashCodeSameForEqualText ()
    throws InvalidPathException {

    Segment a = new DefaultRoute("/foo/bar").getSegment(0);
    Segment b = new DefaultRoute("/foo/bar").getSegment(0);

    Assert.assertEquals(a.hashCode(), b.hashCode());
  }

  public void testSegmentHashCodeDiffersForDifferentText ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar");
    Segment a = route.getSegment(0);
    Segment b = route.getSegment(1);

    Assert.assertNotEquals(a.hashCode(), b.hashCode());
  }

  public void testSegmentEqualsMatchingText ()
    throws InvalidPathException {

    Segment a = new DefaultRoute("/foo/bar").getSegment(0);
    Segment b = new DefaultRoute("/foo/bar").getSegment(0);

    Assert.assertEquals(a, b);
  }

  public void testSegmentEqualsNonMatchingText ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar");
    Segment a = route.getSegment(0);
    Segment b = route.getSegment(1);

    Assert.assertNotEquals(a, b);
  }

  public void testSegmentEqualsNonSegment ()
    throws InvalidPathException {

    Segment a = new DefaultRoute("/foo/bar").getSegment(0);

    Assert.assertNotEquals(a, "foo");
  }

  public void testSegmentSubSequence ()
    throws InvalidPathException {

    Segment segment = new DefaultRoute("/foobar").getSegment(0);

    Assert.assertEquals(segment.subSequence(1, 4).toString(), "oob");
  }

  public void testEqualsRejectsNullAndNonRouteObjects ()
    throws InvalidPathException {

    DefaultRoute route = new DefaultRoute("/foo/bar");

    Assert.assertFalse(route.equals(null), "equals(null) must return false");
    Assert.assertFalse(route.equals(123), "equals against an unrelated type must return false");
  }

  public void testSegmentSubSequenceOnNonFirstSegment ()
    throws InvalidPathException {

    Segment segment = new DefaultRoute("/foo/barbaz").getSegment(1);

    Assert.assertEquals(segment.subSequence(0, 3).toString(), "bar");
    Assert.assertEquals(segment.subSequence(3, 5).toString(), "ba");
  }

  public void testSegmentCharAtOnLastSegment ()
    throws InvalidPathException {

    Segment segment = new DefaultRoute("/foo/quux").getSegment(1);

    Assert.assertEquals(segment.length(), 4);
    Assert.assertEquals(segment.charAt(0), 'q');
    Assert.assertEquals(segment.charAt(3), 'x');
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testSegmentCharAtNegativePosThrows ()
    throws InvalidPathException {

    new DefaultRoute("/foo/bar").getSegment(0).charAt(-1);
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testSegmentCharAtBeyondLengthThrows ()
    throws InvalidPathException {

    new DefaultRoute("/foo").getSegment(0).charAt(99);
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testSegmentSubSequenceNegativeStartThrows ()
    throws InvalidPathException {

    new DefaultRoute("/foo/bar").getSegment(0).subSequence(-1, 2);
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testSegmentSubSequenceEndBeyondLengthThrows ()
    throws InvalidPathException {

    new DefaultRoute("/foo/bar").getSegment(0).subSequence(0, 99);
  }
}
