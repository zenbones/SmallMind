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
package org.smallmind.scribe.pen.adapter;

import java.io.Serializable;
import org.smallmind.scribe.pen.Parameter;

/**
 * Thread-local implementation of {@link ParameterAdapter} using inheritable thread storage.
 */
public class Parameters implements ParameterAdapter {

  private static final Parameters INSTANCE = new Parameters();

  /**
   * Thread-local storage for parameters, inherited by child threads.
   */
  private static final InheritableThreadLocal<RecordParameters> RECORD_PARAMETERS_LOCAL = new InheritableThreadLocal<>() {

    /**
     * Initializes thread-local storage with an empty parameter set.
     *
     * @return new {@link RecordParameters} instance
     */
    @Override
    protected RecordParameters initialValue () {

      return new RecordParameters();
    }
  };

  /**
   * Returns the singleton parameter adapter instance.
   *
   * @return shared {@link Parameters} instance
   */
  public static Parameters getInstance () {

    return INSTANCE;
  }

  /**
   * Adds or replaces a parameter value for the current thread.
   *
   * @param key   parameter key
   * @param value serializable value
   */
  @Override
  public void put (String key, Serializable value) {

    RECORD_PARAMETERS_LOCAL.get().put(key, value);
  }

  /**
   * Removes a parameter for the current thread.
   *
   * @param key key to remove
   */
  @Override
  public void remove (String key) {

    RECORD_PARAMETERS_LOCAL.get().remove(key);
  }

  /**
   * Clears all parameters for the current thread.
   */
  @Override
  public void clear () {

    RECORD_PARAMETERS_LOCAL.get().clear();
  }

  /**
   * Retrieves a parameter for the current thread.
   *
   * @param key key to look up
   * @return value or {@code null} if absent
   */
  @Override
  public Serializable get (String key) {

    return RECORD_PARAMETERS_LOCAL.get().get(key);
  }

  /**
   * Returns all parameters for the current thread.
   *
   * @return array of parameters
   */
  @Override
  public Parameter[] getParameters () {

    return RECORD_PARAMETERS_LOCAL.get().asParameters();
  }
}
