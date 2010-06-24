package org.smallmind.nutsnbolts.swing.menu;

import java.util.HashMap;
import java.util.LinkedList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class MenuModel {

   LinkedList<JMenuBar> menuBarList;
   HashMap<String, JMenuItem> menuMap;

   public MenuModel () {

      menuBarList = new LinkedList<JMenuBar>();
      menuMap = new HashMap<String, JMenuItem>();
   }

   public synchronized JMenuBar getMenuBar (int index) {

      return menuBarList.get(index);
   }

   public synchronized JMenuBar addMenuBar () {

      JMenuBar menuBar;

      menuBar = new JMenuBar();
      menuBarList.add(menuBar);

      return menuBar;
   }

   public synchronized void addMenuReference (String menuPath, JMenuItem menuItem) {

      menuMap.put(menuPath, menuItem);
   }

   public synchronized JMenuItem getMenuItem (String menuPath) {

      return menuMap.get(menuPath);
   }

}
