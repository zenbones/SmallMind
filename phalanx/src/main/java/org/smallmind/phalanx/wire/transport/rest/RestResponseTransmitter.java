package org.smallmind.phalanx.wire.transport.rest;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;

public class RestResponseTransmitter implements ResponseTransmitter {

  private Response.ResponseBuilder responseBuilder;

  public Response getResultSignal () {

    return (responseBuilder != null) ? responseBuilder.build() : Response.noContent().build();
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result) {

    responseBuilder = Response.ok(new ResultSignal(error, nativeType, result), MediaType.APPLICATION_JSON_TYPE).header("X-SMALLMIND-WIRE-CALLER-ID", callerId).header("X-SMALLMIND-WIRE-CORRELATION-ID", correlationId);
  }
}
