package org.smallmind.wicket.component.label;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.StringResourceModel;

public class FormLabel extends Label {

   public FormLabel (String id, Component parentComponent, FormComponent formComponent, String resourceKey) {

      super(id, new FormLabelModel(parentComponent, resourceKey));

      add(new AttributeAppender("style", true, new FormLabelColorModel(formComponent), ";"));
   }

   private static class FormLabelModel extends StringResourceModel {

      public FormLabelModel (Component parentComponent, String resourceKey) {

         super(resourceKey, parentComponent, null);
      }

      public String getObject () {

         return super.getObject() + ":";
      }
   }

   private class FormLabelColorModel extends AbstractReadOnlyModel {

      private FormComponent formComponent;

      public FormLabelColorModel (FormComponent formComponent) {

         this.formComponent = formComponent;
      }

      public Object getObject () {

         return (formComponent.isValid()) ? "color: #000000" : "color: #FF0000";
      }
   }
}
