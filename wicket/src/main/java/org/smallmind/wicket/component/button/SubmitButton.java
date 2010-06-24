package org.smallmind.wicket.component.button;

import org.smallmind.wicket.skin.SkinManager;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

public class SubmitButton extends Button {

   public SubmitButton (String id, IModel labelModel, Form form, SkinManager skinManager) {

      super(id, labelModel, skinManager);

      addButtonBehavior(new AttributeModifier("onclick", true, new FormSubmitActionModel(form)));
   }

   private class FormSubmitActionModel extends AbstractReadOnlyModel {

      private Form form;

      public FormSubmitActionModel (Form form) {

         this.form = form;
      }

      public Object getObject () {

         if (!SubmitButton.this.isEnabled()) {
            return "";
         }

         return "SMALLMIND.component.button.submit('" + form.getMarkupId() + "')";
      }
   }
}