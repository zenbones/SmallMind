package org.smallmind.scribe.pen.probe;

public class RangeMetric<N extends Number & Comparable<Number>> extends Metric {

   public void includeValue (N number)
      throws ProbeException {

      try {
         if ((!containsKey("low")) || (number.compareTo((Number)getData("low")) < 0)) {
            setData("low", number);
         }

         if ((!containsKey("high")) || (number.compareTo((Number)getData("high")) > 0)) {
            setData("high", number);
         }
      }
      catch (Exception exception) {
         throw new ProbeException(exception);
      }
   }
}

