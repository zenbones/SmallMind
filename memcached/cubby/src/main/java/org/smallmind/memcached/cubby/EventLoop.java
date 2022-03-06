package org.smallmind.memcached.cubby;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventLoop implements Runnable {

  private final CountDownLatch terminationLatch = new CountDownLatch(1);
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final LinkedBlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();
  private final SocketChannel socketChannel;
  private final Selector selector;
  private final SelectionKey selectionKey;

  public void stop ()
    throws InterruptedException {

    finished.set(true);
    terminationLatch.await();
  }

  public void send (Command command) {

    synchronized (commandQueue) {
      commandQueue.offer(command);
      selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
      selector.wakeup();
    }
  }

  @Override
  public void run () {

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
    byte[] commandBuffer = null;
    int writeIndex = 0;

    try {
      while (!finished.get()) {
        try {
          if (selector.select(1000) > 0) {

            Iterator<SelectionKey> selectionKeyIter = selector.selectedKeys().iterator();

            while (selectionKeyIter.hasNext()) {

              SelectionKey selectionKey = selectionKeyIter.next();

              try {
                if (selectionKey.isValid()) {
                  if (selectionKey.isReadable() && selectionKey.channel().isOpen()) {
                    try {

                      StringBuilder lineBuilder = new StringBuilder();
                      boolean data = true;

                      byteBuffer.clear();
                      if (((SocketChannel)selectionKey.channel()).read(byteBuffer) > 0) {

                        char singleChar;

                        byteBuffer.flip();
                        do {
                          switch (singleChar = (char)byteBuffer.getInt()) {
                            case '\r':
                              if (!data) {
                                lineBuilder.append('\r');
                              }
                              data = false;
                              break;
                            case '\n':
                              if (data) {
                                lineBuilder.append('\n');
                              } else {
                                System.out.println(lineBuilder);
                                lineBuilder = new StringBuilder();
                                data = true;
                              }
                              break;
                            default:
                              lineBuilder.append(singleChar);
                          }
                        } while (byteBuffer.remaining() > 0);
                      }
                    } catch (IOException ioException) {
                      System.out.println("close");
                      // closeSelectionKey(selectionKey);
                    }
                  }
                  if (selectionKey.isWritable() && selectionKey.channel().isOpen()) {

                    if (commandBuffer == null) {
                      synchronized (commandQueue) {

                        Command command;

                        if ((command = commandQueue.poll()) == null) {
                          selectionKey.interestOps(SelectionKey.OP_READ);
                        } else {
                          commandBuffer = command.toString().getBytes();
                        }
                      }
                    }

                    if (commandBuffer != null) {
                      byteBuffer.clear();
                      try {
                        byteBuffer.put(commandBuffer, writeIndex, Math.min(byteBuffer.remaining(), commandBuffer.length - writeIndex));

                        byteBuffer.flip();
                        writeIndex += socketChannel.write(byteBuffer);

                        if (writeIndex == commandBuffer.length) {
                         commandBuffer = null;
                         writeIndex = 0;
                        }
                      } catch (IOException ioException) {
                        System.out.println("close");
                        // closeSelectionKey(selectionKey);
                      }
                    }
                  }
                } else {
                  System.out.println("close");
                  // closeSelectionKey(selectionKey);
                }
              } catch (CancelledKeyException cancelledKeyException) {
                System.out.println("close");
                // closeSelectionKey(selectionKey);
              } finally {
                selectionKeyIter.remove();
              }
            }
          }
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      }
    } finally {
      terminationLatch.countDown();
    }
  }
}
