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
package org.smallmind.nutsnbolts.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

/**
 * Coordinates a single layout pass by caching element measurements, tracking partial axis solutions,
 * and enforcing that every component appears in exactly one horizontal and one vertical box.
 */
public class LayoutTailor {

  private final HashMap<Sizing, Double> measurementMap = new HashMap<Sizing, Double>();
  private final HashMap<Object, PartialSolution> solutionMap = new HashMap<Object, PartialSolution>();
  private final HashSet<Object> completedSet = new HashSet<Object>();

  /**
   * Initializes the tailor by pre-registering all components that are expected to receive layout constraints,
   * allowing {@link #cleanup()} to detect any that were never placed.
   *
   * @param componentList the complete list of platform components participating in this layout pass
   */
  public LayoutTailor (List<?> componentList) {

    for (Object component : componentList) {
      solutionMap.put(component, null);
    }
  }

  /**
   * Caches a measurement for the given part, axis, and measurement type so that repeated
   * queries for the same combination avoid redundant computation.
   *
   * @param part        the layout part whose measurement is being stored
   * @param bias        the axis to which the measurement applies
   * @param tapeMeasure the category of measurement (minimum, preferred, or maximum)
   * @param measurement the computed measurement value to cache
   */
  public void store (Object part, Bias bias, TapeMeasure tapeMeasure, double measurement) {

    measurementMap.put(new Sizing(part, bias, tapeMeasure), measurement);
  }

  /**
   * Retrieves a previously cached measurement for the given part, axis, and measurement type.
   *
   * @param part        the layout part whose measurement is requested
   * @param bias        the axis of the requested measurement
   * @param tapeMeasure the category of measurement (minimum, preferred, or maximum)
   * @return the cached measurement, or {@code null} if no value has been stored for this combination
   */
  public Double lookup (Object part, Bias bias, TapeMeasure tapeMeasure) {

    return measurementMap.get(new Sizing(part, bias, tapeMeasure));
  }

  /**
   * Records the resolved position and size for an element along one axis, and for planar elements
   * invokes the final two-axis placement once both the horizontal and vertical constraints are known.
   * Linear elements are placed immediately; planar elements must be visited by both a horizontal and
   * a vertical box before placement occurs.
   *
   * @param bias        the axis being resolved in this call
   * @param position    the starting offset along the axis
   * @param measurement the allocated size along the axis
   * @param element     the element to place
   * @throws LayoutException if a box or component is placed in more than one parent along the same axis,
   *                         or if a box element is reused across multiple parent boxes
   */
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
        } else if (partialSolution.bias().equals(bias)) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical box, and no more", element.getPart());
        } else {
          switch (bias) {
            case HORIZONTAL:
              ((PlanarPart)element).applyLayout(new Pair(position, partialSolution.position()), new Pair(measurement, partialSolution.measurement()));
              break;
            case VERTICAL:
              ((PlanarPart)element).applyLayout(new Pair(partialSolution.position(), position), new Pair(partialSolution.measurement(), measurement));
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

  /**
   * Verifies that every pre-registered component received constraints along both axes during the layout pass,
   * throwing a {@link LayoutException} describing the first component found to be incompletely placed.
   *
   * @throws LayoutException if any component was never added to any box, or was added to only one axis's box
   */
  public void cleanup () {

    if (!solutionMap.isEmpty()) {

      Iterator<Map.Entry<Object, PartialSolution>> solutionEntryIter = solutionMap.entrySet().iterator();

      if (solutionEntryIter.hasNext()) {

        Map.Entry<Object, PartialSolution> solutionEntry = solutionEntryIter.next();

        if (solutionEntry.getValue() == null) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical box", solutionEntry.getKey());
        } else {
          throw new LayoutException("The layout component (%s) was only added to a %s box, and must be constrained in both directions", solutionEntry.getKey(), solutionEntry.getValue().bias());
        }
      }
    }
  }
}
