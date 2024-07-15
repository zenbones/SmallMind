/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.scribe.pen;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.springframework.beans.factory.InitializingBean;

public class FluentBitAppender extends AbstractAppender implements InitializingBean {

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
  private MessagePackFormatter formatter;
  private Socket socket;
  private Map<String, String> additionalEventData;
  private Timestamp timestamp = DateFormatTimestamp.getDefaultInstance();
  private RecordElement[] recordElements = RecordElement.values();
  private ArrayNode entriesNode;
  private String newLine = System.getProperty("line.separator");
  private String host;
  private int port;
  private int retryAttempts = 3;
  private int batch = 1;

  public FluentBitAppender (String name) {

    this(name, null);
  }

  public FluentBitAppender (String name, ErrorHandler errorHandler) {

    super(name, errorHandler);
  }

  public void setHost (String host) {

    this.host = host;
  }

  public void setPort (int port) {

    this.port = port;
  }

  public void setTimestamp (Timestamp timestamp) {

    this.timestamp = timestamp;
  }

  public void setRecordElements (RecordElement[] recordElements) {

    this.recordElements = recordElements;
  }

  public void setNewLine (String newLine) {

    this.newLine = newLine;
  }

  public void setAdditionalEventData (Map<String, String> additionalEventData) {

    this.additionalEventData = additionalEventData;
  }

  public void setRetryAttempts (int retryAttempts) {

    this.retryAttempts = retryAttempts;
  }

  public void setBatch (int batch) {

    this.batch = batch;
  }

  @Override
  public void afterPropertiesSet () {

    formatter = new MessagePackFormatter(timestamp, recordElements, newLine);
    entriesNode = JsonNodeFactory.instance.arrayNode(batch);
  }

  @Override
  public synchronized void close ()
    throws LoggerException {

    if (finished.compareAndSet(false, true)) {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ioException) {
          throw new LoggerException(ioException);
        }
      }
    }
  }

  @Override
  public synchronized void handleOutput (Record<?> record)
    throws IOException, LoggerException {

    if (finished.get()) {
      throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
    } else {

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

      if (entriesNode.add(entryNode).size() >= batch) {
        try {

          ArrayNode eventNode = JsonNodeFactory.instance.arrayNode();
          ObjectNode optionNode = JsonNodeFactory.instance.objectNode();
          byte[] chunk = new byte[16];
          int retry = 0;

          ThreadLocalRandom.current().nextBytes(chunk);
          optionNode.put("chunk", Base64Codec.encode(chunk));
          optionNode.put("size", batch);

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
        } finally {
          entriesNode = JsonNodeFactory.instance.arrayNode(batch);
        }
      }
    }
  }

  private void connect ()
    throws IOException {

    socket = new Socket(host, port);
    socket.setTcpNoDelay(true);
    socket.setSoTimeout(1000);
  }
}

