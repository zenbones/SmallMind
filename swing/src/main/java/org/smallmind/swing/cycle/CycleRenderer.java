package org.smallmind.swing.cycle;

import java.awt.Component;

public interface CycleRenderer {

   public abstract Component getCycleRendererComponent (Cycle cycle, Object value, int index, boolean selected);

}
