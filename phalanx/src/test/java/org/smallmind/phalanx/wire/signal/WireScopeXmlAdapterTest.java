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
package org.smallmind.phalanx.wire.signal;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.phalanx.wire.WireScopeManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import tools.jackson.databind.JsonNode;

/**
 * Exercises {@link WireScopeXmlAdapter} inside a bound {@link PerApplicationScope}, and the two cross-flavor
 * mismatches a receiver can hit: a wire scope decoded on a thread with no scope, and a wire context decoded on a
 * thread with no context. Both throw out of the adapter rather than degrading silently.
 */
@Test(groups = "unit")
public class WireScopeXmlAdapterTest {

  @Test
  public void testMarshalNullReturnsNull () {

    Assert.assertNull(new WireScopeXmlAdapter().marshal(null));
  }

  @Test
  public void testMarshalEmptyReturnsNull () {

    Assert.assertNull(new WireScopeXmlAdapter().marshal(new WireScope[0]));
  }

  @Test
  public void testUnmarshalNullReturnsEmptyArray () {

    Assert.assertEquals(new WireScopeXmlAdapter().unmarshal(null).length, 0);
  }

  @Test
  public void testRegisteredTypeRoundTripsInsideTheScope () {

    PerApplicationScope scope = new PerApplicationScope();
    WireScopeXmlAdapter adapter = new WireScopeXmlAdapter();
    AdapterScope adapterScope = new AdapterScope();

    adapterScope.setValue("payload");

    scope.run(() -> {
      WireScopeManager.register("AdapterScope", AdapterScope.class);

      JsonNode node = adapter.marshal(new WireScope[] {adapterScope});
      WireScope[] unmarshalled = adapter.unmarshal(node);

      Assert.assertEquals(unmarshalled.length, 1);
      Assert.assertTrue(unmarshalled[0] instanceof AdapterScope);
      Assert.assertEquals(((AdapterScope)unmarshalled[0]).getValue(), "payload");
    });
  }

  @Test
  public void testUnregisteredTypeBecomesProtoScope () {

    PerApplicationScope scope = new PerApplicationScope();
    WireScopeXmlAdapter adapter = new WireScopeXmlAdapter();
    ProtoWireScope proto = new ProtoWireScope("xa-unregistered-tag", "raw-payload");

    scope.run(() -> {

      JsonNode node = adapter.marshal(new WireScope[] {proto});
      WireScope[] unmarshalled = adapter.unmarshal(node);

      Assert.assertEquals(unmarshalled.length, 1);
      Assert.assertTrue(unmarshalled[0] instanceof ProtoWireScope);
      Assert.assertEquals(((ProtoWireScope)unmarshalled[0]).getSkin(), "xa-unregistered-tag");
    });
  }

  // A scope that no refresh registered into resolves nothing; the element degrades to a proto scope with no error.
  @Test
  public void testScopeWithoutRegistrationsDegradesToProtoScope () {

    WireScopeXmlAdapter adapter = new WireScopeXmlAdapter();
    AdapterScope adapterScope = new AdapterScope();

    adapterScope.setValue("payload");

    JsonNode node = new PerApplicationScope().call(() -> adapter.marshal(new WireScope[] {adapterScope}));
    WireScope[] unmarshalled = new PerApplicationScope().call(() -> adapter.unmarshal(node));

    Assert.assertEquals(unmarshalled.length, 1);
    Assert.assertTrue(unmarshalled[0] instanceof ProtoWireScope, "an unregistered handle must degrade, not fail: " + unmarshalled[0]);
  }

  // A context-only receiver (no scope on the worker) cannot decode a wire scope element.
  @Test
  public void testWireScopeOnAThreadWithoutAScopeThrows ()
    throws Exception {

    WireScopeXmlAdapter adapter = new WireScopeXmlAdapter();
    JsonNode node = new PerApplicationScope().call(() -> adapter.marshal(new WireScope[] {new ProtoWireScope("tag", "payload")}));

    try {
      onIsolatedThread(() -> {
        new PerApplicationContext();

        return adapter.unmarshal(node);
      });
      Assert.fail("decoding a wire scope with no scope bound must throw");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  // A scope-only receiver (no context on the worker) cannot decode a wire context element.
  @Test
  public void testWireContextOnAThreadWithoutAContextThrows ()
    throws Exception {

    WireContextXmlAdapter adapter = new WireContextXmlAdapter();
    JsonNode node = onIsolatedThread(() -> {
      new PerApplicationContext();
      WireContextManager.register("tag", WireContextXmlAdapterTest.AdapterContext.class);

      return adapter.marshal(new WireContext[] {new ProtoWireContext("tag", "payload")});
    });

    try {
      onIsolatedThread(() -> new PerApplicationScope().call(() -> adapter.unmarshal(node)));
      Assert.fail("decoding a wire context with no context attached must throw");
    } catch (MissingPerApplicationContextException missingPerApplicationContextException) {
      // expected
    }
  }

  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw (Error)failure.get();
    }

    return result.get();
  }

  public static class AdapterScope extends WireScope {

    private String value;

    public String getValue () {

      return value;
    }

    public void setValue (String value) {

      this.value = value;
    }
  }
}
