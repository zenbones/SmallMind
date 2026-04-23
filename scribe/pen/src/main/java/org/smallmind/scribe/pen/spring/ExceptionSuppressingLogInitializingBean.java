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
 * Spring {@link InitializingBean} that collects throwable class definitions and, on initialization, registers
 * them with {@link ExceptionSuppressingLogFilter} so that log records carrying those exception types are
 * silently discarded by the filter.
 */
public class ExceptionSuppressingLogInitializingBean implements InitializingBean {

  private final LinkedList<Class<? extends Throwable>> suppressedThrowableClassList = new LinkedList<>();

  /**
   * Supplies the throwable classes that should be suppressed; each class in the array will be added to
   * the internal list and subsequently registered with {@link ExceptionSuppressingLogFilter} when
   * {@link #afterPropertiesSet()} is called.
   *
   * @param suppressedThrowableClasses array of throwable types whose log records should be silently discarded
   */
  public void setSuppressedThrowableClasses (Class<? extends Throwable>[] suppressedThrowableClasses) {

    suppressedThrowableClassList.addAll(Arrays.asList(suppressedThrowableClasses));
  }

  /**
   * Passes all accumulated suppressed throwable classes to
   * {@link ExceptionSuppressingLogFilter#addSuppressedThrowableClasses} so that the filter begins
   * discarding records associated with those exception types.
   */
  @Override
  public void afterPropertiesSet () {

    ExceptionSuppressingLogFilter.addSuppressedThrowableClasses(suppressedThrowableClassList);
  }
}
