package org.smallmind.cometd.oumuamua.backbone;

import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

public class TransferQueueDeliveryCallback implements DeliveryCallback {

  private final LinkedTransferQueue<byte[]> linkedTransferQueue = new LinkedTransferQueue<>();

  @Override
  public boolean deliver (byte[] data, long timeout, TimeUnit unit)
    throws InterruptedException {

    return linkedTransferQueue.tryTransfer(data, timeout, TimeUnit.SECONDS);
  }

  public byte[] poll (long timeout, TimeUnit unit)
    throws InterruptedException {

    return linkedTransferQueue.poll(timeout, unit);
  }
}
