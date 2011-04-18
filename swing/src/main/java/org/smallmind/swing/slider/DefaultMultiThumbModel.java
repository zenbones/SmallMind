/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing.slider;

import java.util.LinkedList;

public class DefaultMultiThumbModel implements MultiThumbModel {

  private LinkedList<Integer> thumbList;
  private int minimumValue = 0;
  private int maximumValue = 100;

  public DefaultMultiThumbModel () {

    thumbList = new LinkedList<Integer>();
  }

  @Override
  public synchronized void setMinimumValue (int minimumValue) {

    this.minimumValue = minimumValue;
  }

  @Override
  public synchronized int getMinimumValue () {

    return minimumValue;
  }

  @Override
  public synchronized void setMaximumValue (int maximumValue) {

    this.maximumValue = maximumValue;
  }

  @Override
  public synchronized int getMaximumValue () {

    return maximumValue;
  }

  @Override
  public synchronized void addThumb (int thumbValue) {

    thumbList.add(thumbValue);
  }

  @Override
  public synchronized void removeThumb (int thumbIndex) {

    thumbList.remove(thumbIndex);
  }

  @Override
  public synchronized int getThumbCount () {

    return thumbList.size();
  }

  @Override
  public synchronized int getThumbValue (int thumbIndex) {

    return thumbList.get(thumbIndex);
  }

  @Override
  public boolean moveThumb (int thumbIndex, int thumbValue) {

    thumbList.set(thumbIndex, thumbValue);

    return true;
  }
}
