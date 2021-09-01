/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.component.button;

import java.util.Properties;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.smallmind.wicket.behavior.CssBehavior;
import org.smallmind.wicket.behavior.JavaScriptBehavior;
import org.smallmind.wicket.skin.SkinManager;

public class Button extends Panel {

  private final WebMarkupContainer buttonSkin;

  public Button (String id, IModel<?> labelModel, SkinManager skinManager) {

    super(id);

    Properties cssProperties;

    setOutputMarkupId(true);

    add(buttonSkin = new WebMarkupContainer("buttonSkin"));
    buttonSkin.setOutputMarkupId(true);
    buttonSkin.add(new Label("buttonText", labelModel));
    buttonSkin.add(new AttributeModifier("class", new ButtonClassModel()));
    buttonSkin.add(new AttributeModifier("incapacitated", new ButtonDisabledModel()));

    add(new JavaScriptBehavior(Button.class, "Button.js", null));

    cssProperties = skinManager.getProperties((WebApplication)getApplication(), Button.class);
    cssProperties.put("contextpath", ((WebApplication)getApplication()).getServletContext().getContextPath());
    add(new CssBehavior(Button.class, "Button.css", cssProperties));
  }

  public String getInnerMarkupId () {

    return buttonSkin.getMarkupId();
  }

  protected void addButtonBehavior (Behavior behavior) {

    buttonSkin.add(behavior);
  }

  private class ButtonClassModel implements IModel<String> {

    public String getObject () {

      return (!isEnabled()) ? "buttondisabled" : "buttonstandard";
    }
  }

  private class ButtonDisabledModel implements IModel<String> {

    public String getObject () {

      return (!isEnabled()) ? "true" : "false";
    }
  }
}