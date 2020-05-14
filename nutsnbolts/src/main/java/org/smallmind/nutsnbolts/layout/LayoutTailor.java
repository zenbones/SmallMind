/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.nutsnbolts.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class LayoutTailor {

  private final HashMap<Sizing, Double> measurementMap = new HashMap<Sizing, Double>();
  private final HashMap<Object, PartialSolution> solutionMap = new HashMap<Object, PartialSolution>();
  private final HashSet<Object> completedSet = new HashSet<Object>();

  public LayoutTailor (List<?> componentList) {

    for (Object component : componentList) {
      solutionMap.put(component, null);
    }
  }

  public void store (Object part, Bias bias, TapeMeasure tapeMeasure, double measurement) {

    measurementMap.put(new Sizing(part, bias, tapeMeasure), measurement);
  }

  public Double lookup (Object part, Bias bias, TapeMeasure tapeMeasure) {

    return measurementMap.get(new Sizing(part, bias, tapeMeasure));
  }

  public void applyLayout (Bias bias, double position, double measurement, ParaboxElement<?> element) {

    switch (element.getDimensionality()) {
      case LINE:

        if (completedSet.contains(element.getPart())) {
          throw new LayoutException("No layout box may be added to more than a single parent (do not store or otherwise re-use boxes)");
        }

        ((LinearPart)element).applyLayout(bias, position, measurement, this);
        completedSet.add(element.getPart());
        break;
      case PLANE:

        PartialSolution partialSolution;

        if (completedSet.contains(element.getPart())) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical box, and no more", element.getPart());
        }

        if ((partialSolution = solutionMap.remove(element.getPart())) == null) {
          solutionMap.put(element.getPart(), new PartialSolution(bias, position, measurement));
        } else if (partialSolution.getBias().equals(bias)) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical box, and no more", element.getPart());
        } else {
          switch (bias) {
            case HORIZONTAL:
              ((PlanarPart)element).applyLayout(new Pair(position, partialSolution.getPosition()), new Pair(measurement, partialSolution.getMeasurement()));
              break;
            case VERTICAL:
              ((PlanarPart)element).applyLayout(new Pair(partialSolution.getPosition(), position), new Pair(partialSolution.getMeasurement(), measurement));
              break;
            default:
              throw new UnknownSwitchCaseException(bias.name());
          }

          completedSet.add(element.getPart());
        }
        break;
      default:
        throw new UnknownSwitchCaseException(element.getDimensionality().name());
    }
  }

  public void cleanup () {

    if (!solutionMap.isEmpty()) {

      Iterator<Map.Entry<Object, PartialSolution>> solutionEntryIter = solutionMap.entrySet().iterator();

      if (solutionEntryIter.hasNext()) {

        Map.Entry<Object, PartialSolution> solutionEntry = solutionEntryIter.next();

        if (solutionEntry.getValue() == null) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical box", solutionEntry.getKey());
        } else {
          throw new LayoutException("The layout component (%s) was only added to a %s box, and must be constrained in both directions", solutionEntry.getKey(), solutionEntry.getValue().getBias());
        }
      }
    }
  }
}
