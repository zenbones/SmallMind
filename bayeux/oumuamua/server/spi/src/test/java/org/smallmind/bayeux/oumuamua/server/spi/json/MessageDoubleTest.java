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
package org.smallmind.bayeux.oumuamua.server.spi.json;

import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueType;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MessageDoubleTest {

  private OrthodoxValueFactory factory;

  @BeforeMethod
  public void beforeMethod () {

    factory = new OrthodoxValueFactory();
  }

  private Message<OrthodoxValue> base () {

    Message<OrthodoxValue> message = new OrthodoxMessage(null, factory);

    message.put(Message.CHANNEL, "/foo/bar");
    message.put(Message.ID, "42");
    message.put(Message.SESSION_ID, "session-abc");
    message.put(Message.SUCCESSFUL, true);

    return message;
  }

  public void testReadsBayeuxFieldsThrough () {

    MessageDouble<OrthodoxValue> doubled = new MessageDouble<>(base());

    Assert.assertEquals(doubled.getChannel(), "/foo/bar");
    Assert.assertEquals(doubled.getId(), "42");
    Assert.assertEquals(doubled.getSessionId(), "session-abc");
    Assert.assertTrue(doubled.isSuccessful());
    Assert.assertEquals(doubled.getType(), ValueType.OBJECT);
  }

  public void testWriteToDoubleDoesNotMutateInner () {

    Message<OrthodoxValue> inner = base();
    MessageDouble<OrthodoxValue> doubled = new MessageDouble<>(inner);

    doubled.put(Message.CHANNEL, "/other");

    Assert.assertEquals(doubled.getChannel(), "/other");
    Assert.assertEquals(inner.getChannel(), "/foo/bar");
  }

  public void testRemoveOnDoubleHidesField () {

    Message<OrthodoxValue> inner = base();
    MessageDouble<OrthodoxValue> doubled = new MessageDouble<>(inner);

    doubled.remove(Message.SESSION_ID);
    Assert.assertNull(doubled.getSessionId());
    Assert.assertEquals(inner.getSessionId(), "session-abc");
  }

  public void testGetExtCreatesOverlayObject () {

    Message<OrthodoxValue> inner = base();
    MessageDouble<OrthodoxValue> doubled = new MessageDouble<>(inner);

    Assert.assertNull(doubled.getExt());
    Assert.assertNotNull(doubled.getExt(true));
    Assert.assertNotNull(doubled.getExt());
    Assert.assertNull(inner.getExt());
  }

  public void testNestedReadIsAutoWrapped () {

    Message<OrthodoxValue> inner = base();

    inner.put(Message.DATA, factory.objectValue().put("k", factory.textValue("v")));

    MessageDouble<OrthodoxValue> doubled = new MessageDouble<>(inner);

    Assert.assertTrue(doubled.getData() instanceof MergingObjectValue);
    Assert.assertEquals(doubled.getData().size(), 1);
  }
}
