package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseReader {

  private StringBuilder responseBuilder = new StringBuilder();
  private boolean complete = false;

  public Response read (ByteBuffer byteBuffer) {

    char singleChar;

    do {
      switch (singleChar = (char)byteBuffer.get()) {
        case '\r':
          if (complete) {
            responseBuilder.append('\r');
          }
          complete = true;
          break;
        case '\n':
          if (!complete) {
            responseBuilder.append('\n');
          } else {
            try {

              Response response = ResponseParser.parse(responseBuilder);

              complete = false;
              responseBuilder = new StringBuilder();

              return response;
            } catch (IOException ioException) {
              LoggerManager.getLogger(EventLoop.class).error(ioException);
            }
          }
          break;
        default:
          responseBuilder.append(singleChar);
      }
    } while (byteBuffer.remaining() > 0);

    return null;
  }
}
