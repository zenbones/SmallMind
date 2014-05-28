/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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

import java.util.HashSet;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.smallmind.wicket.behavior.JavaScriptNamespaceBehavior;

public abstract class VisualizationBorder extends Border {

  private HashSet<String> panelIdSet = new HashSet<String>();

  public VisualizationBorder (String id) {

    super(id);

    Label scriptLabel;

    add(new JavaScriptNamespaceBehavior("SMALLMIND.visualization.flot." + getMarkupId()));
    add(new Behavior() {

      @Override
      public void renderHead (Component component, IHeaderResponse response) {

        super.renderHead(component, response);
        response.render(JavaScriptHeaderItem.forScript("google.load('visualization', '" + VisualizationInfo.getVersion() + "');", VisualizationBorder.class.getName()));
      }
    });

    scriptLabel = new Label("visualizationBorderScript", new VisualizationBorderScriptModel());
    scriptLabel.setEscapeModelStrings(false);

    add(scriptLabel);
  }

  public abstract DataTable getDataTable ();

  protected void addRenderingPanel (String id) {

    panelIdSet.add(id);
  }

  public void loadData (AjaxRequestTarget target) {

    target.appendJavaScript(buildTableScript());
    target.appendJavaScript("SMALLMIND.visualization.flot." + getMarkupId() + ".loadData();");
  }

  private String buildTableScript () {

    DataTable dataTable;
    StringBuilder scriptBuilder = new StringBuilder();
    StringBuilder rowBuilder;
    int columnIndex = 0;

    dataTable = getDataTable();

    scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append('.').append("data = new google.visualization.DataTable();");

    for (ColumnDescription columnDescription : dataTable.getColumnDescriptions()) {
      scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".data.addColumn('").append(columnDescription.getType().getScriptVersion()).append("','").append(columnDescription.getLabel()).append("','").append(columnDescription.getId()).append("');");
      if (columnDescription.hasProperties()) {
        scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".data.setColumnProperties(").append(columnIndex).append(',').append(columnDescription.getPropertiesAsJson()).append(");");
      }
      columnIndex++;
    }

    for (TableRow tableRow : dataTable.getRows()) {
      rowBuilder = new StringBuilder("[");
      for (TableCell tableCell : tableRow.getCells()) {
        if (rowBuilder.length() > 1) {
          rowBuilder.append(',');
        }

        if ((tableCell.getFormattedValue() == null) && (!tableCell.hasProperties())) {
          rowBuilder.append(tableCell.getValue().forScript());
        }
        else {
          rowBuilder.append("{v: ").append(tableCell.getValue().forScript());

          if (tableCell.getFormattedValue() != null) {
            rowBuilder.append(",f: '").append(tableCell.getFormattedValue()).append('\'');
          }

          if (tableCell.hasProperties()) {
            rowBuilder.append(",p: ").append(tableCell.getPropertiesAsJson());
          }

          rowBuilder.append('}');
        }
      }
      rowBuilder.append("]");

      scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".data.addRow(").append(rowBuilder).append(");");
    }

    return scriptBuilder.toString();
  }

  private class VisualizationBorderScriptModel extends AbstractReadOnlyModel<String> {

    @Override
    public String getObject () {

      StringBuilder scriptBuilder = new StringBuilder();

      scriptBuilder.append(buildTableScript());
      scriptBuilder.append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".loadData  = function () {");

      for (String panelId : panelIdSet) {
        scriptBuilder.append("SMALLMIND.visualization.flot.").append(panelId).append(".drawChart(").append("SMALLMIND.visualization.flot.").append(getMarkupId()).append('.').append("data);");
      }

      scriptBuilder.append("};");
      scriptBuilder.append("google.setOnLoadCallback(").append("SMALLMIND.visualization.flot.").append(getMarkupId()).append(".loadData);");

      panelIdSet.clear();

      return scriptBuilder.toString();
    }
  }
}
