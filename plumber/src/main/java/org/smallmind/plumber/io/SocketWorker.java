/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.plumber.io;

import java.io.IOException;
import java.net.Socket;
import org.smallmind.quorum.pool.simple.PooledComponent;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class SocketWorker implements PooledComponent, Runnable {

  private Logger logger;
  private ServerSocketHerald herald;
  private Socket socket;

  public SocketWorker (Logger logger, ServerSocketHerald herald) {

    this.logger = logger;
    this.herald = herald;
  }

  public void setSocket (Socket socket) {

    this.socket = socket;
  }

  public abstract void socketWork (Socket socket)
    throws Exception;

  public void run () {

    try {
      if (socket == null) {
        throw new IllegalArgumentException("No socket has been set on this SocketWorker");
      }
      if (socket.isClosed()) {
        throw new IllegalArgumentException("The socket has already been closed");
      }

      try {
        socketWork(socket);
      }
      finally {
        socket.close();
      }
    }
    catch (Exception e) {
      logger.error(e);
    }
    finally {
      herald.returnConnection(this);
    }
  }

  public void close ()
    throws IOException {

    socket.close();
  }

  @Override
  public void terminate () {

    try {
      close();
    }
    catch (IOException ioException) {
      LoggerManager.getLogger(SocketWorker.class).error(ioException);
    }
  }
}
