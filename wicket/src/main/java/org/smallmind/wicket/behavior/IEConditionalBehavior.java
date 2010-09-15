package org.smallmind.wicket.behavior;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;

public abstract class IEConditionalBehavior extends AbstractBehavior {

   @Override
   public void beforeRender (Component component) {

      super.beforeRender(component);

      component.getResponse().write("<!--[if IE]>");
   }

   @Override
   public void onRendered (Component component) {

      super.onRendered(component);

      component.getResponse().write("<![endif]-->");
   }
}