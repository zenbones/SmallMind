package org.smallmind.memcached.cubby;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class CubbyConnection {

  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
  private CountDownLatch terminationLatch = new CountDownLatch(1);
  private AtomicBoolean finished = new AtomicBoolean(false);
  private ServerSocketChannel socketChannel;
  private Selector selector;

  public CubbyConnection () {

    SelectionKey selectionKey;
    Thread eventThread;

    socketChannel = SocketChannel.open();
    socketChannel.configureBlocking(false);
    socketChannel.socket().bind(new InetSocketAddress(hostName, port));

    selectionKey = socketChannel.register(selector = Selector.open(), SelectionKey.OP_WRITE);

    eventThread = new Thread(eventLoop = new EventLoop());
    eventThread.setDaemon(true);
    eventThread.start();
  }
}
