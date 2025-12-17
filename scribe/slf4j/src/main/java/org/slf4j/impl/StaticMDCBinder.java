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
package org.slf4j.impl;

import org.slf4j.spi.MDCAdapter;
import org.smallmind.scribe.slf4j.ScribeMDCAdapter;

/**
 * SLF4J service binder that exposes the scribe-backed {@link MDCAdapter} implementation.
 * This class mirrors the standard SLF4J lookup contract to supply MDC support at runtime.
 */
public class StaticMDCBinder {

  public static final StaticMDCBinder SINGLETON = new StaticMDCBinder();

  private final MDCAdapter mdcAdapter;

  /**
   * Creates a binder with a singleton {@link ScribeMDCAdapter} instance.
   */
  public StaticMDCBinder () {

    mdcAdapter = new ScribeMDCAdapter();
  }

  /**
   * Provides the singleton binder instance expected by SLF4J.
   *
   * @return the static binder
   */
  public static StaticMDCBinder getSingleton () {

    return SINGLETON;
  }

  /**
   * Returns the MDC adapter bound to this logger implementation.
   *
   * @return the MDC adapter to use for contextual logging
   */
  public MDCAdapter getMDCA () {

    return mdcAdapter;
  }
}
