/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.swing;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;

public class Colorizer {

  private static final Random RANDOM = new SecureRandom();
  private static final double GOLDEN_RATIO_CONJUGATE = 1 / ((1 + Math.sqrt(5)) / 2);

  private final HashMap<String, Color> colorMap = new HashMap<String, Color>();

  private double hue = RANDOM.nextDouble();

  public synchronized Color getColor (String name) {

    Color color;

    if ((color = colorMap.get(name)) == null) {

      hue += GOLDEN_RATIO_CONJUGATE;
      hue %= 1;

      colorMap.put(name, color = Color.getHSBColor((float)hue, (RANDOM.nextInt(50) + 50) / 100F, (RANDOM.nextInt(25) + 75) / 100F));
    }

    return color;
  }
}
