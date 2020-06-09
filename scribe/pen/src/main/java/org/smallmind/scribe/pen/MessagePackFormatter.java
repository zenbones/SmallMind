/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

import java.util.Date;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class MessagePackFormatter {

  private final Timestamp timestamp;
  private final RecordElement[] recordElements;
  private final String newLine;

  public MessagePackFormatter (Timestamp timestamp, RecordElement[] recordElements, String newLine) {

    this.timestamp = timestamp;
    this.recordElements = recordElements;
    this.newLine = newLine;
  }

  public JsonNode format (Record record) {

    ObjectNode messageNode = JsonNodeFactory.instance.objectNode();

    for (RecordElement recordElement : recordElements) {
      switch (recordElement) {
        case DATE:
          messageNode.put("date", timestamp.getTimestamp(new Date(record.getMillis())));
          break;
        case MILLISECONDS:
          messageNode.put("milliseconds", record.getMillis());
          break;
        case LOGGER_NAME:
          messageNode.put("logger", record.getLoggerName());
          break;
        case LEVEL:
          messageNode.put("level", record.getLevel().name());
          break;
        case MESSAGE:

          String message;

          if ((message = record.getMessage()) == null) {

            Throwable throwable;

            if ((throwable = record.getThrown()) != null) {
              message = throwable.getMessage();
            }
          }

          messageNode.put("message", message);
          break;
        case THREAD:
          appendThreadInfo(messageNode, record.getThreadName(), record.getThreadID());
          break;
        case LOGICAL_CONTEXT:
          appendLogicalContext(messageNode, record.getLogicalContext());
          break;
        case PARAMETERS:
          appendParameters(messageNode, record.getParameters());
          break;
        case STACK_TRACE:
          appendStackTrace(messageNode, record.getThrown());
          break;
        default:
          throw new UnknownSwitchCaseException(recordElement.name());
      }
    }

    return messageNode;
  }

  private void appendThreadInfo (ObjectNode messageNode, String threadName, long threadId) {

    if ((threadName != null) || (threadId > 0)) {

      ObjectNode threadNode = JsonNodeFactory.instance.objectNode();

      if (threadName != null) {
        threadNode.put("name", threadName);
      }
      if (threadId > 0) {
        threadNode.put("id", threadId);
      }

      messageNode.set("thread", threadNode);
    }
  }

  private void appendLogicalContext (ObjectNode messageNode, LogicalContext logicalContext) {

    if ((logicalContext != null) && (logicalContext.isFilled())) {

      ObjectNode contextNode = JsonNodeFactory.instance.objectNode();

      contextNode.put("class", logicalContext.getClassName());
      contextNode.put("method", logicalContext.getMethodName());
      contextNode.put("native", logicalContext.isNativeMethod());
      if ((!logicalContext.isNativeMethod()) && (logicalContext.getLineNumber() > 0)) {
        contextNode.put("line", logicalContext.getLineNumber());
      }
      contextNode.put("file", logicalContext.getFileName());

      messageNode.set("context", contextNode);
    }
  }

  private void appendParameters (ObjectNode messageNode, Parameter[] parameters) {

    if (parameters.length > 0) {

      ObjectNode parametersNode = JsonNodeFactory.instance.objectNode();

      for (Parameter parameter : parameters) {

        String key;

        if ((key = parameter.getKey()) != null) {
          if (parameter.getValue() == null) {
            parametersNode.set(key, JsonNodeFactory.instance.nullNode());
          } else {
            parametersNode.put(key, parameter.getValue().toString());
          }
        }
      }

      if (!parametersNode.isEmpty()) {
        messageNode.set("parameters", parametersNode);
      }
    }
  }

  private void appendStackTrace (ObjectNode messageNode, Throwable throwable) {

    StackTraceElement[] prevStackTrace = null;
    int repeatedElements;

    if (throwable != null) {

      StringBuilder traceBuilder = new StringBuilder();

      do {

        if (prevStackTrace == null) {
          traceBuilder.append("Exception in thread ");
        } else {
          traceBuilder.append("Caused by: ");
        }

        traceBuilder.append(throwable.getClass().getCanonicalName());
        traceBuilder.append(": ");
        traceBuilder.append(throwable.getMessage());
        traceBuilder.append(newLine);

        for (StackTraceElement singleElement : throwable.getStackTrace()) {

          traceBuilder.append("   ");

          if (prevStackTrace != null) {
            if ((repeatedElements = findRepeatedStackElements(singleElement, prevStackTrace)) >= 0) {
              traceBuilder.append("   ... ");
              traceBuilder.append(repeatedElements);
              traceBuilder.append(" more");
              traceBuilder.append(newLine);
              break;
            }
          }

          traceBuilder.append("   at ");
          traceBuilder.append(singleElement.toString());
          traceBuilder.append(newLine);
        }

        prevStackTrace = throwable.getStackTrace();
      } while ((throwable = throwable.getCause()) != null);

      messageNode.put("stackTrace", traceBuilder.toString());
    }
  }

  private int findRepeatedStackElements (StackTraceElement singleElement, StackTraceElement[] prevStackTrace) {

    for (int count = 0; count < prevStackTrace.length; count++) {
      if (singleElement.equals(prevStackTrace[count])) {
        return prevStackTrace.length - count;
      }
    }

    return -1;
  }
}