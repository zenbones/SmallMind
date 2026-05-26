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
package org.smallmind.memcached.cubby.translator;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class LargeKeyHashingTranslatorTest {

  public void testShortKeysPassThroughDelegateUnchanged ()
    throws Exception {

    DefaultKeyTranslator delegate = new DefaultKeyTranslator();
    LargeKeyHashingTranslator translator = new LargeKeyHashingTranslator(delegate);

    String key = "modest-key";
    String expected = delegate.encode(key);

    Assert.assertEquals(translator.encode(key), expected);
    Assert.assertTrue(expected.length() <= 250, "Precondition: short key encodes under 250 chars");
  }

  public void testOversizeKeyFallsBackToHashAndStaysUnderTwoHundredFiftyCharacters ()
    throws Exception {

    DefaultKeyTranslator delegate = new DefaultKeyTranslator();
    LargeKeyHashingTranslator translator = new LargeKeyHashingTranslator(delegate);

    String longKey = "X".repeat(400);

    Assert.assertTrue(delegate.encode(longKey).length() > 250, "Precondition: delegate encoding exceeds 250 chars");

    String encoded = translator.encode(longKey);

    Assert.assertTrue(encoded.length() <= 250, "Encoded length=" + encoded.length());
    Assert.assertNotEquals(encoded, delegate.encode(longKey));
  }

  public void testHashFallbackIsDeterministic ()
    throws Exception {

    LargeKeyHashingTranslator translator = new LargeKeyHashingTranslator(new DefaultKeyTranslator());
    String longKey = "Y".repeat(400);

    Assert.assertEquals(translator.encode(longKey), translator.encode(longKey));
  }

  public void testDifferentLongKeysProduceDifferentHashedEncodings ()
    throws Exception {

    LargeKeyHashingTranslator translator = new LargeKeyHashingTranslator(new DefaultKeyTranslator());

    Assert.assertNotEquals(translator.encode("A".repeat(400)), translator.encode("B".repeat(400)));
  }
}
