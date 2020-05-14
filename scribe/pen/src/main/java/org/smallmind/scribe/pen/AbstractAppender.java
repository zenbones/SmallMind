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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractAppender implements Appender {

  private final ConcurrentLinkedQueue<Filter> filterList;
  private Formatter formatter;
  private ErrorHandler errorHandler;
  private String name;
  private boolean active = true;

  public AbstractAppender () {

    this(null, null, null);
  }

  public AbstractAppender (Formatter formatter, ErrorHandler errorHandler) {

    this(null, formatter, errorHandler);
  }

  public AbstractAppender (String name, Formatter formatter, ErrorHandler errorHandler) {

    this.name = name;
    this.formatter = formatter;
    this.errorHandler = errorHandler;

    filterList = new ConcurrentLinkedQueue<Filter>();
  }

  public abstract void handleOutput (Record record)
    throws Exception;

  public void handleError (ErrorHandler errorHandler, Record record, Exception exception) {

    errorHandler.process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
  }

  @Override
  public String getName () {

    return name;
  }

  @Override
  public void setName (String name) {

    this.name = name;
  }

  @Override
  public synchronized void clearFilters () {

    filterList.clear();
  }

  @Override
  public synchronized void setFilter (Filter filter) {

    filterList.clear();
    filterList.add(filter);
  }

  @Override
  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);
  }

  @Override
  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  @Override
  public synchronized void setFilters (List<Filter> replacementFilterList) {

    filterList.clear();
    filterList.addAll(replacementFilterList);
  }

  @Override
  public ErrorHandler getErrorHandler () {

    return errorHandler;
  }

  @Override
  public void setErrorHandler (ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  @Override
  public Formatter getFormatter () {

    return formatter;
  }

  @Override
  public void setFormatter (Formatter formatter) {

    this.formatter = formatter;
  }

  @Override
  public boolean isActive () {

    return active;
  }

  @Override
  public void setActive (boolean active) {

    this.active = active;
  }

  @Override
  public void publish (Record record) {

    try {
      for (Filter filter : filterList) {
        if (!filter.willLog(record)) {
          return;
        }
      }

      handleOutput(record);
    } catch (Exception exception) {
      if (errorHandler == null) {
        exception.printStackTrace();
      } else {
        handleError(errorHandler, record, exception);
      }
    }
  }

  @Override
  public void close ()
    throws LoggerException {

  }
}