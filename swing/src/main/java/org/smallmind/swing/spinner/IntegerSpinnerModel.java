package org.smallmind.swing.spinner;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class IntegerSpinnerModel implements EdgeAwareSpinnerModel {

   private WeakEventListenerList<ChangeListener> listenerList;
   private Integer minimumValue;
   private Integer maximumValue;
   private int value;
   private int increment;

   public IntegerSpinnerModel (int value, int increment, Integer minimumValue, Integer maximumValue) {

      listenerList = new WeakEventListenerList<ChangeListener>();

      this.value = value;
      this.increment = increment;
      this.minimumValue = minimumValue;
      this.maximumValue = maximumValue;
   }

   public void addChangeListener (ChangeListener changeListener) {

      listenerList.addListener(changeListener);
   }

   public void removeChangeListener (ChangeListener changeListener) {

      listenerList.removeListener(changeListener);
   }

   public Object getMinimumValue () {

      return minimumValue;
   }

   public Object getMaximumValue () {

      return maximumValue;
   }

   public Object getValue () {

      return value;
   }

   public void setValue (Object value) {

      ChangeEvent changeEvent;

      this.value = (Integer)value;

      changeEvent = new ChangeEvent(this);
      for (ChangeListener changeListener : listenerList) {
         changeListener.stateChanged(changeEvent);
      }
   }

   public Object getNextValue () {

      return value + increment;
   }

   public Object getPreviousValue () {

      return value - increment;
   }

}
