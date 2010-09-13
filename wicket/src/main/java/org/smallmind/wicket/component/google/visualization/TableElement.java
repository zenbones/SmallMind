package org.smallmind.wicket.component.google.visualization;

public class TableElement {

   private String properties;

   public synchronized void setProperties (String properties) {

      this.properties = properties;
   }

   public boolean hasProperties () {

      return properties != null;
   }

   public synchronized String getPropertiesAsJson () {

      return (properties == null) ? "{}" : properties.toString();
   }
}
