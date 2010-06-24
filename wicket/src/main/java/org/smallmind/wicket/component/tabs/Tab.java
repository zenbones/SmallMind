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
      tabSkin.add(new AttributeModifier("class", true, new TabClassModel()));
      tabSkin.add(new AttributeModifier("selected", true, new TabSelectedModel()));

      tabSkin.add(new OnClickAjaxEventBehavior());
   }

   public String getInnerMarkupId () {

      return tabSkin.getMarkupId();
   }

   public Panel getPanel (String id) {

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