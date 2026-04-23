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

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Static registry that creates, caches, and manages {@link Logger} instances and maps each one to
 * its highest-priority matching {@link Template}. Logger lookup uses double-checked locking for
 * safe, low-contention creation. Adding or removing a template triggers re-association of all
 * known loggers.
 */
public class LoggerManager {

  private static final HashMap<String, Logger> LOGGER_MAP = new HashMap<String, Logger>();
  private static final HashMap<Logger, Template> TEMPLATE_MAP = new HashMap<Logger, Template>();
  private static final HashSet<Template> TEMPLATE_SET = new HashSet<Template>();
  private static final CopyOnWriteArraySet<String> LOGGING_PACKAGE_PREFIX_QUEUE = new CopyOnWriteArraySet<String>();

  static {

    LOGGING_PACKAGE_PREFIX_QUEUE.add("org.smallmind.scribe.");
  }

  /**
   * Registers an additional package prefix whose classes should be treated as logging infrastructure
   * and excluded from call-stack capture when computing caller information.
   *
   * @param packageName package prefix string to register (e.g. {@code "com.example.logging."});
   *                    must not be {@code null}
   */
  public static void addLoggingPackagePrefix (String packageName) {

    LOGGING_PACKAGE_PREFIX_QUEUE.add(packageName);
  }

  /**
   * Tests whether the given fully qualified class name belongs to any registered logging
   * infrastructure package and should therefore be skipped during call-stack inspection.
   *
   * @param className fully qualified class name to test; must not be {@code null}
   * @return {@code true} if {@code className} starts with any registered package prefix;
   * {@code false} otherwise
   */
  public static boolean isLoggingClass (String className) {

    for (String packagePrefix : LOGGING_PACKAGE_PREFIX_QUEUE) {
      if (className.startsWith(packagePrefix)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Adds a template to the active template set and immediately re-associates every known logger
   * with the highest-priority matching template. Has no effect if the template is already registered.
   *
   * @param template template to register; must not be {@code null}
   */
  public static void addTemplate (Template template) {

    synchronized (LoggerManager.class) {
      if (!TEMPLATE_SET.contains(template)) {
        TEMPLATE_SET.add(template);
        reAssociateAllLoggers();
      }
    }
  }

  /**
   * Removes a template from the active template set and immediately re-associates every known logger
   * with the highest-priority remaining matching template. Has no effect if the template is not registered.
   *
   * @param template template to remove; must not be {@code null}
   */
  public static void removeTemplate (Template template) {

    synchronized (LoggerManager.class) {
      if (TEMPLATE_SET.remove(template)) {
        reAssociateAllLoggers();
      }
    }
  }

  /**
   * Returns the template currently mapped to the given logger.
   *
   * @param logger the logger whose associated template is requested; must not be {@code null}
   * @return the template governing this logger, or {@code null} if no template matches it
   */
  public static Template getTemplate (Logger logger) {

    synchronized (LoggerManager.class) {
      return TEMPLATE_MAP.get(logger);
    }
  }

  /**
   * Returns or creates the {@link Logger} whose name is the canonical name of the supplied class,
   * delegating to {@link #getLogger(String)}.
   *
   * @param loggableClass class whose canonical name identifies the logger; must not be {@code null}
   * @return the cached or newly created logger for that name
   */
  public static Logger getLogger (Class<?> loggableClass) {

    return getLogger(loggableClass.getCanonicalName());
  }

  /**
   * Returns the cached {@link Logger} for the given name, or creates and caches a new one using
   * double-checked locking. Newly created loggers are immediately associated with the best matching
   * template from the active template set.
   *
   * @param name the logger name; must not be {@code null}
   * @return the cached or newly created logger for that name
   */
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

  /**
   * Applies a single template change to every logger currently associated with the given template,
   * pushing the updated configuration value onto each affected logger.
   *
   * @param change   the aspect of the template that changed
   * @param template the template that was modified; must not be {@code null}
   */
  protected static void commitTemplateChanges (Template.Change change, Template template) {

    synchronized (LoggerManager.class) {
      for (Logger logger : TEMPLATE_MAP.keySet()) {
        if (TEMPLATE_MAP.get(logger).equals(template)) {
          template.applyChange(change, logger);
        }
      }
    }
  }

  /**
   * Iterates over all known loggers and re-runs template matching for each one, updating the
   * template association map and applying any changed configuration.
   */
  private static void reAssociateAllLoggers () {

    for (Logger logger : LOGGER_MAP.values()) {
      associateTemplate(logger);
    }
  }

  /**
   * Finds the highest-priority matching template for the given logger, applies it if the association
   * has changed, and updates the template map accordingly. Removes the logger from the map if no
   * template matches.
   *
   * @param logger the logger to match and configure
   */
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
