package org.smallmind.wicket.component.radio;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;

public abstract class AjaxRadio<E> extends Radio<E> {

   public AjaxRadio (String id) {

      this(id, null, null);
   }

   public AjaxRadio (String id, IModel<E> model) {

      this(id, model, null);
   }

   public AjaxRadio (String id, RadioGroup<E> group) {

      this(id, null, group);
   }

   public AjaxRadio (String id, IModel<E> model, RadioGroup<E> group) {

      super(id, model, group);

      add(new OnClickAjaxEventBehavior());
   }

   public abstract void onClick (E selection, AjaxRequestTarget target);

   private class OnClickAjaxEventBehavior extends AjaxEventBehavior {

      public OnClickAjaxEventBehavior () {

         super("onClick");
      }

      protected void onEvent (AjaxRequestTarget target) {

         if (AjaxRadio.this.isEnabled()) {
            onClick(AjaxRadio.this.getModel().getObject(), target);
         }
      }
   }
}
