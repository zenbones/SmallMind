package org.smallmind.swing.event;

import java.util.EventListener;

public interface MultiListSelectionListener<T extends Comparable<T>> extends EventListener {

   public abstract T getKey ();

   public abstract void valueChanged (MultiListSelectionEvent selectionEvent);

}
