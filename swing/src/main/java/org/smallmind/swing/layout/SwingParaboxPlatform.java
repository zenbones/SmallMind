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
package org.smallmind.swing.layout;

import java.awt.ComponentOrientation;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Flow;
import org.smallmind.nutsnbolts.layout.Orientation;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.Perimeter;

public class SwingParaboxPlatform implements ParaboxPlatform {

  private static final Perimeter PERIMETER;
  private static final Orientation ORIENTATION;
  private static final double RELATED_GAP;
  private static final double UNRELATED_GAP;

  static {

    ComponentOrientation componentOrientation = ComponentOrientation.getOrientation(Locale.getDefault());
    LayoutStyle layoutStyle = LayoutStyle.getInstance();
    JButton button = new JButton();
    JTextField textField = new JTextField();
    double containerGap;

    ORIENTATION = new Orientation(componentOrientation.isHorizontal() ? Bias.HORIZONTAL : Bias.VERTICAL, componentOrientation.isLeftToRight() ? Flow.FIRST_TO_LAST : Flow.LAST_TO_FIRST);

    containerGap = layoutStyle.getContainerGap(button, SwingConstants.EAST, null);

    RELATED_GAP = layoutStyle.getPreferredGap(button, textField, LayoutStyle.ComponentPlacement.RELATED, SwingConstants.EAST, null);
    UNRELATED_GAP = layoutStyle.getPreferredGap(button, textField, LayoutStyle.ComponentPlacement.UNRELATED, SwingConstants.EAST, null);
    PERIMETER = new Perimeter(containerGap, containerGap, containerGap, containerGap);
  }

  @Override
  public double getRelatedGap () {

    return RELATED_GAP;
  }

  @Override
  public double getUnrelatedGap () {

    return UNRELATED_GAP;
  }

  @Override
  public Perimeter getFramePerimeter () {

    return PERIMETER;
  }

  @Override
  public Orientation getOrientation () {

    return ORIENTATION;
  }
}
