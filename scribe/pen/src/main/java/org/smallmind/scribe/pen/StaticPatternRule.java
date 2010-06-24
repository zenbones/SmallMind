package org.smallmind.scribe.pen;

import java.util.Collection;

public class StaticPatternRule implements PatternRule {

   private String staticField;

   public StaticPatternRule (String staticField) {

      this.staticField = staticField;
   }

   public String getHeader () {

      return null;
   }

   public String getFooter () {

      return null;
   }

   public String convert (Record record, Collection<Filter> filterCollection, Timestamp timestamp) {

      return staticField;
   }
}
