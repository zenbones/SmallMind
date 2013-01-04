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
package org.smallmind.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.security.EncryptionUtilities;
import org.smallmind.nutsnbolts.security.HashAlgorithm;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;

public abstract class Websocket {

  private final Socket socket;
  private final MessageWorker messageWorker;
  private final ConcurrentLinkedQueue<String> pingKeyQueue = new ConcurrentLinkedQueue<>();
  private final AtomicReference<ReadyState> readyStateRef = new AtomicReference<>(ReadyState.CONNECTING);
  private final String url;
  private final byte[] rawBuffer = new byte[1024];

  public Websocket (URI uri, String... protocols)
    throws IOException, NoSuchAlgorithmException, WebsocketException {

    Thread workerThread;
    byte[] keyBytes = new byte[16];

    ThreadLocalRandom.current().nextBytes(keyBytes);

    if (!uri.isAbsolute()) {
      throw new SyntaxException("A websocket uri must be absolute");
    }
    if ((uri.getScheme() == null) || (!(uri.getScheme().equals("ws") || uri.getScheme().equals("wss")))) {
      throw new SyntaxException("A websocket requires a uri with either the 'ws' or 'wss' scheme");
    }
    if ((uri.getFragment() != null) && (uri.getFragment().length() > 0)) {
      throw new SyntaxException("A websocket uri may not contain a fragment");
    }

    if (!ProtocolValidator.validate(protocols)) {
      throw new SyntaxException("The provided protocols(%s) are not valid", Arrays.toString(protocols));
    }

    url = uri.toString();

    socket = new Socket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : uri.getScheme().equals("ws") ? 80 : 443);
    socket.setTcpNoDelay(true);

    // initial handshake request
    socket.getOutputStream().write(Handshake.constructRequest(uri, keyBytes, protocols));
    Handshake.validateResponse(new String(readData()), keyBytes, protocols);
    readyStateRef.set(ReadyState.OPEN);

    workerThread = new Thread(messageWorker = new MessageWorker());
    workerThread.setDaemon(true);
    workerThread.start();
  }

  public abstract void onError (Exception exception);

  public abstract void onPong (byte[] message);

  public abstract void onText (String message);

  public abstract void onBinary (byte[] message);

  public void ping (byte[] buffer)
    throws IOException, WebsocketException {

    //TODO: closed check

    try {
      pingKeyQueue.add(Base64.encode(EncryptionUtilities.hash(HashAlgorithm.SHA_1, buffer)));
      write(Frame.ping(buffer));
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new WebsocketException(noSuchAlgorithmException);
    }
  }

  public void text (String message)
    throws IOException {

    //TODO: closed check
    write(Frame.text(message));
  }

  public void binary (byte[] buffer)
    throws IOException {

    //TODO: closed check
    write(Frame.binary(buffer));
  }

  public void close () {

    // TODO:
  }

  private void write (byte[] buffer)
    throws IOException {

    socket.getOutputStream().write(buffer);
  }

  private byte[] readData ()
    throws IOException, WebsocketException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // TODO: Should NOT need to do this
    socket.getOutputStream().write(Frame.pong(new byte[0]));
    do {

      int bytesRead;

      bytesRead = socket.getInputStream().read(rawBuffer);
      outputStream.write(rawBuffer, 0, bytesRead);
    } while (socket.getInputStream().available() > 0);
    outputStream.close();

    return outputStream.toByteArray();
  }

  public String url () {

    return url;
  }

  public ReadyState getReadyState () {

    return readyStateRef.get();
  }

  public int readyState () {

    return readyStateRef.get().ordinal();
  }

  public String extensions () {

    return "";
  }

  private class MessageWorker implements Runnable {

    private CountDownLatch exitLatch = new CountDownLatch(1);
    private AtomicBoolean aborted = new AtomicBoolean(false);
    private LinkedList<Data> dataList = new LinkedList<>();

    public void abort ()
      throws InterruptedException {

      if (aborted.compareAndSet(false, true)) {
      }

      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!aborted.get()) {
          try {

            Data data;

            if ((data = Frame.decode(readData())).isFinal()) {
              switch (data.getOpCode()) {
                case CONTINUATION:
                  if (dataList.isEmpty()) {
                    throw new WebsocketException("No continuation exists to terminate");
                  }

                  try {
                    ByteArrayOutputStream fragmentStream = new ByteArrayOutputStream();

                    for (Data fragmentedData : dataList) {
                      fragmentStream.write(fragmentedData.getMessage());
                    }
                    fragmentStream.write(data.getMessage());
                    fragmentStream.close();

                    switch (dataList.getFirst().getOpCode()) {
                      case TEXT:
                        onText(new String(fragmentStream.toByteArray()));
                        break;
                      case BINARY:
                        onBinary(fragmentStream.toByteArray());
                        break;
                      default:
                        throw new WebsocketException("The current continuation starts with an illegal op code(%s)", dataList.getFirst().getOpCode().name());
                    }
                  }
                  finally {
                    dataList.clear();
                  }
                  break;
                case TEXT:
                  if (!dataList.isEmpty()) {
                    dataList.clear();
                    throw new WebsocketException("Expecting the final frame of a continuation");
                  }

                  onText(new String(data.getMessage()));
                  break;
                case BINARY:
                  if (!dataList.isEmpty()) {
                    dataList.clear();
                    throw new WebsocketException("Expecting the final frame of a continuation");
                  }

                  onBinary(data.getMessage());
                  break;
                case CLOSE:
                  // TODO:
                  break;
                case PING:
                  socket.getOutputStream().write(Frame.pong(data.getMessage()));
                  break;
                case PONG:

                  Iterator<String> pingKeyIter = pingKeyQueue.iterator();
                  String pongKey = Base64.encode(EncryptionUtilities.hash(HashAlgorithm.SHA_1, data.getMessage()));

                  while (pingKeyIter.hasNext()) {

                    String pingKey = pingKeyIter.next();

                    pingKeyIter.remove();
                    if (pongKey.equals(pingKey)) {
                      onPong(data.getMessage());
                      break;
                    }
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(data.getOpCode().name());
              }
            }
            else {
              if (!(data.getOpCode().equals(OpCode.CONTINUATION) || data.getOpCode().equals(OpCode.TEXT) || data.getOpCode().equals(OpCode.BINARY))) {
                throw new WebsocketException("All control frames must be marked as final");
              }
              if ((data.getOpCode().equals(OpCode.TEXT) || data.getOpCode().equals(OpCode.BINARY)) && (!dataList.isEmpty())) {
                dataList.clear();
                throw new WebsocketException("Starting a new continuation before the previous continuation has terminated");
              }
              if (data.getOpCode().equals(OpCode.CONTINUATION) && dataList.isEmpty()) {
                throw new WebsocketException("The first frame of a continuation must have an op code != 0");
              }

              dataList.add(data);
            }
          }
          catch (Exception exception) {
            onError(exception);
          }
        }
      }
      finally {
        exitLatch.countDown();
      }
    }
  }
}
