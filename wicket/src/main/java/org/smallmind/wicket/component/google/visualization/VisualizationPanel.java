/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.component.google.visualization;

import java.util.UUID;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.smallmind.wicket.FormattedWicketRuntimeException;
import org.smallmind.wicket.behavior.JavaScriptNamespaceBehavior;

public class VisualizationPanel extends Panel {

  public VisualizationPanel (String id, String jsonClass) {

    this(id, null, jsonClass, null);
  }

  public VisualizationPanel (String id, String jsonClass, String options) {

    this(id, null, jsonClass, options);
  }

  public VisualizationPanel (String id, IModel<String> javascriptModel, String jsonClass) {

    this(id, javascriptModel, jsonClass, null);
  }

  public VisualizationPanel (String id, IModel<String> javascriptModel, String jsonClass, String options) {

    super(id);

    Label divLabel;
    Label scriptLabel;
    String divId;

    divId = UUID.randomUUID().toString();

    add(new JavaScriptNamespaceBehavior("SMALLMIND.visualization.flot." + getMarkupId()));

    divLabel = new Label("visualizationDiv", new Model<String>("<div id=\"" + divId + "\"></div>"));
    divLabel.setEscapeModelStrings(false);

    scriptLabel = new Label("visualizationPanelScript", new VisualizationPanelScriptModel(javascriptModel, divId, jsonClass, options));
    scriptLabel.setEscapeModelStrings(false);

    add(divLabel);
    add(scriptLabel);
  }

  public String getJavascriptDataVariable () {

    return "data";
  }

  private class VisualizationPanelScriptModel extends AbstractReadOnlyModel<String> {

    private IModel<String> javascriptModel;
    private String divId;
    private String jsonClass;
    private String options;

    public VisualizationPanelScriptModel (IModel<String> javascriptModel, String divId, String jsonClass, String options) {

      this.javascriptModel = javascriptModel;
      this.divId = divId;
      this.jsonClass = jsonClass;
      this.options = options;
    }

    @Override
    public String getObject () {

      VisualizationBorder visualizationBorder;
      StringBuilder scriptBuilder = new StringBuilder();

      if ((visualizationBorder = findParent(VisualizationBorder.class)) == null) {
        throw new FormattedWicketRuntimeException("%s(%s) is not within the context of a %s", VisualizationPanel.class.getSimpleName(), getMarkupId(), VisualizationBorder.class.getSimpleName());
      }

      scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".drawChart  = function (data) {");

      if (javascriptModel != null) {
        scriptBuilder.append(javascriptModel.getObject());
      }

      scriptBuilder.append("var chart = new ").append(jsonClass).append("(document.getElementById('").append(divId).append("'));");
      scriptBuilder.append("chart.draw(").append(getJavascriptDataVariable()).append(", ").append((options == null) ? "{}" : options).append(");");
      scriptBuilder.append('}');

      visualizationBorder.addRenderingPanel(getMarkupId());

      return scriptBuilder.toString();
    }
  }
}
