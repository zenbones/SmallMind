/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.extras.instrument;

public class Measure {

  private double avgRate;
  private double avgRate_1;
  private double avgRate_5;
  private double avgRate_15;

  public Measure (double avgRate, double avgRate_1, double avgRate_5, double avgRate_15) {

    this.avgRate = avgRate;
    this.avgRate_1 = avgRate_1;
    this.avgRate_5 = avgRate_5;
    this.avgRate_15 = avgRate_15;
  }

  public double getAvgRate () {

    return avgRate;
  }

  public double getAvgRate_1 () {

    return avgRate_1;
  }

  public double getAvgRate_5 () {

    return avgRate_5;
  }

  public double getAvgRate_15 () {

    return avgRate_15;
  }
}
