package org.smallmind.wicket.model;

import java.text.MessageFormat;
import org.apache.wicket.Component;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;

public class FormattedResourceModel extends AbstractReadOnlyModel {

   private Component component;
   private Object[] args;
   private String key;

   public FormattedResourceModel (Component component, String key, Object... args) {

      this.component = component;
      this.key = key;
      this.args = args;
   }

   public Object getObject () {

      if ((args == null) || args.length == 0) {
         return component.getApplication().getResourceSettings().getLocalizer().getString(key, component);
      }

      Object[] unwrappedArgs = new Object[args.length];

      for (int count = 0; count < args.length; count++) {
         if (args[count] instanceof Model) {
            unwrappedArgs[count] = ((Model)args[count]).getObject();
         }
         else {
            unwrappedArgs[count] = args[count];
         }
      }

      return MessageFormat.format(component.getApplication().getResourceSettings().getLocalizer().getString(key, component), unwrappedArgs);
   }

   public String toString () {

      return getObject().toString();
   }
}
