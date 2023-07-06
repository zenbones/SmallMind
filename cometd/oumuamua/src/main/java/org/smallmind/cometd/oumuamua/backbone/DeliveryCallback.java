package org.smallmind.cometd.oumuamua.backbone;

@FunctionalInterface
public interface DeliveryCallback {

  void deliver (byte[] data);
}
