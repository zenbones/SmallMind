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

  private static final ParaboxConstraint IMMUTABLE_RIGID_INSTANCE = new ParaboxConstraint();
  private static final ParaboxConstraint IMMUTABLE_GROW_INSTANCE = new ParaboxConstraint(0.5D, 0.5D, 0.0D, 0.0D);
  private static final ParaboxConstraint IMMUTABLE_GROW_X_INSTANCE = new ParaboxConstraint(0.5D, 0.0D, 0.0D, 0.0D);
  private static final ParaboxConstraint IMMUTABLE_GROW_Y_INSTANCE = new ParaboxConstraint(0.0D, 0.5D, 0.0D, 0.0D);
  private static final ParaboxConstraint IMMUTABLE_SHRINK_INSTANCE = new ParaboxConstraint(0.0D, 0.0D, 0.5D, 0.5D);
  private static final ParaboxConstraint IMMUTABLE_SHRINK_X_INSTANCE = new ParaboxConstraint(0.0D, 0.0D, 0.5D, 0.0D);
  private static final ParaboxConstraint IMMUTABLE_SHRINK_Y_INSTANCE = new ParaboxConstraint(0.0D, 0.0D, 0.0D, 0.5D);
  private static final ParaboxConstraint IMMUTABLE_GROW_AND_SHRINK_INSTANCE = new ParaboxConstraint(0.5D, 0.5D, 0.5D, 0.5D);
  private static final ParaboxConstraint IMMUTABLE_GROW_X_AND_SHRINK_X_INSTANCE = new ParaboxConstraint(0.5D, 0.0D, 0.5D, 0.0D);
  private static final ParaboxConstraint IMMUTABLE_GROW_Y_AND_SHRINK_Y_INSTANCE = new ParaboxConstraint(0.0D, 0.5D, 0.0D, 0.5D);

  private double growX;
  private double growY;
  private double shrinkX;
  private double shrinkY;

  public static ParaboxConstraint immutable () {

    return IMMUTABLE_RIGID_INSTANCE;
  }

  public static ParaboxConstraint expand () {

    return IMMUTABLE_GROW_INSTANCE;
  }

  public static ParaboxConstraint expandX () {

    return IMMUTABLE_GROW_X_INSTANCE;
  }

  public static ParaboxConstraint expandY () {

    return IMMUTABLE_GROW_Y_INSTANCE;
  }

  public static ParaboxConstraint contract () {

    return IMMUTABLE_SHRINK_INSTANCE;
  }

  public static ParaboxConstraint contractX () {

    return IMMUTABLE_SHRINK_X_INSTANCE;
  }

  public static ParaboxConstraint contractY () {

    return IMMUTABLE_SHRINK_Y_INSTANCE;
  }

  public static ParaboxConstraint stretch () {

    return IMMUTABLE_GROW_AND_SHRINK_INSTANCE;
  }

  public static ParaboxConstraint stretchX () {

    return IMMUTABLE_GROW_X_AND_SHRINK_X_INSTANCE;
  }

  public static ParaboxConstraint stretchY () {

    return IMMUTABLE_GROW_Y_AND_SHRINK_Y_INSTANCE;
  }

  public static MutableParaboxConstraint create () {

    return new MutableParaboxConstraint();
  }

  public ParaboxConstraint () {

  }

  public ParaboxConstraint (double growX, double growY, double shrinkX, double shrinkY) {

    this.growX = growX;
    this.growY = growY;
    this.shrinkX = shrinkX;
    this.shrinkY = shrinkY;
  }

  public double getGrowX () {

    return growX;
  }

  private void setGrowX (double growX) {

    this.growX = growX;
  }

  public double getGrowY () {

    return growY;
  }

  private void setGrowY (double growY) {

    this.growY = growY;
  }

  public double getShrinkX () {

    return shrinkX;
  }

  private void setShrinkX (double shrinkX) {

    this.shrinkX = shrinkX;
  }

  public double getShrinkY () {

    return shrinkY;
  }

  private void setShrinkY (double shrinkY) {

    this.shrinkY = shrinkY;
  }
}
