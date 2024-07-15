/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

public class LoggerManager {

  private static final HashMap<String, Logger> LOGGER_MAP = new HashMap<String, Logger>();
  private static final HashMap<Logger, Template> TEMPLATE_MAP = new HashMap<Logger, Template>();
  private static final HashSet<Template> TEMPLATE_SET = new HashSet<Template>();
  private static final CopyOnWriteArraySet<String> LOGGING_PACKAGE_PREFIX_QUEUE = new CopyOnWriteArraySet<String>();

  static {

    LOGGING_PACKAGE_PREFIX_QUEUE.add("org.smallmind.scribe.");
  }

  public static void addLoggingPackagePrefix (String packageName) {

    LOGGING_PACKAGE_PREFIX_QUEUE.add(packageName);
  }

  public static boolean isLoggingClass (String className) {

    for (String packagePrefix : LOGGING_PACKAGE_PREFIX_QUEUE) {
      if (className.startsWith(packagePrefix)) {
        return true;
      }
    }

    return false;
  }

  public static void addTemplate (Template template) {

    synchronized (LoggerManager.class) {
      if (!TEMPLATE_SET.contains(template)) {
        TEMPLATE_SET.add(template);
        reAssociateAllLoggers();
      }
    }
  }

  public static void removeTemplate (Template template) {

    synchronized (LoggerManager.class) {
      if (TEMPLATE_SET.remove(template)) {
        reAssociateAllLoggers();
      }
    }
  }

  public static Template getTemplate (Logger logger) {

    synchronized (LoggerManager.class) {
      return TEMPLATE_MAP.get(logger);
    }
  }

  public static Logger getLogger (Class<?> loggableClass) {

    return getLogger(loggableClass.getCanonicalName());
  }

  public static Logger getLogger (String name) {

    Logger logger;

    if ((logger = LOGGER_MAP.get(name)) == null) {
      synchronized (LoggerManager.class) {
        if ((logger = LOGGER_MAP.get(name)) == null) {
          logger = new Logger(name);
          LOGGER_MAP.put(name, logger);
          associateTemplate(logger);
        }
      }
    }

    return logger;
  }

  protected static void commitTemplateChanges (Template.Change change, Template template) {

    synchronized (LoggerManager.class) {
      for (Logger logger : TEMPLATE_MAP.keySet()) {
        if (TEMPLATE_MAP.get(logger).equals(template)) {
          template.applyChange(change, logger);
        }
      }
    }
  }

  private static void reAssociateAllLoggers () {

    for (Logger logger : LOGGER_MAP.values()) {
      associateTemplate(logger);
    }
  }

  private static void associateTemplate (Logger logger) {

    Template matchingTemplate = null;
    int matchingPriority = Template.NO_MATCH;
    int templatePriority;

    for (Template template : TEMPLATE_SET) {
      templatePriority = template.matchLogger(logger.getName());
      if (templatePriority > matchingPriority) {
        matchingTemplate = template;
        matchingPriority = templatePriority;
      }
    }

    if (matchingTemplate == null) {
      TEMPLATE_MAP.remove(logger);
    } else if (!matchingTemplate.equals(getTemplate(logger))) {
      matchingTemplate.apply(logger);
      TEMPLATE_MAP.put(logger, matchingTemplate);
    }
  }
}
