/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.component.google.visualization.flot;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.smallmind.wicket.behavior.JavaScriptNamespaceBehavior;
import org.smallmind.wicket.component.google.visualization.VisualizationPanel;

public class FlotVisualizationPanel extends VisualizationPanel {

  public FlotVisualizationPanel (String id) {

    this(id, null, null);
  }

  public FlotVisualizationPanel (String id, String options) {

    this(id, null, options);
  }

  public FlotVisualizationPanel (String id, IModel<String> JavaScriptModel) {

    this(id, JavaScriptModel, null);
  }

  public FlotVisualizationPanel (String id, IModel<String> JavaScriptModel, String options) {

    super(id, JavaScriptModel, "SMALLMIND.visualization.flot.Flot", options);

    add(new JavaScriptNamespaceBehavior("SMALLMIND.visualization.flot"));
    add(new Behavior() {

      @Override
      public void renderHead (Component component, IHeaderResponse response) {

        super.renderHead(component, response);

        response.renderString("<!--[if IE]>");
        response.renderJavaScriptReference(new JavaScriptResourceReference(FlotVisualizationPanel.class, "api/excanvas.js"));
        response.renderString("<![endif]-->");
      }
    });
    add(new Behavior() {

      @Override
      public void renderHead (Component component, IHeaderResponse response) {

        super.renderHead(component, response);
        response.renderJavaScriptReference(new JavaScriptResourceReference(FlotVisualizationPanel.class, "api/jquery.js"));
        response.renderJavaScriptReference(new JavaScriptResourceReference(FlotVisualizationPanel.class, "api/jquery.flot.js"));
        response.renderJavaScriptReference(new JavaScriptResourceReference(FlotVisualizationPanel.class, "api/jquery.flot.stack.js"));
        response.renderJavaScriptReference(new JavaScriptResourceReference(FlotVisualizationPanel.class, "api/visualization.flot.js"));
      }
    });
  }
}
