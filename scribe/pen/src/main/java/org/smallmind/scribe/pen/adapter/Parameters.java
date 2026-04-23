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
 * Singleton {@link ParameterAdapter} implementation that stores contextual key/value parameters in an
 * {@link InheritableThreadLocal}, so that child threads automatically inherit the parameter set that was
 * active in their parent thread at the time of creation.
 */
public class Parameters implements ParameterAdapter {

  private static final Parameters INSTANCE = new Parameters();

  private static final InheritableThreadLocal<RecordParameters> RECORD_PARAMETERS_LOCAL = new InheritableThreadLocal<>() {

    @Override
    protected RecordParameters initialValue () {

      return new RecordParameters();
    }
  };

  /**
   * Returns the process-wide singleton instance of this adapter.
   *
   * @return the shared {@code Parameters} instance
   */
  public static Parameters getInstance () {

    return INSTANCE;
  }

  /**
   * Stores or replaces the value associated with {@code key} in the calling thread's parameter map.
   *
   * @param key   the parameter key; must not be {@code null}
   * @param value the serializable value to associate with the key
   */
  @Override
  public void put (String key, Serializable value) {

    RECORD_PARAMETERS_LOCAL.get().put(key, value);
  }

  /**
   * Removes the parameter identified by {@code key} from the calling thread's parameter map.
   *
   * @param key the key of the parameter to remove
   */
  @Override
  public void remove (String key) {

    RECORD_PARAMETERS_LOCAL.get().remove(key);
  }

  /**
   * Removes all parameters from the calling thread's parameter map.
   */
  @Override
  public void clear () {

    RECORD_PARAMETERS_LOCAL.get().clear();
  }

  /**
   * Returns the value associated with {@code key} in the calling thread's parameter map.
   *
   * @param key the key to look up
   * @return the associated value, or {@code null} if no mapping exists for the key
   */
  @Override
  public Serializable get (String key) {

    return RECORD_PARAMETERS_LOCAL.get().get(key);
  }

  /**
   * Returns a snapshot of all parameters in the calling thread's parameter map as a {@link Parameter} array.
   *
   * @return an array of all current parameters; never {@code null} but may be empty
   */
  @Override
  public Parameter[] getParameters () {

    return RECORD_PARAMETERS_LOCAL.get().asParameters();
  }
}
