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

public abstract class ParaboxElement<C> {

  private C component;
  private ParaboxConstraint constraint;

  public ParaboxElement (C component, ParaboxConstraint constraint) {

    this.component = component;
    this.constraint = constraint;
  }

  public abstract Size getComponentMinimumSize ();

  public abstract Size getComponentPreferredSize ();

  public C getComponent () {

    return component;
  }

  public ParaboxConstraint getConstraint () {

    return constraint;
  }

  public Size getMinimumSize () {

    if ((constraint.getShrinkX() > 0) && (constraint.getShrinkY() > 0)) {

      return getComponentMinimumSize();
    }
    else if (constraint.getShrinkX() > 0) {

      return new Size(getComponentMinimumSize().getWidth(), getComponentPreferredSize().getHeight());
    }
    else if (constraint.getShrinkY() > 0) {

      return new Size(getComponentPreferredSize().getWidth(), getComponentMinimumSize().getHeight());
    }
    else {

      return getComponentPreferredSize();
    }
  }

  public Size getPreferredSize () {

    return getComponentPreferredSize();
  }
}
