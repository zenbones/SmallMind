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
package org.smallmind.phalanx.wire.transport;

import java.lang.reflect.InvocationTargetException;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.phalanx.wire.MissingInvocationException;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.WireTestingException;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests the server-side method resolution and dispatch surface of {@link MethodInvoker}: matching a
 * partial (name-only) function descriptor to a complete one, reporting an unknown invocation, the
 * unwrapping of a service-thrown checked exception, and the push/pop of wire contexts around a call.
 */
@Test(groups = "unit")
public class MethodInvokerTest {

  private MethodInvoker methodInvoker;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    methodInvoker = new MethodInvoker(new WireTestingServiceImpl(), WireTestingService.class);
  }

  @Test
  public void testMatchResolvesPartialDescriptorByName () {

    Function matched = methodInvoker.match(new Function("echoString"));

    Assert.assertNotNull(matched);
    Assert.assertEquals(matched.getName(), "echoString");
    Assert.assertFalse(matched.isPartial());
  }

  @Test
  public void testMatchReturnsNullForUnknownName () {

    Assert.assertNull(methodInvoker.match(new Function("doesNotExist")));
  }

  @Test(expectedExceptions = MissingInvocationException.class)
  public void testGetMethodologyRejectsUnknownFunction ()
    throws MissingInvocationException {

    methodInvoker.getMethodology(new Function("doesNotExist", "V"));
  }

  @Test
  public void testRemoteInvocationReturnsServiceResult ()
    throws Exception {

    Function function = new Function(WireTestingService.class.getMethod("echoString", String.class));

    Assert.assertEquals(methodInvoker.remoteInvocation(null, function, "hi"), "hi");
  }

  @Test(expectedExceptions = WireTestingException.class)
  public void testRemoteInvocationUnwrapsServiceException ()
    throws Exception {

    methodInvoker.remoteInvocation(null, new Function(WireTestingService.class.getMethod("throwError")));
  }

  @Test
  public void testRemoteInvocationPushesContextForTheCallAndPopsItAfter ()
    throws Exception {

    Function function = new Function(WireTestingService.class.getMethod("hasContext"));

    Assert.assertEquals(methodInvoker.remoteInvocation(new WireContext[] {new TestWireContext("flibble")}, function), Boolean.TRUE);
    Assert.assertFalse(ContextFactory.exists(TestWireContext.class));
  }

  @Test
  public void testSyntheticObjectMethodsDispatch ()
    throws Exception {

    WireTestingServiceImpl target = new WireTestingServiceImpl();
    MethodInvoker invoker = new MethodInvoker(target, WireTestingService.class);
    Function equalsFunction = new Function(target.getClass().getMethod("equals", Object.class));
    Function hashCodeFunction = new Function(target.getClass().getMethod("hashCode"));

    Assert.assertEquals(invoker.remoteInvocation(null, equalsFunction, target), Boolean.TRUE);
    Assert.assertEquals(invoker.remoteInvocation(null, equalsFunction, new Object()), Boolean.FALSE);
    Assert.assertEquals(invoker.remoteInvocation(null, hashCodeFunction), target.hashCode());
  }

  @Test(expectedExceptions = InvocationTargetException.class)
  public void testNonExceptionCauseIsRethrownAsInvocationTargetException ()
    throws Exception {

    new MethodInvoker(new ErrorServiceImpl(), ErrorService.class).remoteInvocation(null, new Function(ErrorService.class.getMethod("boom")));
  }

  @Test
  public void testMatchHonoursSignatureAndResultType ()
    throws Exception {

    Function wrongSignature = new Function(WireTestingService.class.getMethod("echoString", String.class));
    Function wrongResultType = new Function(WireTestingService.class.getMethod("echoString", String.class));

    //  A complete, correct descriptor matches (exercises the signature- and result-type-equal arms);
    //  a descriptor that differs in either must NOT match, or the wrong method would be invoked with
    //  the caller's arguments.
    Assert.assertNotNull(methodInvoker.match(new Function(WireTestingService.class.getMethod("echoString", String.class))));

    wrongSignature.setSignature(new String[] {"!nope"});
    Assert.assertNull(methodInvoker.match(wrongSignature));

    wrongResultType.setResultType("!nope");
    Assert.assertNull(methodInvoker.match(wrongResultType));
  }

  @Test
  public void testNullContextElementsAreSkippedAndStackStaysBalanced ()
    throws Exception {

    Function function = new Function(WireTestingService.class.getMethod("hasContext"));

    //  A null element interleaved with a real context must be skipped on both the push and the pop, and
    //  the context stack must be balanced afterward — an imbalance would leak the context onto this
    //  pooled thread and bleed it into the next invocation.
    Assert.assertEquals(methodInvoker.remoteInvocation(new WireContext[] {new TestWireContext("flibble"), null}, function), Boolean.TRUE);
    Assert.assertFalse(ContextFactory.exists(TestWireContext.class));
  }

  public interface ErrorService {

    void boom ();
  }

  public static class ErrorServiceImpl implements ErrorService {

    @Override
    public void boom () {

      throw new AssertionError("boom");
    }
  }
}
