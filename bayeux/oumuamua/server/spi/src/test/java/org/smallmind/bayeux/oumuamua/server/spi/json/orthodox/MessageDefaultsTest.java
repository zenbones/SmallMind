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
package org.smallmind.bayeux.oumuamua.server.spi.json.orthodox;

import java.nio.charset.StandardCharsets;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MessageDefaultsTest {

  private OrthodoxValueFactory factory;
  private OrthodoxCodec codec;

  @BeforeClass
  public void beforeClass () {

    factory = new OrthodoxValueFactory();
    codec = new OrthodoxCodec(new JaxbDeserializer<>());
  }

  private OrthodoxMessage message () {

    return new OrthodoxMessage(codec, factory);
  }

  public void testIsSuccessfulReturnsFalseWhenAbsent () {

    Assert.assertFalse(message().isSuccessful());
  }

  public void testIsSuccessfulReturnsTrueWhenTrue () {

    OrthodoxMessage message = message();

    message.put(Message.SUCCESSFUL, factory.booleanValue(true));

    Assert.assertTrue(message.isSuccessful());
  }

  public void testIsSuccessfulReturnsFalseWhenFalse () {

    OrthodoxMessage message = message();

    message.put(Message.SUCCESSFUL, factory.booleanValue(false));

    Assert.assertFalse(message.isSuccessful());
  }

  public void testIsSuccessfulReturnsFalseWhenNotBoolean () {

    OrthodoxMessage message = message();

    message.put(Message.SUCCESSFUL, factory.textValue("true"));

    Assert.assertFalse(message.isSuccessful());
  }

  public void testGetIdReturnsNullWhenAbsent () {

    Assert.assertNull(message().getId());
  }

  public void testGetIdReturnsValueWhenPresent () {

    OrthodoxMessage message = message();

    message.put(Message.ID, factory.textValue("msg-1"));

    Assert.assertEquals(message.getId(), "msg-1");
  }

  public void testGetIdReturnsNullWhenNotString () {

    OrthodoxMessage message = message();

    message.put(Message.ID, factory.numberValue(42));

    Assert.assertNull(message.getId());
  }

  public void testGetSessionIdReturnsValueWhenPresent () {

    OrthodoxMessage message = message();

    message.put(Message.SESSION_ID, factory.textValue("session-abc"));

    Assert.assertEquals(message.getSessionId(), "session-abc");
  }

  public void testGetChannelReturnsValueWhenPresent () {

    OrthodoxMessage message = message();

    message.put(Message.CHANNEL, factory.textValue("/meta/connect"));

    Assert.assertEquals(message.getChannel(), "/meta/connect");
  }

  public void testGetAdviceReturnsNullWhenAbsent () {

    Assert.assertNull(message().getAdvice());
  }

  public void testGetAdviceCreatesWhenRequested () {

    OrthodoxMessage message = message();

    ObjectValue<OrthodoxValue> first = message.getAdvice(true);

    Assert.assertNotNull(first);
    Assert.assertSame(message.getAdvice(true), first);
  }

  public void testGetExtCreatesWhenRequested () {

    OrthodoxMessage message = message();

    Assert.assertNotNull(message.getExt(true));
  }

  public void testGetDataCreatesWhenRequested () {

    OrthodoxMessage message = message();

    Assert.assertNotNull(message.getData(true));
  }

  public void testEncodeProducesJson ()
    throws Exception {

    OrthodoxMessage message = message();

    message.put(Message.CHANNEL, factory.textValue("/test"));

    String json = new String(message.encode(), StandardCharsets.UTF_8);

    Assert.assertTrue(json.contains("channel"));
    Assert.assertTrue(json.contains("/test"));
  }
}
