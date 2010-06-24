package org.smallmind.wicket.component.button;

import java.util.Properties;
import org.smallmind.wicket.behavior.CssBehavior;
import org.smallmind.wicket.skin.SkinManager;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;

public class Button extends Panel {

   private WebMarkupContainer buttonSkin;

   public Button (String id, IModel labelModel, SkinManager skinManager) {

      super(id);

      Properties cssProperties;

      setOutputMarkupId(true);

      add(buttonSkin = new WebMarkupContainer("buttonSkin"));
      buttonSkin.setOutputMarkupId(true);
      buttonSkin.add(new Label("buttonText", labelModel));
      buttonSkin.add(new AttributeModifier("class", true, new ButtonClassModel()));
      buttonSkin.add(new AttributeModifier("incapacitated", true, new ButtonDisabledModel()));

      add(HeaderContributor.forJavaScript(Button.class, "Button.js"));

      cssProperties = skinManager.getProperties((WebApplication)getApplication(), Button.class);
      cssProperties.put("contextpath", ((WebApplication)getApplication()).getServletContext().getContextPath());
      add(new CssBehavior(Button.class, "Button.css", cssProperties));
   }

   public String getInnerMarkupId () {

      return buttonSkin.getMarkupId();
   }

   protected void addButtonBehavior (IBehavior behavior) {

      buttonSkin.add(behavior);
   }

   private class ButtonClassModel extends AbstractReadOnlyModel {

      public Object getObject () {

         return (!isEnabled()) ? "buttondisabled" : "buttonstandard";
      }
   }

   private class ButtonDisabledModel extends AbstractReadOnlyModel {

      public Object getObject () {

         return (!isEnabled()) ? "true" : "false";
      }
   }
}