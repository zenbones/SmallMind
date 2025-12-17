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
package org.smallmind.batch.base;

public abstract class BatchParameter<T> {

  private final T value;
  private final boolean identifying;

  /**
   * Constructs a batch parameter wrapper.
   *
   * @param value       the parameter value to be supplied to a job
   * @param identifying {@code true} if the parameter participates in the identity of a job instance
   */
  public BatchParameter (T value, boolean identifying) {

    this.value = value;
    this.identifying = identifying;
  }

  /**
   * Identifies the underlying parameter type so that it can be translated to Spring Batch.
   *
   * @return the declared {@link ParameterType}
   */
  public abstract ParameterType getType ();

  /**
   * Provides the raw value stored for the parameter.
   *
   * @return the parameter value
   */
  public T getValue () {

    return value;
  }

  /**
   * Indicates whether this parameter should be treated as part of the job instance identity.
   *
   * @return {@code true} if identifying, otherwise {@code false}
   */
  public boolean isIdentifying () {

    return identifying;
  }
}
