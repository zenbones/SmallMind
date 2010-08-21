package org.smallmind.swing.banner;

import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

public class BannerListModel implements ListModel {

   private Object[] elements;

   public void addListDataListener (ListDataListener listDataListener) {
   }

   public void removeListDataListener (ListDataListener listDataListener) {
   }

   public BannerListModel (Object[] elements) {

      this.elements = elements;
   }

   public int getSize () {
      return elements.length;
   }

   public Object getElementAt (int index) {
      return elements[index];
   }

}
