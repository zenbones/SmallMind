/*
 * Copyright (c) 2007 through 2026 David Berkman
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
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.pen.fluentbit;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.scribe.pen.AbstractAppender;
import org.smallmind.scribe.pen.DateFormatTimestamp;
import org.smallmind.scribe.pen.ErrorHandler;
import org.smallmind.scribe.pen.Logger;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.Record;
import org.smallmind.scribe.pen.RecordElement;
import org.smallmind.scribe.pen.Timestamp;
import org.springframework.beans.factory.InitializingBean;

// TODO: The underlying org.msgpack.jackson.dataformat will be dependent on jackson 2.x for the foreseeable future

/**
 * Scribe appender that collects log records from a {@link SynchronousQueue}, batches them up to a configurable
 * size or grace period, serializes each batch to MessagePack via Jackson, and sends it to a Fluent Bit TCP
 * input; worker threads are started by {@link #afterPropertiesSet()} and run until {@link #close()} is called.
 */
public class FluentBitAppender extends AbstractAppender implements InitializingBean {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
  private final SynchronousQueue<org.smallmind.scribe.pen.Record<?>> recordQueue = new SynchronousQueue<>();
  private CountDownLatch finishedLatch;
  private MessagePackFormatter formatter;
  private Map<String, String> additionalEventData;
  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private RecordElement[] recordElements = RecordElement.values();
  private String newLine = System.lineSeparator();
  private String host;
  private long batchGracePeriodMilliseconds = 3000;
  private int port;
  private int retryAttempts = 3;
  private int concurrencyLimit = 1;
  private int batchSize = 1;

  /**
   * Constructs a {@code FluentBitAppender} with the given name and no error handler; appender errors will be
   * silently discarded unless an error handler is supplied later or via the two-argument constructor.
   *
   * @param name the name that identifies this appender and is used as the Fluent Bit stream/tag name
   */
  public FluentBitAppender (String name) {

    this(name, null);
  }

  /**
   * Constructs a {@code FluentBitAppender} with the given name and error handler; the error handler receives
   * records when TCP delivery fails after all retry attempts.
   *
   * @param name         the name that identifies this appender and is used as the Fluent Bit stream/tag name
   * @param errorHandler the handler invoked on delivery failures; may be {@code null}
   */
  public FluentBitAppender (String name, ErrorHandler errorHandler) {

    super(name, errorHandler);
  }

  /**
   * Sets the host name or IP address of the Fluent Bit TCP input that worker threads connect to.
   *
   * @param host the target host name or address
   */
  public void setHost (String host) {

    this.host = host;
  }

  /**
   * Sets the TCP port number on which the Fluent Bit input is listening.
   *
   * @param port the target port number
   */
  public void setPort (int port) {

    this.port = port;
  }

  /**
   * Overrides the timestamp provider used when serializing events; defaults to
   * {@link DateFormatTimestamp#getDefaultInstance()}.
   *
   * @param timestamp the timestamp implementation to embed in each serialized event
   */
  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  /**
   * Specifies which {@link RecordElement} fields are included when serializing each log record; defaults to
   * all available elements.
   *
   * @param recordElements the record fields to include in each serialized event
   */
  public void setRecordElements (RecordElement[] recordElements) {

    this.recordElements = recordElements;
  }

  /**
   * Overrides the line separator used when rendering multi-line stack traces in serialized events; defaults
   * to {@link System#lineSeparator()}.
   *
   * @param newLine the newline text to insert between stack trace lines
   */
  public void setNewLine (String newLine) {

    this.newLine = newLine;
  }

  /**
   * Sets a map of static key/value pairs merged into the record node of every event forwarded to Fluent Bit,
   * for example pod name, namespace, or deployment environment.
   *
   * @param additionalEventData the map of static fields to add to every event; may be {@code null}
   */
  public void setAdditionalEventData (Map<String, String> additionalEventData) {

    this.additionalEventData = additionalEventData;
  }

  /**
   * Sets the number of times a worker will attempt to reconnect and resend a batch before delegating
   * the failure to the error handler; the effective minimum is 1.
   *
   * @param retryAttempts the maximum number of send attempts per batch
   */
  public void setRetryAttempts (int retryAttempts) {

    this.retryAttempts = Math.max(1, retryAttempts);
  }

  /**
   * Sets the number of worker threads started by {@link #afterPropertiesSet()} to drain the record queue
   * and send batches; the effective minimum is 1.
   *
   * @param concurrencyLimit the number of concurrent sender threads
   */
  public void setConcurrencyLimit (int concurrencyLimit) {

    this.concurrencyLimit = Math.max(1, concurrencyLimit);
  }

  /**
   * Sets the maximum number of records accumulated into a single batch before it is transmitted to Fluent Bit;
   * a batch is also flushed early when the grace period elapses. The effective minimum is 1.
   *
   * @param batchSize the target number of records per batch
   */
  public void setBatchSize (int batchSize) {

    this.batchSize = Math.max(1, batchSize);
  }

  /**
   * Sets the maximum number of milliseconds a partially filled batch waits for additional records before
   * being transmitted regardless of batch size; the effective minimum is 1000 ms.
   *
   * @param batchGracePeriodMilliseconds the flush deadline in milliseconds for partially filled batches
   */
  public void setBatchGracePeriodMilliseconds (long batchGracePeriodMilliseconds) {

    this.batchGracePeriodMilliseconds = Math.max(1000L, batchGracePeriodMilliseconds);
  }

  /**
   * Initializes the {@link MessagePackFormatter} from the configured timestamp, record elements, and newline
   * string, creates the finish latch, and starts the configured number of {@code FluentBitWorker} threads;
   * must be called before any records are submitted.
   */
  @Override
  public void afterPropertiesSet () {

    formatter = new MessagePackFormatter(timestamp, recordElements, newLine);
    finishedLatch = new CountDownLatch(concurrencyLimit);

    for (int index = 0; index < concurrencyLimit; index++) {
      new Thread(new FluentBitWorker(finishedLatch)).start();
    }
  }

  /**
   * Places the record onto the internal {@link SynchronousQueue} for pickup by a worker thread; blocks until
   * a worker accepts the record.
   *
   * @param record the log record to deliver to Fluent Bit
   * @throws LoggerException      if this appender has already been closed
   * @throws InterruptedException if the calling thread is interrupted while waiting for a worker to accept the record
   */
  @Override
  public synchronized void handleOutput (org.smallmind.scribe.pen.Record<?> record)
    throws LoggerException, InterruptedException {

    if (closed.get()) {
      throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
    } else {
      recordQueue.put(record);
    }
  }

  /**
   * Marks this appender as closed so that no further records are accepted, then waits for all worker threads
   * to finish draining the queue and transmitting any pending batches; idempotent on repeated calls.
   *
   * @throws LoggerException if the calling thread is interrupted while waiting for workers to finish
   */
  @Override
  public synchronized void close ()
    throws LoggerException {

    if (closed.compareAndSet(false, true)) {
      try {
        finishedLatch.await();
      } catch (InterruptedException interruptedException) {
        throw new LoggerException(interruptedException);
      }
    }
  }

  private class FluentBitWorker implements Runnable {

    private final CountDownLatch finishedLatch;
    private Socket socket;
    private ArrayNode entriesNode = JsonNodeFactory.instance.arrayNode(batchSize);

    /**
     * Constructs a worker that will count down the given latch when it finishes, allowing
     * {@link FluentBitAppender#close()} to detect that all workers have exited.
     *
     * @param finishedLatch the latch to decrement when this worker's run loop exits
     */
    public FluentBitWorker (CountDownLatch finishedLatch) {

      this.finishedLatch = finishedLatch;
    }

    /**
     * Polls the record queue in a loop, builds batches up to the configured size or grace period, sends each
     * completed batch to Fluent Bit, and exits cleanly when the appender is closed, flushing any remaining
     * records and decrementing the finish latch.
     */
    @Override
    public void run () {

      try {

        long lastSentMillis = System.currentTimeMillis();

        while (!closed.get()) {
          try {

            Record<?> record;

            if ((record = recordQueue.poll(1, TimeUnit.SECONDS)) != null) {

              ArrayNode entryNode = JsonNodeFactory.instance.arrayNode();
              ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
              ObjectNode recordNode = formatter.format(record);

              if ((additionalEventData != null) && (!additionalEventData.isEmpty())) {
                for (Map.Entry<String, String> additionalDataEntry : additionalEventData.entrySet()) {
                  recordNode.put(additionalDataEntry.getKey(), additionalDataEntry.getValue());
                }
              }

              messageNode.set("message", recordNode);
              entryNode.add(record.getMillis() / 1000);
              entryNode.add(messageNode);

              if ((entriesNode.add(entryNode).size() >= batchSize) || ((System.currentTimeMillis()) - lastSentMillis >= batchGracePeriodMilliseconds)) {
                lastSentMillis = System.currentTimeMillis();
                send();
              }
            } else if ((!entriesNode.isEmpty()) && ((System.currentTimeMillis() - lastSentMillis) >= batchGracePeriodMilliseconds)) {
              lastSentMillis = System.currentTimeMillis();
              send();
            }
          } catch (InterruptedException interruptedException) {
            handleError(Logger.unknown(), interruptedException);
          }
        }

        try {
          if (socket != null) {
            socket.close();
          }
        } catch (IOException ioException) {
          handleError(Logger.unknown(), ioException);
        }
      } catch (Throwable throwable) {
        // Just in case, we should know something bad has happened
        handleError(Logger.unknown(), throwable);
      } finally {
        finishedLatch.countDown();
      }
    }

    /**
     * Sends the current batch to Fluent Bit, reconnecting as needed.
     */
    private void send () {

      try {
        ArrayNode eventNode = JsonNodeFactory.instance.arrayNode();
        ObjectNode optionNode = JsonNodeFactory.instance.objectNode();
        byte[] chunk = new byte[16];
        int retry = 0;

        ThreadLocalRandom.current().nextBytes(chunk);
        optionNode.put("chunk", Base64Codec.encode(chunk));
        optionNode.put("size", batchSize);

        eventNode.add(getName());
        eventNode.add(entriesNode);
        eventNode.add(optionNode);

        do {
          try {
            if (socket == null) {
              connect();
            } else if (socket.isClosed() || (!socket.isConnected())) {
              socket.close();
              connect();
            }

            socket.getOutputStream().write(objectMapper.writeValueAsBytes(eventNode));
          } catch (IOException ioException) {
            socket = null;
            if (++retry > retryAttempts) {
              throw new FluentBitConnectionException(ioException, "Failed to connect to host(%s:%d)", host, port);
            }
          }
        } while (socket == null);
      } catch (IOException ioException) {
        handleError(Logger.unknown(), ioException);
      } finally {
        entriesNode = JsonNodeFactory.instance.arrayNode(batchSize);
      }
    }

    /**
     * Opens a TCP connection to the configured Fluent Bit host.
     *
     * @throws IOException if the socket cannot be created
     */
    private void connect ()
      throws IOException {

      socket = new Socket(host, port);
      socket.setTcpNoDelay(true);
      socket.setSoTimeout(1000);
    }
  }
}
