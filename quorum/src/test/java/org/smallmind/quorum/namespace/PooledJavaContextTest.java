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
package org.smallmind.quorum.namespace;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import javax.naming.CommunicationException;
import javax.naming.Name;
import javax.naming.NamingException;
import org.smallmind.quorum.namespace.NamespaceTestSupport.RecordingDirContext;
import org.smallmind.quorum.namespace.NamespaceTestSupport.StubNameTranslator;
import org.smallmind.quorum.namespace.event.JavaContextEvent;
import org.smallmind.quorum.namespace.event.JavaContextListener;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies the pool-facing behaviour added by {@link PooledJavaContext}: a backing-store
 * {@link CommunicationException} fires an abort event before propagating, the no-argument
 * {@link PooledJavaContext#close()} signals a logical return (rather than physically closing the
 * connection), and {@link PooledJavaContext#close(boolean) close(true)} performs the physical close.
 */
@Test(groups = "unit")
public class PooledJavaContextTest {

  private final StubNameTranslator translator = new StubNameTranslator();

  private PooledJavaContext pooledContext (RecordingDirContext backing) {

    return new PooledJavaContext(new Hashtable<>(), backing, translator, new JavaNameParser(translator), false);
  }

  public void testCommunicationFailureFiresAbortEventAndRethrows ()
    throws NamingException {

    CommunicationException failure = new CommunicationException("backing store unreachable");
    // A backing context whose lookup fails with a communication error, modelling a dropped connection.
    RecordingDirContext throwingBacking = new RecordingDirContext() {

      @Override
      public Object lookup (Name name)
        throws NamingException {

        throw failure;
      }
    };

    PooledJavaContext context = new PooledJavaContext(new Hashtable<>(), throwingBacking, translator, new JavaNameParser(translator), false);
    RecordingListener listener = new RecordingListener();

    context.addJavaContextListener(listener);

    // Exercise the Name-typed overload directly: the String overload would delegate back through this
    // same polymorphic method and fire the abort on both stack frames.
    Name target = new JavaNameParser(translator).parse("alpha");
    CommunicationException thrown = Assert.expectThrows(CommunicationException.class, () -> context.lookup(target));

    Assert.assertSame(thrown, failure, "the original communication exception should propagate unchanged");
    Assert.assertEquals(listener.getAborted().size(), 1, "an abort event should fire exactly once");
    Assert.assertSame(listener.getAborted().get(0).getCommunicationException(), failure);
    Assert.assertTrue(listener.getClosed().isEmpty(), "a failed operation must not signal a clean close");
  }

  public void testLogicalCloseFiresClosedEventWithoutPhysicalClose ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    PooledJavaContext context = pooledContext(backing);
    RecordingListener listener = new RecordingListener();

    context.addJavaContextListener(listener);
    context.close();

    Assert.assertEquals(listener.getClosed().size(), 1, "a logical close should fire one closed event");
    Assert.assertFalse(listener.getClosed().get(0).containsCommunicationException(), "a clean close carries no communication exception");
    Assert.assertEquals(backing.getCloseCount(), 0, "a logical close must not physically close the backing connection");
  }

  public void testForcedCloseClosesTheBackingConnection ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    PooledJavaContext context = pooledContext(backing);
    RecordingListener listener = new RecordingListener();

    context.addJavaContextListener(listener);
    context.close(true);

    Assert.assertEquals(backing.getCloseCount(), 1, "a forced close should physically close the backing connection");
    Assert.assertTrue(listener.getClosed().isEmpty(), "a forced close should not fire a logical closed event");
  }

  public void testRemovedListenerStopsReceivingEvents ()
    throws Exception {

    RecordingDirContext backing = new RecordingDirContext();
    PooledJavaContext context = pooledContext(backing);
    RecordingListener listener = new RecordingListener();

    context.addJavaContextListener(listener);
    context.removeJavaContextListener(listener);
    context.close();

    Assert.assertTrue(listener.getClosed().isEmpty(), "a removed listener should no longer be notified");
  }

  private static class RecordingListener implements JavaContextListener {

    private final List<JavaContextEvent> closed = new ArrayList<>();
    private final List<JavaContextEvent> aborted = new ArrayList<>();

    private List<JavaContextEvent> getClosed () {

      return closed;
    }

    private List<JavaContextEvent> getAborted () {

      return aborted;
    }

    @Override
    public void contextClosed (JavaContextEvent javaContextEvent) {

      closed.add(javaContextEvent);
    }

    @Override
    public void contextAborted (JavaContextEvent javaContextEvent) {

      aborted.add(javaContextEvent);
    }
  }
}
