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
package org.smallmind.swing.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import org.smallmind.nutsnbolts.layout.ParaboxLayout;

public class ParaboxLayoutManager implements LayoutManager2 {

  private ParaboxLayout paraboxLayout;

  public ParaboxLayoutManager () {


  }

  @Override
  public void addLayoutComponent (Component comp, Object constraints) {

  }

  @Override
  public Dimension maximumLayoutSize (Container target) {

    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public float getLayoutAlignmentX (Container target) {

    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public float getLayoutAlignmentY (Container target) {

    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void invalidateLayout (Container target) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void addLayoutComponent (String name, Component comp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void removeLayoutComponent (Component comp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Dimension preferredLayoutSize (Container parent) {

    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public Dimension minimumLayoutSize (Container parent) {

    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void layoutContainer (Container parent) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
