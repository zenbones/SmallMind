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
import javax.swing.JPanel;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.ReadableParaboxConstraint;

public class ParaboxPanel extends JPanel {

  public ParaboxPanel () {

    this(Bias.HORIZONTAL);
  }

  public ParaboxPanel (Bias bias) {

    this(bias, Gap.RELATED);
  }

  public ParaboxPanel (Bias bias, Gap gap) {

    this(bias, gap, Alignment.LEADING, Alignment.CENTER);
  }

  public ParaboxPanel (Bias bias, double gap) {

    this(bias, gap, Alignment.LEADING, Alignment.CENTER);
  }

  public ParaboxPanel (Bias bias, Gap gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    super(new ParaboxLayoutManager(bias, gap, biasedAlignment, unbiasedAlignment));
  }

  public ParaboxPanel (Bias bias, double gap, Alignment biasedAlignment, Alignment unbiasedAlignment) {

    super(new ParaboxLayoutManager(bias, gap, biasedAlignment, unbiasedAlignment));
  }

  public ParaboxPanel addComponent (Component component) {

    add(component);

    return this;
  }

  public ParaboxPanel addComponent (Component component, ReadableParaboxConstraint constraint) {

    add(component, constraint);

    return this;
  }
}
