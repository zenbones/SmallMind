/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;
import org.smallmind.scribe.pen.LoggerManager;
import org.smallmind.scribe.slf4j.ScribeLoggerFactory;

public class StaticLoggerBinder implements LoggerFactoryBinder {

   public static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
   public static final String REQUESTED_API_VERSION = "1.6.1";

   private final ILoggerFactory loggerFactory;

   static {

      LoggerManager.addLoggingPackagePrefix("org.slf4j.");
   }

   public static StaticLoggerBinder getSingleton () {

      return SINGLETON;
   }

   public StaticLoggerBinder () {

      loggerFactory = new ScribeLoggerFactory();
   }

   public ILoggerFactory getLoggerFactory () {

      return loggerFactory;
   }

   public String getLoggerFactoryClassStr () {

      return ScribeLoggerFactory.class.getName();
   }
}