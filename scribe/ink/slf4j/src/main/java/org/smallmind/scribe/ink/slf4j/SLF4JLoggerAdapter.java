package org.smallmind.scribe.ink.slf4j;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.Enhancer;
import org.smallmind.scribe.pen.Filter;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.ParameterAdapter;
import org.smallmind.scribe.pen.adapter.Parameters;

public class SLF4JLoggerAdapter implements LoggerAdapter {

  private final Logger logger;
  private final ConcurrentLinkedQueue<Filter> filterList;
  private final ConcurrentLinkedQueue<Enhancer> enhancerList;

  public SLF4JLoggerAdapter (Logger logger) {

    this.logger = logger;

    filterList = new ConcurrentLinkedQueue<>();
    enhancerList = new ConcurrentLinkedQueue<>();
  }

  @Override
  public String getName () {

    return logger.getName();
  }

  @Override
  public ParameterAdapter getParameterAdapter () {

    return Parameters.getInstance();
  }

  @Override
  public boolean getAutoFillLoggerContext () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void addFilter (Filter filter) {

    filterList.add(filter);
  }

  @Override
  public void clearFilters () {

    filterList.clear();
  }

  @Override
  public void addAppender (Appender appender) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void clearAppenders () {

    throw new UnsupportedOperationException();
  }

  @Override
  public void addEnhancer (Enhancer enhancer) {

    enhancerList.add(enhancer);
  }

  @Override
  public void clearEnhancers () {

    enhancerList.clear();
  }

  @Override
  public Level getLevel () {

    if (logger.isTraceEnabled()) {
      return Level.TRACE;
    } else if (logger.isDebugEnabled()) {
      return Level.DEBUG;
    } else if (logger.isInfoEnabled()) {
      return Level.INFO;
    } else if (logger.isWarnEnabled()) {
      return Level.WARN;
    } else {
      return Level.ERROR;
    }
  }

  @Override
  public void setLevel (Level level) {

    throw new UnsupportedOperationException();
  }

  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    SLF4JRecord slf4JRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if (willLog(level)) {
        slf4JRecord = new SLF4JRecord(logger.getName(), level, throwable, message, args);
        slf4JRecord.setParameters(getParameterAdapter().getParameters());
        enhanceRecord(slf4JRecord);
        slf4JRecord.with(logger);
      }
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    SLF4JRecord slf4JRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if (willLog(level)) {
        slf4JRecord = new SLF4JRecord(logger.getName(), level, throwable, (object == null) ? null : object.toString());
        slf4JRecord.setParameters(getParameterAdapter().getParameters());
        enhanceRecord(slf4JRecord);
        slf4JRecord.with(logger);
      }
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    SLF4JRecord slf4JRecord;

    if ((!level.equals(Level.OFF)) && getLevel().noGreater(level)) {
      if (willLog(level)) {
        slf4JRecord = new SLF4JRecord(logger.getName(), level, throwable, (supplier == null) ? null : supplier.get());
        slf4JRecord.setParameters(getParameterAdapter().getParameters());
        enhanceRecord(slf4JRecord);
        slf4JRecord.with(logger);
      }
    }
  }

  private boolean willLog (Level level) {

    if (!filterList.isEmpty()) {

      SLF4JRecord filterRecord = new SLF4JRecord(logger.getName(), level, null, null);

      for (Filter filter : filterList) {
        if (!filter.willLog(filterRecord)) {

          return false;
        }
      }
    }

    return true;
  }

  private void enhanceRecord (SLF4JRecord record) {

    for (Enhancer enhancer : enhancerList) {
      enhancer.enhance(record);
    }
  }
}
