package org.smallmind.wicket.component.button;

import org.smallmind.wicket.skin.SkinManager;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public abstract class AjaxButton extends Button {

   public AjaxButton (String id, IModel labelModel, SkinManager skinManager) {

      super(id, labelModel, skinManager);

      addButtonBehavior(new OnClickAjaxEventBehavior());
   }

   public abstract void onClick (final AjaxRequestTarget target);

   private class OnClickAjaxEventBehavior extends AjaxEventBehavior {

      public OnClickAjaxEventBehavior () {

         super("onClick");
      }

      protected void onEvent (final AjaxRequestTarget target) {

         if (AjaxButton.this.isEnabled()) {
            onClick(target);
         }
      }
   }
}