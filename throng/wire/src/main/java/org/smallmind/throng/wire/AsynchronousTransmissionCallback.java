package org.smallmind.throng.wire;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.time.Duration;
import org.smallmind.web.jersey.fault.Fault;
import org.smallmind.web.jersey.fault.FaultWrappingException;
import org.smallmind.web.jersey.fault.NativeLanguage;
import org.smallmind.web.jersey.fault.NativeObject;

public class AsynchronousTransmissionCallback implements TransmissionCallback {

  private final CountDownLatch resultLatch = new CountDownLatch(1);
  private final AtomicReference<Duration> timeoutDurationRef = new AtomicReference<>();
  private final AtomicReference<ResultSignal> resultSignalRef = new AtomicReference<>();
  private String serviceName;
  private String functionName;

  public AsynchronousTransmissionCallback (String serviceName, String functionName) {

    this.serviceName = serviceName;
    this.functionName = functionName;
  }

  @Override
  public void destroy (Duration timeoutDuration) {

    timeoutDurationRef.set(timeoutDuration);

    resultLatch.countDown();
  }

  @Override
  public Object getResult (SignalCodec signalCodec)
    throws Throwable {

    ResultSignal resultSignal;

    resultLatch.await();

    if ((resultSignal = resultSignalRef.get()) == null) {

      Duration timeoutDuration = timeoutDurationRef.get();

      throw new TransportTimeoutException("The timeout(%s) milliseconds was exceeded while waiting for a response(%s.%s)", (timeoutDuration == null) ? "unknown" : String.valueOf(timeoutDuration.toMilliseconds()), serviceName, functionName);
    }

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

  public void setResultSignal (ResultSignal resultSignal) {

    resultSignalRef.set(resultSignal);
    resultLatch.countDown();
  }
}