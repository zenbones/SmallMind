/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.web.jersey.aop.Validated;

@Path("/org/smallmind/wire/transport/response")
public class RestResponseTransport implements ResponseTransport {

  private final AtomicReference<TransportState> stateRef = new AtomicReference<>(TransportState.PLAYING);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  private SignalCodec signalCodec;

  public void setSignalCodec (SignalCodec signalCodec) {

    this.signalCodec = signalCodec;
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public TransportState getState () {

    return stateRef.get();
  }

  @Override
  public void play () {

  }

  @Override
  public void pause ()
    throws Exception {

    throw new UnsupportedOperationException();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Validated
  public Response invoke (@HeaderParam("X-SMALLMIND-WIRE-CALLER-ID") String callerId, @HeaderParam("X-SMALLMIND-WIRE-MESSAGE-ID") String messageId, InvocationSignal invocationSignal)
    throws Throwable {

    if (TransportState.PLAYING.equals(stateRef.get())) {
      return Instrument.with(RestResponseTransport.class, SpeedometerBuilder.instance(), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName())).on(
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

  @Override
  public void close () {

    stateRef.set(TransportState.CLOSED);
  }
}
