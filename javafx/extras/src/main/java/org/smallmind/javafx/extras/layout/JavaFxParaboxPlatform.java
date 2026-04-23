/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.javafx.extras.layout;

import org.smallmind.nutsnbolts.layout.Bias;
import org.smallmind.nutsnbolts.layout.Flow;
import org.smallmind.nutsnbolts.layout.Orientation;
import org.smallmind.nutsnbolts.layout.ParaboxPlatform;
import org.smallmind.nutsnbolts.layout.Perimeter;

/**
 * JavaFX-specific implementation of {@link ParaboxPlatform} that supplies the default spacing and
 * frame padding values used by the parabox layout engine when no explicit overrides are provided.
 * The defaults reflect standard JavaFX UI conventions: a 10 px frame perimeter, 5 px related gap,
 * and 10 px unrelated gap, with a horizontal-first, left-to-right orientation.
 */
public class JavaFxParaboxPlatform implements ParaboxPlatform {

  private static final Perimeter PERIMETER = new Perimeter(10.0D, 10.0D, 10.0D, 10.0D);
  private static final Orientation ORIENTATION = new Orientation(Bias.HORIZONTAL, Flow.FIRST_TO_LAST);

  /**
   * Returns the default gap in pixels between logically related components.
   *
   * @return 5.0 pixels
   */
  @Override
  public double getRelatedGap () {

    return 5.0D;
  }

  /**
   * Returns the default gap in pixels between logically unrelated components.
   *
   * @return 10.0 pixels
   */
  @Override
  public double getUnrelatedGap () {

    return 10.0D;
  }

  /**
   * Returns the default frame perimeter — 10 px of padding on all four sides — applied by
   * {@link ParaboxPane} when no explicit insets are specified.
   *
   * @return the frame perimeter; never {@code null}
   */
  @Override
  public Perimeter getFramePerimeter () {

    return PERIMETER;
  }

  /**
   * Returns the default layout orientation: horizontal primary axis with left-to-right (first-to-
   * last) flow.
   *
   * @return the orientation; never {@code null}
   */
  @Override
  public Orientation getOrientation () {

    return ORIENTATION;
  }
}
