/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.component.tabs;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

public class Tab extends Panel {

  private TabPanel tabPanel;
  private ITab tab;
  private WebMarkupContainer tabSkin;
  private boolean selected;
  private int index;

  public Tab (String id, TabPanel tabPanel, ITab tab, int index, boolean selected) {

    super(id);

    WebMarkupContainer outerSpan;
    Label innerSpan;

    this.tabPanel = tabPanel;
    this.tab = tab;
    this.index = index;
    this.selected = selected;

    setOutputMarkupId(true);

    innerSpan = new Label("innerSpan", tab.getTitle());

    outerSpan = new WebMarkupContainer("outerSpan");
    outerSpan.add(innerSpan);

    add(tabSkin = new WebMarkupContainer("tabSkin"));
    tabSkin.setOutputMarkupId(true);
    tabSkin.add(outerSpan);
    tabSkin.add(new AttributeModifier("class", new TabClassModel()));
    tabSkin.add(new AttributeModifier("selected", new TabSelectedModel()));

    tabSkin.add(new OnClickAjaxEventBehavior());
  }

  public String getInnerMarkupId () {

    return tabSkin.getMarkupId();
  }

  public WebMarkupContainer getPanel (String id) {

    return tab.getPanel(id);
  }

  public synchronized Tab setSelected (boolean selected) {

    this.selected = selected;

    return this;
  }

  public synchronized boolean isSelected () {

    return selected;
  }

  private class TabClassModel extends AbstractReadOnlyModel {

    public Object getObject () {

      return (isSelected()) ? "tabselected" : "tabstandard";
    }
  }

  private class TabSelectedModel extends AbstractReadOnlyModel {

    public Object getObject () {

      return (isSelected()) ? "selected" : null;
    }
  }

  private class OnClickAjaxEventBehavior extends AjaxEventBehavior {

    public OnClickAjaxEventBehavior () {

      super("onClick");
    }

    protected void onEvent (final AjaxRequestTarget target) {

      tabPanel.setSelectedIndex(target, index);
    }
  }
}