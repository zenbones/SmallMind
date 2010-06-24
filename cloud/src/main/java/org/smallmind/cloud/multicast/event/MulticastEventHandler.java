package org.smallmind.cloud.multicast.event;

public interface MulticastEventHandler {

   public abstract void deliverEvent (MulticastEvent multicastEvent);

}
