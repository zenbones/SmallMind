package org.smallmind.plumber.http;

import org.smallmind.plumber.io.ServerSocketHerald;
import org.smallmind.plumber.io.SocketWorker;
import org.smallmind.plumber.io.SocketWorkerFactory;
import org.smallmind.scribe.pen.Logger;

public class RecordingSocketWorkerFactory implements SocketWorkerFactory {

   String httpHost;
   int httpPort;
   int bufferSize;

   public RecordingSocketWorkerFactory (String httpHost, int httpPort, int bufferSize) {

      this.httpHost = httpHost;
      this.httpPort = httpPort;
      this.bufferSize = bufferSize;
   }

   public SocketWorker createWorker (Logger logger, ServerSocketHerald herald)
      throws Exception {

      return new RecordingSocketWorker(logger, herald, httpHost, httpPort, bufferSize);
   }

}
