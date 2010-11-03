package org.smallmind.quorum.pool.event;

import java.util.EventListener;

public interface ConnectionInstanceEventListener extends EventListener {

   public abstract void connectionErrorOccurred (ConnectionInstanceEvent event);
}

