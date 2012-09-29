package org.smallmind.nutsnbolts.layout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;

public class LayoutTailor {

  private HashMap<Object, PartialSolution> solutionMap = new HashMap<Object, PartialSolution>();
  private HashSet<Object> completedSet = new HashSet<Object>();

  public LayoutTailor (List<?> componentList) {

    for (Object component : componentList) {
      solutionMap.put(component, null);
    }
  }

  public void applyLayout (Bias bias, double position, double measurement, ParaboxElement<?> element) {

    switch (element.getDimensionality()) {
      case LINE:

        if (completedSet.contains(element.getPart())) {
          throw new LayoutException("No layout group may be added to more than a single parent (do not store or otherwise re-use groups)");
        }

        ((LinearPart)element).applyLayout(position, measurement, this);
        completedSet.add(element.getPart());
        break;
      case PLANE:

        PartialSolution partialSolution;

        if (completedSet.contains(element.getPart())) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical group, and no more", element.getPart());
        }

        if ((partialSolution = solutionMap.remove(element.getPart())) == null) {
          solutionMap.put(element.getPart(), new PartialSolution(bias, position, measurement));
        }
        else if (partialSolution.getBias().equals(bias)) {
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical group, and no more", element.getPart());
        }
        else {
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
          throw new LayoutException("The layout component (%s) must be added to a single horizontal and a single vertical group", solutionEntry.getKey());
        }
        else {
          throw new LayoutException("The layout component (%s) was only added to a %s group, and must be constrained in both directions", solutionEntry.getKey(), solutionEntry.getValue().getBias());
        }
      }
    }
  }
}
