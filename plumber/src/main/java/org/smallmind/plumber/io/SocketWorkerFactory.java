package org.smallmind.plumber.io;

import org.smallmind.scribe.pen.Logger;

public interface SocketWorkerFactory {

   public SocketWorker createWorker (Logger logger, ServerSocketHerald herald)
      throws Exception;

}
