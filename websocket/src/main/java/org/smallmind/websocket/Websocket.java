/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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
import java.net.SocketTimeoutException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSocketFactory;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.security.EncryptionUtilities;
import org.smallmind.nutsnbolts.security.HashAlgorithm;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;

public abstract class Websocket implements AutoCloseable {

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

    if (uri.getScheme().equals("wss")) {
      socket = SSLSocketFactory.getDefault().createSocket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : 443);
    }
    else {
      socket = new Socket(uri.getHost().toLowerCase(), (uri.getPort() != -1) ? uri.getPort() : 80);
    }
    socket.setTcpNoDelay(true);
    socket.setSoTimeout(1000);

    // initial handshake request
    socket.getOutputStream().write(Handshake.constructRequest(uri, keyBytes, protocols));
    Handshake.validateResponse(new String(read()), keyBytes, protocols);
    readyStateRef.set(ReadyState.OPEN);

    workerThread = new Thread(messageWorker = new MessageWorker());
    workerThread.setDaemon(true);
    workerThread.start();
  }

  public abstract void onError (Exception exception);

  public abstract void onPong (byte[] message);

  public abstract void onText (String message);

  public abstract void onBinary (byte[] message);

  public synchronized void ping (byte[] buffer)
    throws IOException, WebsocketException {

    if (readyStateRef.get().equals(ReadyState.CLOSING) || readyStateRef.get().equals(ReadyState.CLOSED)) {
      throw new WebsocketException("The websocket has been closed");
    }

    try {
      pingKeyQueue.add(Base64Codec.encode(EncryptionUtilities.hash(HashAlgorithm.SHA_1, buffer)));
      write(Frame.ping(buffer));
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new WebsocketException(noSuchAlgorithmException);
    }
  }

  public synchronized void text (String message)
    throws IOException, WebsocketException {

    if (readyStateRef.get().equals(ReadyState.CLOSING) || readyStateRef.get().equals(ReadyState.CLOSED)) {
      throw new WebsocketException("The websocket has been closed");
    }

    write(Frame.text(message));
  }

  public synchronized void binary (byte[] buffer)
    throws IOException, WebsocketException {

    if (readyStateRef.get().equals(ReadyState.CLOSING) || readyStateRef.get().equals(ReadyState.CLOSED)) {
      throw new WebsocketException("The websocket has been closed");
    }

    write(Frame.binary(buffer));
  }

  @Override
  public void close ()
    throws IOException, WebsocketException, InterruptedException {

    close(CloseCode.NORMAL);
  }

  public void close (CloseCode closeCode)
    throws IOException, WebsocketException, InterruptedException {

    close(closeCode, null);
  }

  public void close (CloseCode closeCode, String reason)
    throws IOException, WebsocketException, InterruptedException {

    close(closeCode.getCodeAsBytes(), reason);
  }

  private void close (byte[] status, String reason)
    throws IOException, WebsocketException, InterruptedException {

    if (readyStateRef.compareAndSet(ReadyState.OPEN, ReadyState.CLOSING)) {
      try {
        messageWorker.abort();
        write(Frame.close(status, reason));
      }
      finally {
        readyStateRef.set(ReadyState.CLOSED);
      }
    }
  }

  private void write (byte[] buffer)
    throws IOException {

    socket.getOutputStream().write(buffer);
  }

  private byte[] read ()
    throws IOException, WebsocketException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    boolean complete = false;

    // TODO: Should NOT need to do this
    socket.getOutputStream().write(Frame.pong(new byte[0]));

    do {
      do {

        int bytesRead;

        bytesRead = socket.getInputStream().read(rawBuffer);
        outputStream.write(rawBuffer, 0, bytesRead);
      } while (socket.getInputStream().available() > 0);

      if (readyStateRef.get().equals(ReadyState.CONNECTING)) {
        complete = true;
      }
      else {
        if (outputStream.size() >= 2) {

          byte length = (byte)(outputStream.toByteArray()[1] & 0x7F);

          if (length < 126) {
            complete = outputStream.size() == length + 2;
          }
          else if ((length == 126) && (outputStream.size() >= 4)) {

            byte[] currentArray = outputStream.toByteArray();

            complete = outputStream.size() == ((currentArray[2] & 0xFF) << 8) + (currentArray[3] & 0xFF) + 4;
          }
          else if (outputStream.size() >= 10) {

            byte[] currentArray = outputStream.toByteArray();

            complete = outputStream.size() == ((currentArray[6] & 0xFF) << 24) + ((currentArray[7] & 0xFF) << 16) + ((currentArray[8] & 0xFF) << 8) + (currentArray[9] & 0xFF) + 10;
          }
        }
      }
    } while (!complete);

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
    private LinkedList<Fragment> fragmentList = new LinkedList<>();

    public void abort ()
      throws InterruptedException {

      if (aborted.compareAndSet(false, true)) {
        // do nothing for now... maybe with a proper event based implementation
      }

      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!aborted.get()) {
          try {

            Fragment fragment;

            if ((fragment = Frame.decode(read())).isFinal()) {
              switch (fragment.getOpCode()) {
                case CONTINUATION:
                  if (fragmentList.isEmpty()) {
                    throw new WebsocketException("No continuation exists to terminate");
                  }

                  try {
                    ByteArrayOutputStream fragmentStream = new ByteArrayOutputStream();

                    for (Fragment storedFragment : fragmentList) {
                      fragmentStream.write(storedFragment.getMessage());
                    }
                    fragmentStream.write(fragment.getMessage());
                    fragmentStream.close();

                    switch (fragmentList.getFirst().getOpCode()) {
                      case TEXT:
                        onText(new String(fragmentStream.toByteArray()));
                        break;
                      case BINARY:
                        onBinary(fragmentStream.toByteArray());
                        break;
                      default:
                        throw new WebsocketException("The current continuation starts with an illegal op code(%s)", fragmentList.getFirst().getOpCode().name());
                    }
                  }
                  finally {
                    fragmentList.clear();
                  }
                  break;
                case TEXT:
                  if (!fragmentList.isEmpty()) {
                    fragmentList.clear();
                    throw new WebsocketException("Expecting the final frame of a continuation");
                  }

                  onText(new String(fragment.getMessage()));
                  break;
                case BINARY:
                  if (!fragmentList.isEmpty()) {
                    fragmentList.clear();
                    throw new WebsocketException("Expecting the final frame of a continuation");
                  }

                  onBinary(fragment.getMessage());
                  break;
                case CLOSE:

                  byte[] status;

                  if (fragment.getMessage().length < 2) {
                    status = CloseCode.SERVER_ERROR.getCodeAsBytes();
                  }
                  else {
                    status = new byte[2];
                    System.arraycopy(fragment.getMessage(), 0, status, 0, 2);
                  }

                  close(status, null);
                  break;
                case PING:
                  socket.getOutputStream().write(Frame.pong(fragment.getMessage()));
                  break;
                case PONG:

                  Iterator<String> pingKeyIter = pingKeyQueue.iterator();
                  String pongKey = Base64Codec.encode(EncryptionUtilities.hash(HashAlgorithm.SHA_1, fragment.getMessage()));

                  while (pingKeyIter.hasNext()) {

                    String pingKey = pingKeyIter.next();

                    pingKeyIter.remove();
                    if (pongKey.equals(pingKey)) {
                      onPong(fragment.getMessage());
                      break;
                    }
                  }
                  break;
                default:
                  throw new UnknownSwitchCaseException(fragment.getOpCode().name());
              }
            }
            else {
              if (!(fragment.getOpCode().equals(OpCode.CONTINUATION) || fragment.getOpCode().equals(OpCode.TEXT) || fragment.getOpCode().equals(OpCode.BINARY))) {
                throw new WebsocketException("All control frames must be marked as final");
              }
              if ((fragment.getOpCode().equals(OpCode.TEXT) || fragment.getOpCode().equals(OpCode.BINARY)) && (!fragmentList.isEmpty())) {
                fragmentList.clear();
                throw new WebsocketException("Starting a new continuation before the previous continuation has terminated");
              }
              if (fragment.getOpCode().equals(OpCode.CONTINUATION) && fragmentList.isEmpty()) {
                throw new WebsocketException("The first frame of a continuation must have an op code != 0");
              }

              fragmentList.add(fragment);
            }
          }
          catch (SocketTimeoutException socketTimeoutException) {
            // TODO: Do something here
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
