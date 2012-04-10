/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.scribe.ink.jdk;

import java.io.UnsupportedEncodingException;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.smallmind.scribe.pen.Appender;
import org.smallmind.scribe.pen.LoggerException;
import org.smallmind.scribe.pen.adapter.RecordWrapper;

public class JDKAppenderWrapper extends Handler {

   private Appender appender;

   public JDKAppenderWrapper (Appender appender) {

      this.appender = appender;
   }

   protected Appender getInnerAppender () {

      return appender;
   }

   public void setEncoding (String encoding)
      throws SecurityException, UnsupportedEncodingException {

      throw new UnsupportedOperationException("Unsupported native JDK Logging method");
   }

   public String getEncoding () {

      throw new UnsupportedOperationException("Unsupported native JDK Logging method");
   }

   public Level getLevel () {

      throw new UnsupportedOperationException("Unsupported native JDK Logging method");
   }

   public void setLevel (Level newLevel)
      throws SecurityException {

      throw new UnsupportedOperationException("Unsupported native JDK Logging method");
   }

   public void setFormatter (Formatter formatter) {

      appender.setFormatter(new JDKFormatterAdapter(formatter));
   }

   public Formatter getFormatter () {

      org.smallmind.scribe.pen.Formatter formatter;

      if ((formatter = appender.getFormatter()) != null) {
         if (!(formatter instanceof JDKFormatterAdapter)) {
            throw new UnsupportedOperationException("Can not return a non-JDK Logging native Formatter(" + formatter.getClass().getCanonicalName() + ")");
         }

         return ((JDKFormatterAdapter)formatter).getNativeFormatter();
      }

      return null;
   }

   public void setFilter (Filter filter) {

      appender.clearFilters();
      appender.addFilter(new JDKFilterAdapter(filter));
   }

   public Filter getFilter () {

      org.smallmind.scribe.pen.Filter[] filters;

      if ((filters = appender.getFilters()).length > 0) {
         if (!(filters[0] instanceof JDKFilterAdapter)) {
            throw new UnsupportedOperationException("Can not return a non-JDK Logging native Filter(" + filters[0].getClass().getCanonicalName() + ")");
         }

         return ((JDKFilterAdapter)filters[0]).getNativeFilter();
      }

      return null;
   }

   public boolean isLoggable (LogRecord record) {

      for (org.smallmind.scribe.pen.Filter filter : appender.getFilters()) {
         if (!(filter instanceof JDKFilterAdapter)) {
            throw new UnsupportedOperationException("Encountered a non-JDK Logging native Filter(" + filter.getClass().getCanonicalName() + ")");
         }
         else if (!((JDKFilterAdapter)filter).getNativeFilter().isLoggable(record)) {
            return false;
         }
      }

      return false;
   }

   public void setErrorManager (ErrorManager errorManager) {

      appender.setErrorHandler(new JDKErrorHandlerAdapter(errorManager));
   }

   public ErrorManager getErrorManager () {

      org.smallmind.scribe.pen.ErrorHandler errorHandler;

      if ((errorHandler = appender.getErrorHandler()) != null) {
         if (!(errorHandler instanceof JDKErrorHandlerAdapter)) {
            throw new UnsupportedOperationException("Can not return a non-JDK Logging native ErrorManager(" + errorHandler.getClass().getCanonicalName() + ")");
         }

         return ((JDKErrorHandlerAdapter)errorHandler).getNativeErrorManager();
      }

      return null;
   }

   public void publish (LogRecord record) {

      appender.publish(((RecordWrapper)record).getRecord());
   }

   public void flush () {
   }

   public void close ()
      throws SecurityException {

      try {
         appender.close();
      }
      catch (LoggerException loggerException) {
         throw new SecurityException(loggerException);
      }
   }

   public int hashCode () {

      return appender.hashCode();
   }

   protected void finalize () {

      close();
   }

   public boolean equals (Object obj) {

      if (obj instanceof JDKAppenderWrapper) {
         return appender.equals(((JDKAppenderWrapper)obj).getInnerAppender());
      }

      return appender.equals(obj);
   }
}