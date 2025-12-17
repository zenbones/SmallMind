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
package org.smallmind.scribe.pen.spring;

import java.util.Arrays;
import java.util.LinkedList;
import org.smallmind.scribe.pen.ExceptionSuppressingLogFilter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Spring helper that registers suppressed throwable classes for {@link ExceptionSuppressingLogFilter}.
 */
public class ExceptionSuppressingLogInitializingBean implements InitializingBean {

  private final LinkedList<Class<? extends Throwable>> suppressedThrowableClassList = new LinkedList<>();

  /**
   * Configures throwable classes to suppress in logging.
   *
   * @param suppressedThrowableClasses array of throwable types to filter out
   */
  public void setSuppressedThrowableClasses (Class<? extends Throwable>[] suppressedThrowableClasses) {

    suppressedThrowableClassList.addAll(Arrays.asList(suppressedThrowableClasses));
  }

  /**
   * Registers the configured suppressed throwable classes with the filter.
   */
  @Override
  public void afterPropertiesSet () {

    ExceptionSuppressingLogFilter.addSuppressedThrowableClasses(suppressedThrowableClassList);
  }
}
