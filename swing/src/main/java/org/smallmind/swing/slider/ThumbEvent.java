/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.swing.slider;

import java.util.EventObject;

public class ThumbEvent extends EventObject {

  public static enum EventType {ADD, REMOVE, MOVE}

  private EventType eventType;
  private boolean adjusting;
  private int thumbIndex;
  private int currentValue;
  private int adjustedValue;

  public ThumbEvent (Object source, EventType eventType, int thumbIndex, int currentValue, int adjustedValue, boolean adjusting) {

    super(source);

    this.eventType = eventType;
    this.thumbIndex = thumbIndex;
    this.currentValue = currentValue;
    this.adjustedValue = adjustedValue;
    this.adjusting = adjusting;
  }

  public EventType getEventType () {

    return eventType;
  }

  public int getThumbIndex () {

    return thumbIndex;
  }

  public int getCurrentValue () {

    return currentValue;
  }

  public int getAdjustedValue () {

    return adjustedValue;
  }

  public boolean isAdjusting () {

    return adjusting;
  }
}
