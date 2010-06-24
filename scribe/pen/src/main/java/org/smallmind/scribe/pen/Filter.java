package org.smallmind.scribe.pen;

public interface Filter {

   public abstract boolean willLog (Record record);
}