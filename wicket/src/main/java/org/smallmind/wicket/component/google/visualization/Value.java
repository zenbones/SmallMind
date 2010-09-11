package org.smallmind.wicket.component.google.visualization;

public abstract class Value implements Comparable<Value> {

   public abstract ValueType getType ();

   public abstract boolean isNull ();

   public boolean equals (Object obj) {

      return (obj instanceof Value) && (getType().equals(((Value)obj).getType())) && (this.compareTo((Value)obj) == 0);
   }
}
