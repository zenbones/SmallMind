package org.smallmind.swing.spinner;

import javax.swing.SpinnerModel;

public interface EdgeAwareSpinnerModel extends SpinnerModel {

   public Object getMinimumValue ();

   public Object getMaximumValue ();

}
