package org.smallmind.wicket.model;

import org.apache.wicket.model.AbstractReadOnlyModel;

public abstract class VisibilityModel extends AbstractReadOnlyModel {

   public abstract boolean isVisible ();

   public Object getObject () {

      return isVisible() ? "visibility: visible; display: block" : "visibility: collapse; display: none";
   }
}