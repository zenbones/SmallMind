package org.smallmind.phalanx.wire.transport.rest;

import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
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
  public String register (Class<?> serviceInterface, WiredService targetService) throws Exception {

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
  public void pause () throws Exception {

    throw new UnsupportedOperationException();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Validated
  public Response invoke (@HeaderParam("X-SMALLMIND-WIRE-CALLER-ID") String callerId, @HeaderParam("X-SMALLMIND-WIRE-MESSAGE-ID") String messageId, InvocationSignal invocationSignal)
    throws Throwable {

    if (TransportState.PLAYING.equals(stateRef.get())) {
      return Instrument.with(RestResponseTransport.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName())).on(
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
