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

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.productivity.java.syslog4j.Syslog;
import org.productivity.java.syslog4j.SyslogConfigIF;
import org.productivity.java.syslog4j.SyslogIF;
import org.productivity.java.syslog4j.impl.message.structured.StructuredSyslogMessage;
import org.productivity.java.syslog4j.impl.net.udp.UDPNetSyslogConfig;
import org.smallmind.nutsnbolts.http.Base64Codec;
import org.smallmind.nutsnbolts.lang.StackTraceUtilities;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.springframework.beans.factory.InitializingBean;

public class SyslogAppender implements Appender, InitializingBean {

  private SyslogIF syslog;
  private ErrorHandler errorHandler;
  private ConcurrentLinkedQueue<Filter> filterList;
  private String name;
  private String syslogHost = "localhost";
  private String facility = "LOCAL7";
  private boolean active = true;
  private boolean base64EncodeStackTraces = false;
  private int syslogPort = 514;

  public SyslogAppender() {

    filterList = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    SyslogConfigIF config = new UDPNetSyslogConfig();

    config.setHost(syslogHost);
    config.setPort(syslogPort);
    config.setUseStructuredData(true);
    config.setFacility(facility);

    syslog = Syslog.createInstance("logging", config);
  }

  public String getSyslogHost() {

    return syslogHost;
  }

  public void setSyslogHost(String syslogHost) {

    this.syslogHost = syslogHost;
  }

  public int getSyslogPort() {

    return syslogPort;
  }

  public void setSyslogPort(int syslogPort) {

    this.syslogPort = syslogPort;
  }

  public String getFacility() {

    return facility;
  }

  public void setFacility(String facility) {

    this.facility = facility;
  }

  public boolean isBase64EncodeStackTraces() {

    return base64EncodeStackTraces;
  }

  public void setBase64EncodeStackTraces(boolean base64EncodeStackTraces) {

    this.base64EncodeStackTraces = base64EncodeStackTraces;
  }

  @Override
  public String getName() {

    return name;
  }

  @Override
  public void setName(String name) {

    this.name = name;
  }

  @Override
  public synchronized void clearFilters() {

    filterList.clear();
  }

  @Override
  public synchronized void setFilter(Filter filter) {

    filterList.clear();
    filterList.add(filter);
  }

  @Override
  public synchronized void setFilters(List<Filter> replacementFilterList) {

    filterList.clear();
    filterList.addAll(replacementFilterList);
  }

  @Override
  public synchronized void addFilter(Filter filter) {

    filterList.add(filter);
  }

  @Override
  public synchronized Filter[] getFilters() {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  @Override
  public void setErrorHandler(ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  @Override
  public ErrorHandler getErrorHandler() {

    return errorHandler;
  }

  @Override
  public void setFormatter(Formatter formatter) {

    throw new LoggerRuntimeException("The %s does not take external Formatters", SyslogAppender.class.getSimpleName());
  }

  @Override
  public Formatter getFormatter() {

    return null;
  }

  @Override
  public boolean requiresFormatter() {

    return false;
  }

  @Override
  public boolean isActive() {

    return active;
  }

  @Override
  public void setActive(boolean active) {

    this.active = active;
  }

  public void handleError(ErrorHandler errorHandler, Record record, Exception exception) {

    errorHandler.process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
  }

  public void publish(Record record) {

    try {
      for (Filter filter : filterList) {
        if (!filter.willLog(record)) {
          return;
        }
      }

      StructuredSyslogMessage message;
      HashMap<String, HashMap<String, String>> idMap = new HashMap<>();
      HashMap<String, String> logParamMap;
      LogicalContext logicalContext;
      Parameter[] parameters;
      Throwable throwable;
      String threadName;
      long threadId;

      idMap.put("log", logParamMap = new HashMap<>());
      logParamMap.put("timestamp", String.valueOf(record.getMillis()));
      logParamMap.put("logger", record.getLoggerName());

      threadName = record.getThreadName();
      threadId = record.getThreadID();
      if ((threadName != null) || (threadId > 0)) {

        HashMap<String, String> threadParamMap;

        idMap.put("thread", threadParamMap = new HashMap<>());
        if (threadName != null) {
          threadParamMap.put("name", threadName);
        }
        if (threadId > 0) {
          threadParamMap.put("id", String.valueOf(threadId));
        }
      }

      if ((throwable = record.getThrown()) != null) {
        logParamMap.put("stack-trace", (base64EncodeStackTraces) ? Base64Codec.encode(StackTraceUtilities.obtainStackTraceAsString(throwable)) : StackTraceUtilities.obtainStackTraceAsString(throwable));
      }

      if (((logicalContext = record.getLogicalContext()) != null) && logicalContext.isFilled()) {

        HashMap<String, String> contextParamMap;
        int lineNumber;

        idMap.put("context", contextParamMap = new HashMap<>());
        contextParamMap.put("class", logicalContext.getClassName());
        contextParamMap.put("method", logicalContext.getMethodName());
        contextParamMap.put("native", String.valueOf(logicalContext.isNativeMethod()));
        contextParamMap.put("file", logicalContext.getFileName());
        if ((!logicalContext.isNativeMethod()) && ((lineNumber = logicalContext.getLineNumber()) > 0)) {
          contextParamMap.put("line", String.valueOf(lineNumber));
        }
      }

      if ((parameters = record.getParameters()).length > 0) {

        HashMap<String, String> parameterParamMap;

        idMap.put("parameters", parameterParamMap = new HashMap<>());
        for (Parameter parameter : parameters) {

          String key = parameter.getKey();
          Object value = parameter.getValue();

          parameterParamMap.put((key == null) ? "null" : key, (value == null) ? "null" : value.toString());
        }
      }

      message = new StructuredSyslogMessage(String.valueOf(record.getSequenceNumber()), idMap, record.getMessage());

      switch (record.getLevel()) {
        case FATAL:
          syslog.critical(message);
          break;
        case ERROR:
          syslog.error(message);
          break;
        case WARN:
          syslog.warn(message);
          break;
        case INFO:
          syslog.info(message);
          break;
        case DEBUG:
          syslog.debug(message);
          break;
        case TRACE:
          syslog.debug(message);
          break;
        case OFF:
          break;
        default:
          throw new UnknownSwitchCaseException(record.getLevel().name());
      }
    } catch (Exception exception) {
      if (errorHandler == null) {
        exception.printStackTrace();
      } else {
        handleError(errorHandler, record, exception);
      }
    }
  }

  public void close()
      throws LoggerException {

  }
}