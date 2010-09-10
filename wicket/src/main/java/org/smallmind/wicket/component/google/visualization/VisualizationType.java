package org.smallmind.wicket.component.google.visualization;

import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;

public enum VisualizationType {

   Table("google.visualization.Table", "google.load('visualization', '" + VisualizationInfo.getVersion() + "', {'packages':['table']});");

   private String jsonClass;
   private String initializationScript;

   private VisualizationType (String jsonClass, String initializationScript) {

      this.jsonClass = jsonClass;
      this.initializationScript = initializationScript;
   }

   public String getJsonClass () {

      return jsonClass;
   }

   public String getInitializationScript () {

      return initializationScript;
   }

   public IBehavior getInitializationBehavior () {

      return new AbstractBehavior() {

         @Override
         public void renderHead (IHeaderResponse response) {
            super.renderHead(response);

            response.renderJavascript(getInitializationScript(), VisualizationType.class.getName() + '.' + VisualizationType.this.name());
         }
      };
   }
}
