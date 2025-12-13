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
package org.smallmind.javafx.extras.instrument;

/**
 * Represents velocity averages across multiple rolling windows.
 */
public record Blur(double avgVelocity, double avgVelocity_1, double avgVelocity_5, double avgVelocity_15) {

  /**
   * Creates a blur instance with the supplied averages.
   *
   * @param avgVelocity    lifetime average velocity
   * @param avgVelocity_1  one minute average velocity
   * @param avgVelocity_5  five minute average velocity
   * @param avgVelocity_15 fifteen minute average velocity
   */
  public Blur {

  }

  /**
   * @return the lifetime average velocity
   */
  @Override
  public double avgVelocity () {

    return avgVelocity;
  }

  /**
   * @return the one minute average velocity
   */
  @Override
  public double avgVelocity_1 () {

    return avgVelocity_1;
  }

  /**
   * @return the five minute average velocity
   */
  @Override
  public double avgVelocity_5 () {

    return avgVelocity_5;
  }

  /**
   * @return the fifteen minute average velocity
   */
  @Override
  public double avgVelocity_15 () {

    return avgVelocity_15;
  }
}
