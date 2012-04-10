/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.plumber.nio;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.smallmind.quorum.pool.component.PooledComponent;
import org.smallmind.scribe.pen.Logger;

public abstract class SocketChannelWorker implements PooledComponent, Runnable {

  private Logger logger;
  private ServerSocketChannelHerald herald;
  private ServerSocketChannel readyChannel;

  public SocketChannelWorker (Logger logger, ServerSocketChannelHerald herald) {

    this.logger = logger;
    this.herald = herald;
  }

  public void setChannel (ServerSocketChannel readyChannel) {

    this.readyChannel = readyChannel;
  }

  public abstract void socketChannelWork (SocketChannel socketChannel)
    throws Exception;

  public void run () {

    SocketChannel socketChannel;

    try {
      if (readyChannel == null) {
        throw new IllegalArgumentException("No channel has been set on this SocketChannelWorker");
      }

      socketChannel = readyChannel.accept();
      try {
        socketChannelWork(socketChannel);
      }
      finally {
        socketChannel.close();
      }
    }
    catch (Exception e) {
      logger.error(e);
    }
    finally {
      herald.returnConnection(this);
    }
  }

  @Override
  public void terminate () {
  }
}
