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
package org.smallmind.bayeux.oumuamua.server.api;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SegmentTest {

  private static LiteralSegment segment (String text) {

    return new LiteralSegment(text);
  }

  public void testHashCodeSameTextProducesSameHash () {

    Assert.assertEquals(segment("foo").hashCode(), segment("foo").hashCode());
  }

  public void testHashCodeDifferentTextProducesDifferentHash () {

    Assert.assertNotEquals(segment("foo").hashCode(), segment("bar").hashCode());
  }

  public void testHashCodeEmptyStringIsZero () {

    Assert.assertEquals(segment("").hashCode(), 0);
  }

  public void testEqualsMatchingSegmentReturnsTrue () {

    Assert.assertEquals(segment("foo"), segment("foo"));
  }

  public void testEqualsDifferentTextReturnsFalse () {

    Assert.assertNotEquals(segment("foo"), segment("bar"));
  }

  public void testEqualsNonSegmentReturnsFalse () {

    Assert.assertFalse(segment("foo").equals("foo"));
  }

  public void testEqualsNullReturnsFalse () {

    Assert.assertFalse(segment("foo").equals(null));
  }

  private static class LiteralSegment extends Segment {

    private final String text;

    private LiteralSegment (String text) {

      this.text = text;
    }

    @Override
    public boolean matches (CharSequence charSequence) {

      if (charSequence.length() != text.length()) {
        return false;
      }

      for (int pos = 0; pos < text.length(); pos++) {
        if (charSequence.charAt(pos) != text.charAt(pos)) {
          return false;
        }
      }

      return true;
    }

    @Override
    public String toString () {

      return text;
    }

    @Override
    public int length () {

      return text.length();
    }

    @Override
    public char charAt (int index) {

      return text.charAt(index);
    }

    @Override
    public CharSequence subSequence (int start, int end) {

      return text.subSequence(start, end);
    }
  }
}
