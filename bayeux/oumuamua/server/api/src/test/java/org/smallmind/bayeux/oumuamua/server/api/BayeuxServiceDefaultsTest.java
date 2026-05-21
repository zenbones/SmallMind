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

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Codec;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.api.json.ObjectValue;
import org.smallmind.bayeux.oumuamua.server.api.json.TestValueFactory;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.api.json.ValueFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Pins {@link BayeuxService#createResponse} to delegate channel, id, and session-id
 * fields correctly to a newly created message. Uses inline stubs so no SPI dependency
 * is introduced in the api module.
 */
@Test(groups = "unit")
public class BayeuxServiceDefaultsTest {

  private TestValueFactory factory;
  private BayeuxService<TestValueFactory.TestValue> service;

  @BeforeMethod
  public void beforeMethod () {

    factory = new TestValueFactory();
    service = new BayeuxService<TestValueFactory.TestValue>() {

      @Override
      public Route[] boundRoutes () {

        return new Route[0];
      }

      @Override
      public Packet<TestValueFactory.TestValue> process (Protocol<TestValueFactory.TestValue> protocol, Route route, Server<TestValueFactory.TestValue> server, Session<TestValueFactory.TestValue> session, Message<TestValueFactory.TestValue> request) {

        return null;
      }
    };
  }

  private Message<TestValueFactory.TestValue> requestWithId (String id) {

    SimpleMessage request = new SimpleMessage(factory);

    if (id != null) {
      request.put(Message.ID, factory.textValue(id));
    }

    return request;
  }

  public void testCreateResponseSetsChannelFromRoute () {

    Message<TestValueFactory.TestValue> response = service.createResponse(routeFor("/service/test"), serverFor(), sessionWithId("s-1"), requestWithId("r-1"));

    Assert.assertEquals(response.getChannel(), "/service/test");
  }

  public void testCreateResponseSetsIdFromRequest () {

    Message<TestValueFactory.TestValue> response = service.createResponse(routeFor("/service/test"), serverFor(), sessionWithId("s-2"), requestWithId("r-42"));

    Assert.assertEquals(response.getId(), "r-42");
  }

  public void testCreateResponseSetsSessionIdFromSession () {

    Message<TestValueFactory.TestValue> response = service.createResponse(routeFor("/service/test"), serverFor(), sessionWithId("session-99"), requestWithId("r-3"));

    Assert.assertEquals(response.getSessionId(), "session-99");
  }

  public void testCreateResponseWithNullRequestIdProducesNullId () {

    Message<TestValueFactory.TestValue> response = service.createResponse(routeFor("/service/test"), serverFor(), sessionWithId("s-3"), requestWithId(null));

    Assert.assertNull(response.getId());
  }

  private Route routeFor (String path) {

    return new Route() {

      @Override
      public String getPath () {

        return path;
      }

      @Override
      public int size () {

        return 2;
      }

      @Override
      public int lastIndex () {

        return 1;
      }

      @Override
      public Segment getSegment (int index) {

        throw new UnsupportedOperationException();
      }

      @Override
      public boolean isWild () {

        return false;
      }

      @Override
      public boolean isDeepWild () {

        return false;
      }

      @Override
      public boolean isMeta () {

        return false;
      }

      @Override
      public boolean isService () {

        return true;
      }

      @Override
      public boolean matchesPrefix (String... segments) {

        return false;
      }
    };
  }

  private Server<TestValueFactory.TestValue> serverFor () {

    TestValueFactory capturedFactory = factory;

    return new Server<TestValueFactory.TestValue>() {

      @Override
      public Codec<TestValueFactory.TestValue> getCodec () {

        return new Codec<TestValueFactory.TestValue>() {

          @Override
          public Message<TestValueFactory.TestValue> create () {

            return new SimpleMessage(capturedFactory);
          }

          @Override
          public Message<TestValueFactory.TestValue>[] from (byte[] buffer)
            throws IOException {

            throw new UnsupportedOperationException();
          }

          @Override
          public Message<TestValueFactory.TestValue>[] from (String data)
            throws IOException {

            throw new UnsupportedOperationException();
          }

          @Override
          public Value<TestValueFactory.TestValue> convert (Object object)
            throws IOException {

            throw new UnsupportedOperationException();
          }
        };
      }

      @Override
      public Backbone<TestValueFactory.TestValue> getBackbone () {

        return null;
      }

      @Override
      public SecurityPolicy<TestValueFactory.TestValue> getSecurityPolicy () {

        return null;
      }

      @Override
      public boolean allowsImplicitConnection () {

        return false;
      }

      @Override
      public long getSessionConnectionIntervalMilliseconds () {

        return 0;
      }

      @Override
      public boolean isReflecting (Route route) {

        return false;
      }

      @Override
      public boolean isStreaming (Route route) {

        return false;
      }

      @Override
      public String[] getProtocolNames () {

        return new String[0];
      }

      @Override
      public Protocol<TestValueFactory.TestValue> getProtocol (String name) {

        return null;
      }

      @Override
      public String getBayeuxVersion () {

        return "1.0";
      }

      @Override
      public String getMinimumBayeuxVersion () {

        return "1.0";
      }

      @Override
      public void start (ServletConfig servletConfig)
        throws ServletException {

      }

      @Override
      public void stop () {

      }

      @Override
      public void addService (BayeuxService<TestValueFactory.TestValue> service) {

      }

      @Override
      public void removeService (Route route) {

      }

      @Override
      public BayeuxService<TestValueFactory.TestValue> getService (Route route) {

        return null;
      }

      @Override
      public Session<TestValueFactory.TestValue> getSession (String sessionId) {

        return null;
      }

      @Override
      public void addListener (Listener<TestValueFactory.TestValue> listener) {

      }

      @Override
      public void removeListener (Listener<TestValueFactory.TestValue> listener) {

      }

      @Override
      public void addInitializer (ChannelInitializer<TestValueFactory.TestValue> initializer) {

      }

      @Override
      public void removeInitializer (ChannelInitializer<TestValueFactory.TestValue> initializer) {

      }

      @Override
      public Channel<TestValueFactory.TestValue> findChannel (String path)
        throws InvalidPathException {

        return null;
      }

      @Override
      public Channel<TestValueFactory.TestValue> requireChannel (String path, ChannelInitializer... initializers)
        throws InvalidPathException {

        throw new UnsupportedOperationException();
      }

      @Override
      public void removeChannel (Channel<TestValueFactory.TestValue> channel)
        throws ChannelStateException {

      }

      @Override
      public Packet<TestValueFactory.TestValue> onRequest (Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet) {

        return packet;
      }

      @Override
      public Packet<TestValueFactory.TestValue> onResponse (Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet) {

        return packet;
      }

      @Override
      public void deliver (Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet, boolean clustered) {

      }

      @Override
      public void forward (Channel<TestValueFactory.TestValue> channel, Packet<TestValueFactory.TestValue> packet) {

      }

      @Override
      public Object getAttribute (String name) {

        return null;
      }

      @Override
      public void setAttribute (String name, Object value) {

      }

      @Override
      public Object removeAttribute (String name) {

        return null;
      }

      @Override
      public Set<String> getAttributeNames () {

        return Collections.emptySet();
      }
    };
  }

  private Session<TestValueFactory.TestValue> sessionWithId (String sessionId) {

    return new Session<TestValueFactory.TestValue>() {

      @Override
      public String getId () {

        return sessionId;
      }

      @Override
      public SessionState getState () {

        return SessionState.CONNECTED;
      }

      @Override
      public boolean isLocal () {

        return true;
      }

      @Override
      public boolean isLongPolling () {

        return false;
      }

      @Override
      public void setLongPolling (boolean longPolling) {

      }

      @Override
      public int getMaxLongPollQueueSize () {

        return 0;
      }

      @Override
      public void completeHandshake () {

      }

      @Override
      public void completeConnection () {

      }

      @Override
      public void completeDisconnect () {

      }

      @Override
      public void addListener (Listener<TestValueFactory.TestValue> listener) {

      }

      @Override
      public void removeListener (Listener<TestValueFactory.TestValue> listener) {

      }

      @Override
      public void deliver (Channel<TestValueFactory.TestValue> channel, Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet) {

      }

      @Override
      public Packet<TestValueFactory.TestValue> onResponse (Session<TestValueFactory.TestValue> sender, Packet<TestValueFactory.TestValue> packet) {

        return packet;
      }

      @Override
      public void dispatch (Packet<TestValueFactory.TestValue> packet) {

      }

      @Override
      public Packet<TestValueFactory.TestValue> poll (long timeout, TimeUnit unit)
        throws InterruptedException {

        return null;
      }

      @Override
      public Object getAttribute (String name) {

        return null;
      }

      @Override
      public void setAttribute (String name, Object value) {

      }

      @Override
      public Object removeAttribute (String name) {

        return null;
      }

      @Override
      public Set<String> getAttributeNames () {

        return Collections.emptySet();
      }
    };
  }

  /**
   * Self-backed message where {@code put} returns {@code this}, allowing the chain in
   * {@link BayeuxService#createResponse} to remain typed as {@link Message} through the cast.
   */
  private static class SimpleMessage implements Message<TestValueFactory.TestValue>, TestValueFactory.TestValue {

    private final TestValueFactory factory;
    private final LinkedHashMap<String, Value<TestValueFactory.TestValue>> map = new LinkedHashMap<>();

    SimpleMessage (TestValueFactory factory) {

      this.factory = factory;
    }

    @Override
    public void encode (Writer writer)
      throws IOException {

      writer.write("{}");
    }

    @Override
    public ValueFactory<TestValueFactory.TestValue> getFactory () {

      return factory;
    }

    @Override
    public int size () {

      return map.size();
    }

    @Override
    public boolean isEmpty () {

      return map.isEmpty();
    }

    @Override
    public Iterator<String> fieldNames () {

      return map.keySet().iterator();
    }

    @Override
    public Value<TestValueFactory.TestValue> get (String field) {

      return map.get(field);
    }

    @Override
    public <U extends Value<TestValueFactory.TestValue>> ObjectValue<TestValueFactory.TestValue> put (String field, U value) {

      map.put(field, value);

      return this;
    }

    @Override
    public Value<TestValueFactory.TestValue> remove (String field) {

      return map.remove(field);
    }

    @Override
    public ObjectValue<TestValueFactory.TestValue> removeAll () {

      map.clear();

      return this;
    }
  }
}
