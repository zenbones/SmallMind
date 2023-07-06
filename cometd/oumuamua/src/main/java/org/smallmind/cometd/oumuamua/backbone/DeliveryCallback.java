package org.smallmind.cometd.oumuamua.backbone;

import java.util.concurrent.TimeUnit;

@FunctionalInterface
public interface DeliveryCallback {

  boolean deliver (byte[] data, long timeout, TimeUnit unit)
    throws InterruptedException;
}
