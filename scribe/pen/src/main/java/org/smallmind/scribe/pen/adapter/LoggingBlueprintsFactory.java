/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
package org.smallmind.scribe.pen.adapter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ServiceLoader;
import org.smallmind.nutsnbolts.lang.StaticInitializationError;

public class LoggingBlueprintsFactory {

  private static LoggingBlueprints LOGGING_BLUEPRINTS;

  static {

    Iterator<LoggingBlueprints> blueprintsIter;

    blueprintsIter = ServiceLoader.load(LoggingBlueprints.class, Thread.currentThread().getContextClassLoader()).iterator();
    if (!blueprintsIter.hasNext()) {
      throw new StaticInitializationError("No provider found for LoggingBlueprints");
    }

    LOGGING_BLUEPRINTS = blueprintsIter.next();

    if (blueprintsIter.hasNext()) {

      LinkedList<String> implementationList = new LinkedList<String>();

      while (blueprintsIter.hasNext()) {
        implementationList.add(blueprintsIter.next().getClass().getName());
      }

      String[] implementations = new String[implementationList.size()];
      implementationList.toArray(implementations);

      throw new StaticInitializationError("Found conflicting service implementations(%s) %s", LoggingBlueprints.class.getSimpleName(), Arrays.toString(implementations));
    }
  }

  public static LoggingBlueprints getLoggingBlueprints () {

    return LOGGING_BLUEPRINTS;
  }
}