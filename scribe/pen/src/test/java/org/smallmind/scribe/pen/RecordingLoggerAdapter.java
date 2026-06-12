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
package org.smallmind.scribe.pen;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.smallmind.scribe.pen.adapter.LoggerAdapter;
import org.smallmind.scribe.pen.adapter.ParameterAdapter;

/**
 * A backend-free {@link LoggerAdapter} that records each {@code logMessage(...)} call into a
 * process-wide sink so that {@link Logger} and {@link LoggerManager} can be exercised without a real
 * ink backend on the test classpath. It replicates the reference {@code IndigenousLoggerAdapter}
 * level-threshold guard ({@code !OFF && getLevel().noGreater(level)}) so that suppressed lazy
 * {@code Supplier}/{@code Object} messages are never materialized — the property the lazy-logging
 * tests assert. Discovered via the {@code META-INF/services} test resource for {@code RecordingLoggingBlueprint}.
 */
public class RecordingLoggerAdapter implements LoggerAdapter {

  private static final List<Event> EVENTS = new ArrayList<>();

  private final MapParameterAdapter parameterAdapter = new MapParameterAdapter();
  private final List<Appender> appenders = new ArrayList<>();
  private final List<Filter> filters = new ArrayList<>();
  private final List<Enhancer> enhancers = new ArrayList<>();
  private final String name;
  private Level level = Level.INFO;
  private boolean autoFillLoggerContext;

  public RecordingLoggerAdapter (String name) {

    this.name = name;
  }

  public static synchronized void reset () {

    EVENTS.clear();
  }

  public static synchronized List<Event> getEvents () {

    return new ArrayList<>(EVENTS);
  }

  private static synchronized void record (Event event) {

    EVENTS.add(event);
  }

  @Override
  public String getName () {

    return name;
  }

  @Override
  public ParameterAdapter getParameterAdapter () {

    return parameterAdapter;
  }

  @Override
  public boolean getAutoFillLoggerContext () {

    return autoFillLoggerContext;
  }

  @Override
  public void setAutoFillLoggerContext (boolean autoFillLoggerContext) {

    this.autoFillLoggerContext = autoFillLoggerContext;
  }

  @Override
  public void addFilter (Filter filter) {

    filters.add(filter);
  }

  @Override
  public void clearFilters () {

    filters.clear();
  }

  public List<Filter> getFilters () {

    return filters;
  }

  @Override
  public void addAppender (Appender appender) {

    appenders.add(appender);
  }

  @Override
  public void clearAppenders () {

    appenders.clear();
  }

  public List<Appender> getAppenders () {

    return appenders;
  }

  @Override
  public void addEnhancer (Enhancer enhancer) {

    enhancers.add(enhancer);
  }

  @Override
  public void clearEnhancers () {

    enhancers.clear();
  }

  public List<Enhancer> getEnhancers () {

    return enhancers;
  }

  @Override
  public Level getLevel () {

    return level;
  }

  @Override
  public void setLevel (Level level) {

    this.level = level;
  }

  private boolean passesThreshold (Level requestedLevel) {

    return (!requestedLevel.equals(Level.OFF)) && getLevel().noGreater(requestedLevel);
  }

  @Override
  public void logMessage (Level level, Throwable throwable, String message, Object... args) {

    if (passesThreshold(level)) {
      record(new Event(name, level, MessageTranslator.translateMessage(message, args), throwable));
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Object object) {

    if (passesThreshold(level)) {
      record(new Event(name, level, (object == null) ? null : object.toString(), throwable));
    }
  }

  @Override
  public void logMessage (Level level, Throwable throwable, Supplier<String> supplier) {

    if (passesThreshold(level)) {
      record(new Event(name, level, (supplier == null) ? null : supplier.get(), throwable));
    }
  }

  /**
   * Immutable snapshot of a single recorded logging call.
   */
  public static class Event {

    private final Level level;
    private final Throwable throwable;
    private final String name;
    private final String message;

    public Event (String name, Level level, String message, Throwable throwable) {

      this.name = name;
      this.level = level;
      this.message = message;
      this.throwable = throwable;
    }

    public String getName () {

      return name;
    }

    public Level getLevel () {

      return level;
    }

    public String getMessage () {

      return message;
    }

    public Throwable getThrowable () {

      return throwable;
    }
  }

  /**
   * A simple map-backed {@link ParameterAdapter} scoped to this single adapter, so parameter
   * round-trips are isolated per logger and do not leak through the shared thread-local store.
   */
  private static class MapParameterAdapter implements ParameterAdapter {

    private final Map<String, Serializable> values = new LinkedHashMap<>();

    @Override
    public void put (String key, Serializable value) {

      values.put(key, value);
    }

    @Override
    public void remove (String key) {

      values.remove(key);
    }

    @Override
    public void clear () {

      values.clear();
    }

    @Override
    public Serializable get (String key) {

      return values.get(key);
    }

    @Override
    public Parameter[] getParameters () {

      Parameter[] parameters = new Parameter[values.size()];
      int index = 0;

      for (Map.Entry<String, Serializable> entry : values.entrySet()) {
        parameters[index++] = new Parameter(entry.getKey(), entry.getValue());
      }

      return parameters;
    }
  }
}
