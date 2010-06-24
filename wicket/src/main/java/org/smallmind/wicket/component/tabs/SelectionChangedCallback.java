package org.smallmind.wicket.component.tabs;

import org.apache.wicket.ajax.AjaxRequestTarget;

public abstract class SelectionChangedCallback {

   public abstract void onSelectionChanged (final AjaxRequestTarget target, int index);
}
