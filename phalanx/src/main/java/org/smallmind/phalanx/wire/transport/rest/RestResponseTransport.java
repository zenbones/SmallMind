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
package org.smallmind.phalanx.wire.transport.rest;

import java.util.concurrent.atomic.AtomicReference;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.web.jersey.aop.Validated;

@Path("/org/smallmind/wire/transport/response")
/**
 * RESTful response transport that accepts invocation signals and returns results synchronously.
 */
public class RestResponseTransport implements ResponseTransport {

  private final AtomicReference<TransportState> stateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  private SignalCodec signalCodec;

  /**
   * Injects the signal codec used to deserialize requests and serialize responses.
   *
   * @param signalCodec codec implementation
   */
  public void setSignalCodec (SignalCodec signalCodec) {

    this.signalCodec = signalCodec;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransportState getState () {

    return stateRef.get();
  }

  /**
   * Resumes request processing (no-op for this implementation).
   */
  @Override
  public void play () {

  }

  /**
   * Pauses request processing (unsupported for this implementation).
   *
   * @throws Exception always thrown as unsupported
   */
  @Override
  public void pause ()
    throws Exception {

    throw new UnsupportedOperationException();
  }

  /**
   * Receives invocation signals over HTTP and returns the resulting {@link ResultSignal} payload.
   *
   * @param callerId         caller identifier supplied via header
   * @param messageId        correlation id supplied via header
   * @param invocationSignal invocation signal body
   * @return HTTP response containing the result signal
   * @throws Throwable if invocation or transport handling fails
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Validated
  public Response invoke (@HeaderParam("X-SMALLMIND-WIRE-CALLER-ID") String callerId, @HeaderParam("X-SMALLMIND-WIRE-MESSAGE-ID") String messageId, InvocationSignal invocationSignal)
    throws Throwable {

    if (TransportState.PLAYING.equals(stateRef.get())) {
      return Instrument.with(RestResponseTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName())).on(
        () -> {

          RestResponseTransmitter responseTransmitter;

          invocationCircuit.handle(responseTransmitter = new RestResponseTransmitter(), signalCodec, callerId, messageId, invocationSignal);

          return responseTransmitter.getResultSignal();
        }
      );
    } else {
      throw new ForbiddenException("The resource has been closed");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close () {

    stateRef.set(TransportState.CLOSED);
  }
}
