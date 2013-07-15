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
