/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.layout;

public class ParaboxConstraint {

  private double growX;
  private double growY;
  private double shrinkX;
  private double shrinkY;

  public ParaboxConstraint mayGrow () {

    return mayGrowX().mayGrowY();
  }

  public ParaboxConstraint setGrow (double growX, double growY) {

    return setGrowX(growX).setGrowY(growY);
  }

  public double getGrowX () {

    return growX;
  }

  public ParaboxConstraint mayGrowX () {

    return setGrowX(0.5D);
  }

  public ParaboxConstraint setGrowX (double growX) {

    this.growX = growX;

    return this;
  }

  public ParaboxConstraint mayGrowY () {

    return setGrowY(0.5D);
  }

  public double getGrowY () {

    return growY;
  }

  public ParaboxConstraint setGrowY (double growY) {

    this.growY = growY;

    return this;
  }

  public ParaboxConstraint mayShrink () {

    return mayShrinkX().mayShrinkY();
  }

  public ParaboxConstraint setShrink (double shrinkX, double shrinkY) {

    return setShrinkX(shrinkX).setShrinkY(shrinkY);
  }

  public double getShrinkX () {

    return shrinkX;
  }

  public ParaboxConstraint mayShrinkX () {

    return setShrinkX(0.5D);
  }

  public ParaboxConstraint setShrinkX (double shrinkX) {

    this.shrinkX = shrinkX;

    return this;
  }

  public double getShrinkY () {

    return shrinkY;
  }

  public ParaboxConstraint mayShrinkY () {

    return setShrinkY(0.5D);
  }

  public ParaboxConstraint setShrinkY (double shrinkY) {

    this.shrinkY = shrinkY;

    return this;
  }
}
