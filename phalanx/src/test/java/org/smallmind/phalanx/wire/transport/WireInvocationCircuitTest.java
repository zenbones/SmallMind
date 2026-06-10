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

import java.util.HashMap;
import java.util.Map;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.web.json.scaffold.fault.Fault;
import org.smallmind.web.json.scaffold.fault.FaultElement;
import org.smallmind.web.json.scaffold.fault.FaultWrappingException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the server-side dispatch and error-propagation contract of {@link WireInvocationCircuit}:
 * a successful call transmits the result, a service-thrown exception and an unregistered service
 * both come back as a {@link Fault}, a non-{@link java.io.Serializable} result is rejected as a
 * fault, and a fire-and-forget (in-only) failure transmits nothing at all.
 */
@Test(groups = "unit")
public class WireInvocationCircuitTest {

  private final SignalCodec signalCodec = new JsonSignalCodec();

  private InvocationSignal invocation (boolean inOnly, int version, String service, Function function, Map<String, Object> arguments) {

    return new InvocationSignal(inOnly, new Route(version, service, function), arguments);
  }

  @Test
  public void testSuccessfulInvocationTransmitsResult ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(WireTestingService.class, new WireTestingServiceImpl());

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("string", "hello");

    Function function = new Function(WireTestingService.class.getMethod("echoString", String.class));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 1, "WireTestService", function, arguments));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertFalse(transmitter.error);
    Assert.assertEquals(transmitter.result, "hello");
    Assert.assertEquals(transmitter.nativeType, "Ljava/lang/String;");
  }

  @Test
  public void testServiceExceptionIsReturnedAsFault ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(WireTestingService.class, new WireTestingServiceImpl());

    Function function = new Function(WireTestingService.class.getMethod("throwError"));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 1, "WireTestService", function, new HashMap<>()));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertTrue(transmitter.error);
    Assert.assertTrue(transmitter.result instanceof Fault);
  }

  @Test
  public void testUnregisteredServiceIsReturnedAsFault ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    Function function = new Function(WireTestingService.class.getMethod("echoString", String.class));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 99, "Nonexistent", function, new HashMap<>()));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertTrue(transmitter.error);
    Assert.assertTrue(transmitter.result instanceof Fault);
  }

  @Test
  public void testInOnlyFailureTransmitsNothing ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    Function function = new Function(WireTestingService.class.getMethod("echoString", String.class));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(true, 99, "Nonexistent", function, new HashMap<>()));

    Assert.assertFalse(transmitter.transmitted);
  }

  @Test
  public void testNonSerializableResultIsRejectedAsFault ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(UnserializableService.class, new UnserializableServiceImpl());

    Function function = new Function(UnserializableService.class.getMethod("make"));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 7, "Unserializable", function, new HashMap<>()));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertTrue(transmitter.error);
    Assert.assertTrue(transmitter.result instanceof Fault);
  }

  @Test
  public void testPartialFunctionIsResolvedByMatch ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(WireTestingService.class, new WireTestingServiceImpl());

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("string", "hello");

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 1, "WireTestService", new Function("echoString"), arguments));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertFalse(transmitter.error);
    Assert.assertEquals(transmitter.result, "hello");
  }

  @Test
  public void testUnresolvablePartialFunctionBecomesFault ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(WireTestingService.class, new WireTestingServiceImpl());

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 1, "WireTestService", new Function("noSuchMethod"), new HashMap<>()));

    Assert.assertTrue(transmitter.transmitted);
    Assert.assertTrue(transmitter.error);
    Assert.assertTrue(transmitter.result instanceof Fault);
  }

  @Test
  public void testFaultWrappingExceptionReturnsEmbeddedFault ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();
    Fault embeddedFault = new Fault(new FaultElement("Faulty", "boom"), new RuntimeException("boom"));

    circuit.register(FaultyService.class, new FaultyServiceImpl(embeddedFault));

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 8, "Faulty", new Function(FaultyService.class.getMethod("boom")), new HashMap<>()));

    Assert.assertTrue(transmitter.error);
    Assert.assertSame(transmitter.result, embeddedFault);
  }

  @Test
  public void testDuplicateRegistrationIsIgnored ()
    throws Exception {

    WireInvocationCircuit circuit = new WireInvocationCircuit();
    CapturingTransmitter transmitter = new CapturingTransmitter();

    circuit.register(WireTestingService.class, new WireTestingServiceImpl());
    circuit.register(WireTestingService.class, new WireTestingServiceImpl() {

      @Override
      public String echoString (String string) {

        return "SECOND";
      }
    });

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("string", "hello");

    circuit.handle(transmitter, signalCodec, "caller", "message", invocation(false, 1, "WireTestService", new Function(WireTestingService.class.getMethod("echoString", String.class)), arguments));

    Assert.assertEquals(transmitter.result, "hello");
  }

  private static class CapturingTransmitter implements ResponseTransmitter {

    private Object result;
    private String nativeType;
    private boolean transmitted;
    private boolean error;

    @Override
    public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result) {

      this.transmitted = true;
      this.error = error;
      this.nativeType = nativeType;
      this.result = result;
    }
  }

  public interface UnserializableService {

    Object make ();
  }

  public static class UnserializableServiceImpl implements UnserializableService, WiredService {

    @Override
    public int getVersion () {

      return 7;
    }

    @Override
    public String getServiceName () {

      return "Unserializable";
    }

    @Override
    public void setResponseTransport (ResponseTransport responseTransport) {

    }

    @Override
    public Object make () {

      return new Object();
    }
  }

  public interface FaultyService {

    void boom ()
      throws FaultWrappingException;
  }

  public static class FaultyServiceImpl implements FaultyService, WiredService {

    private final Fault fault;

    public FaultyServiceImpl (Fault fault) {

      this.fault = fault;
    }

    @Override
    public int getVersion () {

      return 8;
    }

    @Override
    public String getServiceName () {

      return "Faulty";
    }

    @Override
    public void setResponseTransport (ResponseTransport responseTransport) {

    }

    @Override
    public void boom ()
      throws FaultWrappingException {

      throw new FaultWrappingException(fault);
    }
  }
}
