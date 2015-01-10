/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractAppender implements Appender {

  private Formatter formatter;
  private ErrorHandler errorHandler;
  private ConcurrentLinkedQueue<Filter> filterList;
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

  public abstract void handleOutput (String formattedOutput)
    throws Exception;

  public void handleError (ErrorHandler errorHandler, Record record, Exception exception) {

    errorHandler.process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
  }

  public String getName () {

    return name;
  }

  public void setName (String name) {

    this.name = name;
  }

  public synchronized void clearFilters () {

    filterList.clear();
  }

  public synchronized void setFilter (Filter filter) {

    filterList.clear();
    filterList.add(filter);
  }

  public synchronized void setFilters (List<Filter> replacementFilterList) {

    filterList.clear();
    filterList.addAll(replacementFilterList);
  }

  public synchronized void addFilter (Filter filter) {

    filterList.add(filter);
  }

  public synchronized Filter[] getFilters () {

    Filter[] filters;

    filters = new Filter[filterList.size()];
    filterList.toArray(filters);

    return filters;
  }

  public void setErrorHandler (ErrorHandler errorHandler) {

    this.errorHandler = errorHandler;
  }

  public ErrorHandler getErrorHandler () {

    return errorHandler;
  }

  public void setFormatter (Formatter formatter) {

    this.formatter = formatter;
  }

  public Formatter getFormatter () {

    return formatter;
  }

  public boolean isActive () {

    return active;
  }

  public void setActive (boolean active) {

    this.active = active;
  }

  public void publish (Record record) {

    try {
      for (Filter filter : filterList) {
        if (!filter.willLog(record)) {
          return;
        }
      }

      if (formatter != null) {
        handleOutput(formatter.format(record, filterList));
      }
      else if (requiresFormatter()) {
        throw new LoggerException("No formatter set for log output on this appender(%s)", this.getClass().getCanonicalName());
      }
    }
    catch (Exception exception) {
      if (errorHandler == null) {
        exception.printStackTrace();
      }
      else {
        handleError(errorHandler, record, exception);
      }
    }
  }

  public void close ()
    throws LoggerException {

  }
}