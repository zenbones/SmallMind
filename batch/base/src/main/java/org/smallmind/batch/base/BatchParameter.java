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

/**
 * Type-safe envelope for a single batch job parameter, combining a value with a job-instance
 * identity flag.
 * <p>
 * Each concrete subclass pins {@code T} to a specific {@link ParameterType} so the job factory
 * can select the correct Spring Batch {@code JobParameter} variant. The {@code identifying} flag
 * governs whether the value is included in the unique key that distinguishes one job instance
 * from another.
 *
 * @param <T> the Java type of the enclosed parameter value
 */
public abstract class BatchParameter<T> {

  private final T value;
  private final boolean identifying;

  /**
   * Stores the parameter value and identity flag.
   *
   * @param value       the value to deliver to the batch job
   * @param identifying {@code true} if this value forms part of the job-instance key
   */
  public BatchParameter (T value, boolean identifying) {

    this.value = value;
    this.identifying = identifying;
  }

  /**
   * Returns the discriminant that the job factory uses to select the correct Spring Batch
   * parameter builder method.
   *
   * @return the {@link ParameterType} declared by the concrete subclass
   */
  public abstract ParameterType getType ();

  /**
   * Returns the raw parameter value supplied at construction time.
   *
   * @return the parameter value
   */
  public T getValue () {

    return value;
  }

  /**
   * Returns whether this parameter is part of the job-instance identity key.
   *
   * @return {@code true} if identifying, {@code false} otherwise
   */
  public boolean isIdentifying () {

    return identifying;
  }
}
