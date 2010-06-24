package org.smallmind.wicket.component.tabs;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.smallmind.wicket.behavior.CssBehavior;
import org.smallmind.wicket.skin.SkinManager;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.WebApplication;

public class TabPanel extends Panel {

   private SelectionChangedCallback selectionChangedCallback;
   private LinkedList<Tab> tabList;
   private WebMarkupContainer tabContents;
   private int selectedIndex = 0;

   public TabPanel (String id, SkinManager skinManager) {

      this(id, 0, skinManager);
   }

   public TabPanel (String id, int selectedIndex, SkinManager skinManager) {

      super(id);

      Properties cssProperties;

      this.selectedIndex = selectedIndex;

      tabList = new LinkedList<Tab>();

      setOutputMarkupId(true);

      add(new TabListView("tabCell", tabList));

      tabContents = new WebMarkupContainer("tabContents");
      tabContents.setOutputMarkupId(true);
      add(tabContents);

      add(HeaderContributor.forJavaScript(Tab.class, "Tab.js"));

      cssProperties = skinManager.getProperties((WebApplication)getApplication(), TabPanel.class);
      cssProperties.put("contextpath", ((WebApplication)getApplication()).getServletContext().getContextPath());
      add(new CssBehavior(Tab.class, "Tab.css", cssProperties));
   }

   public synchronized void setSelectionChangedCallback (SelectionChangedCallback selectionChangedCallback) {

      this.selectionChangedCallback = selectionChangedCallback;
   }

   public synchronized void addTab (ITab tab) {

      if (tabList.size() == selectedIndex) {
         tabContents.add(tab.getPanel("tabPanel"));
      }

      tabList.add(new Tab("tab", this, tab, tabList.size(), tabList.size() == selectedIndex));
   }

   public synchronized void setSelectedIndex (final AjaxRequestTarget target, int index) {

      Tab selectedTab;

      target.addComponent(tabList.get(selectedIndex).setSelected(false));
      selectedTab = tabList.get(index).setSelected(true);
      tabContents.replace(selectedTab.getPanel("tabPanel"));

      target.addComponent(tabContents);
      target.addComponent(selectedTab);

      selectedIndex = index;

      if (selectionChangedCallback != null) {
         selectionChangedCallback.onSelectionChanged(target, index);
      }
   }

   private class TabListView extends ListView {

      public TabListView (String id, List<Tab> tabList) {

         super(id, tabList);

         setReuseItems(true);
      }

      protected void populateItem (ListItem listItem) {

         listItem.add((Tab)listItem.getModelObject());
      }
   }
}
