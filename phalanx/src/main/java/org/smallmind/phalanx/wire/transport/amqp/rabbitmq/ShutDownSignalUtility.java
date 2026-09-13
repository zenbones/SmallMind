package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import com.rabbitmq.client.ShutdownSignalException;

public class ShutDownSignalUtility {

  public static ShutdownSignalException asShutdownSignal (Throwable throwable) {

    Throwable cursor = throwable;

    while (cursor != null) {
      if (cursor instanceof ShutdownSignalException) {

        return (ShutdownSignalException)cursor;
      }
      cursor = cursor.getCause();
    }

    return null;
  }
}
