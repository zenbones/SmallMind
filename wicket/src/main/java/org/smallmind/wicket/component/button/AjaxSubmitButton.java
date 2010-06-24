package org.smallmind.wicket.component.button;

import org.smallmind.wicket.skin.SkinManager;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public abstract class AjaxSubmitButton extends Button {

   public AjaxSubmitButton (String id, IModel labelModel, Form form, SkinManager skinManager) {

      super(id, labelModel, skinManager);

      addButtonBehavior(new OnClickAjaxFormSubmitBehavior(form, "onclick"));
   }

   public abstract void onSubmit (AjaxRequestTarget target, Form form);

   public void onError (AjaxRequestTarget target, Form form) {
   }

   private class OnClickAjaxFormSubmitBehavior extends AjaxFormSubmitBehavior {

      public OnClickAjaxFormSubmitBehavior (Form form, String event) {

         super(form, event);
      }

      protected void onEvent (AjaxRequestTarget target) {

         if (AjaxSubmitButton.this.isEnabled()) {
            super.onEvent(target);
         }
      }

      public void onSubmit (AjaxRequestTarget target) {

         AjaxSubmitButton.this.onSubmit(target, getForm());
      }

      public void onError (AjaxRequestTarget target) {

         AjaxSubmitButton.this.onError(target, getForm());
      }
   }
}
