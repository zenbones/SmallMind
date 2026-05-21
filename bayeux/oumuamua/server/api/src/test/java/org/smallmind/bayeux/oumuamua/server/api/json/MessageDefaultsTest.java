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
package org.smallmind.bayeux.oumuamua.server.api.json;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Pins each default accessor on {@link Message} (the typed view over Bayeux JSON fields)
 * against the in-memory {@link TestValueFactory}. Covers happy-path reads, missing-field
 * reads, type-mismatch reads, and the create-if-absent behavior of
 * {@code getAdvice} / {@code getExt} / {@code getData}.
 */
@Test(groups = "unit")
public class MessageDefaultsTest {

  private TestValueFactory factory;
  private TestValueFactory.TestMessage message;

  @BeforeMethod
  public void beforeMethod () {

    factory = new TestValueFactory();
    message = factory.message();
  }

  public void testIsSuccessfulTrueWhenFlagPresentAndTrue () {

    message.put(Message.SUCCESSFUL, true);

    Assert.assertTrue(message.isSuccessful());
  }

  public void testIsSuccessfulFalseWhenFlagPresentAndFalse () {

    message.put(Message.SUCCESSFUL, false);

    Assert.assertFalse(message.isSuccessful());
  }

  public void testIsSuccessfulFalseWhenFlagAbsent () {

    Assert.assertFalse(message.isSuccessful());
  }

  public void testIsSuccessfulFalseWhenFlagIsString () {

    message.put(Message.SUCCESSFUL, "true");

    Assert.assertFalse(message.isSuccessful());
  }

  public void testGetIdReturnsStringFieldValue () {

    message.put(Message.ID, "42");

    Assert.assertEquals(message.getId(), "42");
  }

  public void testGetIdReturnsNullWhenAbsent () {

    Assert.assertNull(message.getId());
  }

  public void testGetIdReturnsNullWhenWrongType () {

    message.put(Message.ID, 42);

    Assert.assertNull(message.getId());
  }

  public void testGetSessionIdReturnsStringFieldValue () {

    message.put(Message.SESSION_ID, "session-1");

    Assert.assertEquals(message.getSessionId(), "session-1");
  }

  public void testGetSessionIdReturnsNullWhenAbsent () {

    Assert.assertNull(message.getSessionId());
  }

  public void testGetChannelReturnsStringFieldValue () {

    message.put(Message.CHANNEL, "/meta/handshake");

    Assert.assertEquals(message.getChannel(), "/meta/handshake");
  }

  public void testGetChannelReturnsNullWhenAbsent () {

    Assert.assertNull(message.getChannel());
  }

  public void testGetAdviceReturnsNullWhenAbsent () {

    Assert.assertNull(message.getAdvice());
  }

  public void testGetAdviceCreatesObjectWhenRequestedAndAbsent () {

    ObjectValue<TestValueFactory.TestValue> advice = message.getAdvice(true);

    Assert.assertNotNull(advice);
    Assert.assertSame(message.get(Message.ADVICE), advice);
  }

  public void testGetAdviceReturnsExistingWhenPresent () {

    message.getAdvice(true).put("retry", true);

    ObjectValue<TestValueFactory.TestValue> first = message.getAdvice();
    ObjectValue<TestValueFactory.TestValue> second = message.getAdvice(true);

    Assert.assertSame(second, first);
  }

  public void testGetExtCreatesObjectWhenRequestedAndAbsent () {

    Assert.assertNull(message.getExt());

    ObjectValue<TestValueFactory.TestValue> ext = message.getExt(true);

    Assert.assertNotNull(ext);
    Assert.assertSame(message.get(Message.EXT), ext);
  }

  public void testGetDataCreatesObjectWhenRequestedAndAbsent () {

    Assert.assertNull(message.getData());

    ObjectValue<TestValueFactory.TestValue> data = message.getData(true);

    Assert.assertNotNull(data);
    Assert.assertSame(message.get(Message.DATA), data);
  }

  public void testGetAdviceReturnsNullWhenFieldIsNotObject () {

    message.put(Message.ADVICE, "not-an-object");

    Assert.assertNull(message.getAdvice());
  }

  public void testGetDataIgnoresExistingNonObjectAndCreatesWhenRequested () {

    message.put(Message.DATA, "scalar");

    ObjectValue<TestValueFactory.TestValue> data = message.getData(true);

    Assert.assertNotNull(data);
    Assert.assertSame(message.get(Message.DATA), data);
  }

  public void testEncodeProducesJsonBytes ()
    throws Exception {

    byte[] bytes = message.encode();

    Assert.assertEquals(new String(bytes, java.nio.charset.StandardCharsets.UTF_8), "{}");
  }
}
