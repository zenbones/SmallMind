package org.smallmind.scribe.ink.slf4j;

import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerContext;
import org.smallmind.scribe.pen.MessageTranslator;
import org.smallmind.scribe.pen.Parameter;
import org.smallmind.scribe.pen.ParameterAwareRecord;
import org.smallmind.scribe.pen.SequenceGenerator;

public class SLF4JRecord extends ParameterAwareRecord<SLF4JRecord> {

  private final Level level;
  private final Throwable throwable;
  private final String loggerName;
  private final String message;
  private final String threadName;
  private final Object[] args;
  private final long millis;
  private final long threadId;
  private final long sequenceNumber;
  private LoggerContext loggerContext;

  public SLF4JRecord (String loggerName, Level level, Throwable throwable, String message, Object... args) {

    this.loggerName = loggerName;
    this.level = level;
    this.throwable = throwable;
    this.message = message;
    this.args = args;

    millis = System.currentTimeMillis();

    threadId = Thread.currentThread().getId();
    threadName = Thread.currentThread().getName();
    sequenceNumber = SequenceGenerator.next();
  }

  public void with (Logger logger) {

    LoggingEventBuilder eventBuilder = switch (level) {
      case OFF -> null;
      case TRACE -> logger.atTrace();
      case DEBUG -> logger.atDebug();
      case INFO -> logger.atInfo();
      case WARN -> logger.atWarn();
      case ERROR -> logger.atError();
      case FATAL -> logger.atError();
    };

    if (eventBuilder != null) {

      Parameter[] parameters;

      if (throwable != null) {
        eventBuilder = eventBuilder.setCause(throwable);
      }

      if ((parameters = getParameters()) != null) {
        for (Parameter parameter : parameters) {
          eventBuilder = eventBuilder.addKeyValue(parameter.getKey(), parameter.getValue());
        }
      }

      eventBuilder.log(message, args);
    }
  }

  @Override
  public SLF4JRecord getNativeLogEntry () {

    return this;
  }

  @Override
  public String getLoggerName () {

    return loggerName;
  }

  @Override
  public Level getLevel () {

    return level;
  }

  @Override
  public Throwable getThrown () {

    return throwable;
  }

  @Override
  public String getMessage () {

    return MessageTranslator.translateMessage(message, args);
  }

  @Override
  public LoggerContext getLoggerContext () {

    return loggerContext;
  }

  public void setLoggerContext (LoggerContext loggerContext) {

    this.loggerContext = loggerContext;
  }

  @Override
  public long getThreadID () {

    return threadId;
  }

  @Override
  public String getThreadName () {

    return threadName;
  }

  @Override
  public long getSequenceNumber () {

    return sequenceNumber;
  }

  @Override
  public long getMillis () {

    return millis;
  }
}
