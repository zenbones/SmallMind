/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class Blur {

  private double avgVelocity;
  private double avgVelocity_1;
  private double avgVelocity_5;
  private double avgVelocity_15;

  public Blur (double avgVelocity, double avgVelocity_1, double avgVelocity_5, double avgVelocity_15) {

    this.avgVelocity = avgVelocity;
    this.avgVelocity_1 = avgVelocity_1;
    this.avgVelocity_5 = avgVelocity_5;
    this.avgVelocity_15 = avgVelocity_15;
  }

  public double getAvgVelocity () {

    return avgVelocity;
  }

  public double getAvgVelocity_1 () {

    return avgVelocity_1;
  }

  public double getAvgVelocity_5 () {

    return avgVelocity_5;
  }

  public double getAvgVelocity_15 () {

    return avgVelocity_15;
  }
}
