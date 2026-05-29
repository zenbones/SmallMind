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
package org.smallmind.nutsnbolts.io;

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class StenographWriterTest {

  private static class RecordingListener implements StenographEventListener {

    private final List<String> received = new ArrayList<>();

    @Override
    public void flush (StenographEvent stenographEvent) {

      received.add(stenographEvent.getOutput());
    }
  }

  public void testWriteThenFlushDispatchesAccumulatedTextAndSourceMatches ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    RecordingListener listener = new RecordingListener();

    writer.addStenographListener(listener);
    writer.write("hello".toCharArray(), 0, 5);
    writer.flush();

    Assert.assertEquals(listener.received, List.of("hello"));
  }

  public void testMultipleWritesAccumulateBeforeFlush ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    RecordingListener listener = new RecordingListener();

    writer.addStenographListener(listener);
    writer.write("foo".toCharArray(), 0, 3);
    writer.write("bar".toCharArray(), 0, 3);
    writer.flush();

    Assert.assertEquals(listener.received, List.of("foobar"));
  }

  public void testFlushClearsBufferSoSubsequentFlushDispatchesOnlyNewContent ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    RecordingListener listener = new RecordingListener();

    writer.addStenographListener(listener);
    writer.write("first".toCharArray(), 0, 5);
    writer.flush();
    writer.write("second".toCharArray(), 0, 6);
    writer.flush();

    Assert.assertEquals(listener.received, List.of("first", "second"));
  }

  public void testCloseDispatchesAccumulatedText ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    RecordingListener listener = new RecordingListener();

    writer.addStenographListener(listener);
    writer.write("done".toCharArray(), 0, 4);
    writer.close();

    Assert.assertEquals(listener.received, List.of("done"));
  }

  public void testRemovedListenerReceivesNoFurtherEvents ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    RecordingListener listener = new RecordingListener();

    writer.addStenographListener(listener);
    writer.removeStenographListener(listener);
    writer.write("ignored".toCharArray(), 0, 7);
    writer.flush();

    Assert.assertEquals(listener.received, List.of());
  }

  public void testEventSourceIsTheOriginatingWriter ()
    throws Exception {

    StenographWriter writer = new StenographWriter();
    Object[] capturedSource = new Object[1];

    writer.addStenographListener(event -> capturedSource[0] = event.getSource());
    writer.write("x".toCharArray(), 0, 1);
    writer.flush();

    Assert.assertSame(capturedSource[0], writer);
  }
}
