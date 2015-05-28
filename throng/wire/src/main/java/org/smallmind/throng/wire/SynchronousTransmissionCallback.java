package org.smallmind.throng.wire;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.web.jersey.fault.Fault;
import org.smallmind.web.jersey.fault.FaultWrappingException;
import org.smallmind.web.jersey.fault.NativeLanguage;
import org.smallmind.web.jersey.fault.NativeObject;

public class SynchronousTransmissionCallback implements TransmissionCallback {

  private final ResultSignal resultSignal;

  public SynchronousTransmissionCallback (ResultSignal resultSignal) {

    this.resultSignal = resultSignal;
  }

  @Override
  public void destroy (Duration timeoutDuration) {

  }

  @Override
  public synchronized Object getResult (SignalCodec signalCodec)
    throws Throwable {

    if (resultSignal.isError()) {

      Fault fault = signalCodec.extractObject(resultSignal.getResult(), Fault.class);
      NativeObject nativeObject;

      if (((nativeObject = fault.getNativeObject()) != null) && nativeObject.getLanguage().equals(NativeLanguage.JAVA)) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(nativeObject.getBytes()); ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
          throw (Throwable)objectInputStream.readObject();
        }
      }

      throw new FaultWrappingException(fault);
    }

    return signalCodec.extractObject(resultSignal.getResult(), TypeUtility.nativeDecode(resultSignal.getNativeType()));
  }
}