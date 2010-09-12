package org.smallmind.wicket.component.button;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.smallmind.wicket.skin.SkinManager;

public abstract class AjaxButton extends Button {

   public AjaxButton (String id, IModel labelModel, SkinManager skinManager) {

      super(id, labelModel, skinManager);

      addButtonBehavior(new OnClickAjaxEventBehavior());
   }

   public abstract void onClick (AjaxRequestTarget target);

   private class OnClickAjaxEventBehavior extends AjaxEventBehavior {

      public OnClickAjaxEventBehavior () {

         super("onClick");
      }

      protected void onEvent (AjaxRequestTarget target) {

         if (AjaxButton.this.isEnabled()) {
            onClick(target);
         }
      }
   }
}