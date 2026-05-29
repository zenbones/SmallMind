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
package org.smallmind.nutsnbolts.reflection;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.smallmind.nutsnbolts.reflection.sample.Calculator;
import org.smallmind.nutsnbolts.reflection.sample.Counter;
import org.smallmind.nutsnbolts.reflection.sample.Greeter;
import org.smallmind.nutsnbolts.reflection.sample.PojoBag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ProxyGeneratorTest {

  public void testInterfaceProxyDispatchesEveryCallThroughHandler () {

    RecordingHandler recordingHandler = new RecordingHandler("answer");
    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, recordingHandler);

    Assert.assertEquals(proxy.greet("world"), "answer");
    Assert.assertEquals(recordingHandler.calls.size(), 1);
    Assert.assertEquals(recordingHandler.calls.get(0).methodName, "greet");
    Assert.assertEquals(recordingHandler.calls.get(0).args[0], "world");
  }

  public void testInterfaceProxyPropagatesPrimitiveReturn () {

    RecordingHandler recordingHandler = new RecordingHandler(42);
    Counter proxy = ProxyGenerator.createProxy(Counter.class, recordingHandler);

    Assert.assertEquals(proxy.count(), 42);
  }

  public void testInterfaceProxyForwardsPrimitiveArguments () {

    RecordingHandler recordingHandler = new RecordingHandler(99L);
    Calculator proxy = ProxyGenerator.createProxy(Calculator.class, recordingHandler);

    Assert.assertEquals(proxy.add(7, 3), 99L);
    Assert.assertEquals(recordingHandler.calls.get(0).args[0], 7L);
    Assert.assertEquals(recordingHandler.calls.get(0).args[1], 3L);
  }

  public void testInterfaceProxyWithNullHandlerReturnsDefaults () {

    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, null);

    Assert.assertNull(proxy.greet("anything"));
  }

  public void testClassProxyRoutesObjectMethodsThroughHandler () {

    RecordingHandler recordingHandler = new RecordingHandler(null) {
      @Override
      public Object invoke (Object proxy, java.lang.reflect.Method method, Object[] args) {

        if ("toString".equals(method.getName())) {
          return "stringified";
        } else if ("hashCode".equals(method.getName())) {
          return 7;
        } else if ("equals".equals(method.getName())) {
          return Boolean.TRUE;
        }

        return null;
      }
    };

    PojoBag proxy = ProxyGenerator.createProxy(PojoBag.class, recordingHandler);

    Assert.assertEquals(proxy.toString(), "stringified");
    Assert.assertEquals(proxy.hashCode(), 7);
    Assert.assertTrue(proxy.equals("anything"));
  }

  public void testHandlerThrowableSurfacesAsUndeclaredThrowable () {

    RecordingHandler recordingHandler = new RecordingHandler(null) {
      @Override
      public Object invoke (Object proxy, java.lang.reflect.Method method, Object[] args)
        throws Throwable {

        throw new IllegalStateException("forced");
      }
    };

    Greeter proxy = ProxyGenerator.createProxy(Greeter.class, recordingHandler);

    try {
      proxy.greet("x");
      Assert.fail("Expected handler exception to surface");
    } catch (RuntimeException runtimeException) {
      Assert.assertTrue(runtimeException instanceof IllegalStateException
        || runtimeException.getCause() instanceof IllegalStateException, "Unexpected exception " + runtimeException);
    }
  }

  public void testGenerationIsCachedAcrossInvocations () {

    Greeter first = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("a"));
    Greeter second = ProxyGenerator.createProxy(Greeter.class, new RecordingHandler("b"));

    Assert.assertSame(first.getClass(), second.getClass());
    Assert.assertNotSame(first, second);
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class)
  public void testNonPublicClassIsRejected () {

    ProxyGenerator.createProxy(PackagePrivateClass.class, new RecordingHandler("x"));
  }

  @Test(expectedExceptions = ByteCodeManipulationException.class)
  public void testStaticNestedClassIsRejected () {

    ProxyGenerator.createProxy(PublicStaticNested.class, new RecordingHandler("x"));
  }

  static class PackagePrivateClass {

  }

  public static class PublicStaticNested {

  }

  private static class RecordingHandler implements InvocationHandler {

    private final List<Invocation> calls = new ArrayList<>();
    private final Object returnValue;

    RecordingHandler (Object returnValue) {

      this.returnValue = returnValue;
    }

    @Override
    public Object invoke (Object proxy, java.lang.reflect.Method method, Object[] args)
      throws Throwable {

      calls.add(new Invocation(method.getName(), args == null ? new Object[0] : Arrays.copyOf(args, args.length)));

      return returnValue;
    }
  }

  private static class Invocation {

    private final String methodName;
    private final Object[] args;

    Invocation (String methodName, Object[] args) {

      this.methodName = methodName;
      this.args = args;
    }
  }
}
