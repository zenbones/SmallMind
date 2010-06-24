package org.smallmind.plumber.nio;

import org.smallmind.scribe.pen.Logger;

public interface SocketChannelWorkerFactory {

   public SocketChannelWorker createWorker (Logger logger, ServerSocketChannelHerald herald)
      throws Exception;

}
