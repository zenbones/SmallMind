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
package org.smallmind.nutsnbolts.layout;

/**
 * Composite hash-map key that uniquely identifies a cached measurement for a specific layout part,
 * axis, and measurement category within a {@link LayoutTailor}.
 *
 * @param part        the layout component or box whose measurement is keyed
 * @param bias        the axis to which the measurement applies
 * @param tapeMeasure the category of measurement (minimum, preferred, or maximum)
 */
public record Sizing(Object part, Bias bias, TapeMeasure tapeMeasure) {

  /**
   * Returns a hash code derived from the combination of part identity, axis, and measurement category.
   *
   * @return the combined hash code
   */
  @Override
  public int hashCode () {

    return part.hashCode() ^ bias.hashCode() ^ tapeMeasure.hashCode();
  }

  /**
   * Returns {@code true} if the given object is a {@code Sizing} with the same part, bias, and tape measure.
   *
   * @param obj the object to compare
   * @return {@code true} if the three key fields are equal; {@code false} otherwise
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof Sizing) && ((Sizing)obj).part().equals(part) && ((Sizing)obj).bias().equals(bias) && ((Sizing)obj).tapeMeasure().equals(tapeMeasure);
  }
}
