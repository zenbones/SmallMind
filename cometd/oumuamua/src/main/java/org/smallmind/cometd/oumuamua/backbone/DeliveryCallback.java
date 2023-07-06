package org.smallmind.cometd.oumuamua.backbone;

@FunctionalInterface
public interface DeliveryTruck {

  void deliver (byte[] data);
}
