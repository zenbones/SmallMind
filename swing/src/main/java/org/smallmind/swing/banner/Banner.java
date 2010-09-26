/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.swing.banner;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class Banner extends JComponent implements Scrollable, MouseListener, ListSelectionListener {

   private ListModel listModel;
   private ListSelectionModel listSelectionModel;
   private BannerRenderer bannerRenderer;

   public Banner (ListModel model) {

      ActionMap actionMap;
      InputMap inputMap;

      this.listModel = model;

      listSelectionModel = new DefaultListSelectionModel();
      bannerRenderer = new DefaultBannerRenderer();

      inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      actionMap = getActionMap();

      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "selectLeft");
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "selectRight");
      actionMap.put("selectLeft", new SelectLeftAction());
      actionMap.put("selectRight", new SelectRightAction());

      listSelectionModel.addListSelectionListener(this);
      addMouseListener(this);
   }

   public synchronized ListModel getListModel () {

      return listModel;
   }

   public synchronized ListSelectionModel getListSelectionModel () {

      return listSelectionModel;
   }

   public synchronized BannerRenderer getBannerRenderer () {

      return bannerRenderer;
   }

   public synchronized void setBannerRenderer (BannerRenderer bannerRenderer) {

      this.bannerRenderer = bannerRenderer;
      repaint();
   }

   public synchronized int getIndexAtPoint (Point point) {

      BannerRenderer bannerRenderer;
      Component renderComponent;
      int width = 0;

      bannerRenderer = getBannerRenderer();
      for (int count = 0; count < getListModel().getSize(); count++) {
         renderComponent = bannerRenderer.getBannerRendererComponent(this, getListModel().getElementAt(count), count, false);
         width += (int)renderComponent.getPreferredSize().getWidth();
         if (point.getX() <= width) {
            return count;
         }
      }

      return -1;
   }

   private Rectangle getSquashedRectangleAtIndex (int index) {

      BannerRenderer bannerRenderer;
      Component renderComponent;
      int xPos = 0;

      bannerRenderer = getBannerRenderer();
      for (int count = 0; count < index; count++) {
         renderComponent = bannerRenderer.getBannerRendererComponent(this, getListModel().getElementAt(count), count, false);
         xPos += (int)renderComponent.getPreferredSize().getWidth();
      }

      renderComponent = bannerRenderer.getBannerRendererComponent(this, getListModel().getElementAt(index), index, false);

      return new Rectangle(xPos, 0, (int)renderComponent.getPreferredSize().getWidth(), 0);
   }

   public synchronized void scrollLeft () {

      int leadIndex;

      if ((leadIndex = listSelectionModel.getLeadSelectionIndex()) >= 0) {
         if (leadIndex > 0) {
            scrollToIndex(leadIndex - 1, true);
         }
      }
   }

   public synchronized void scrollRight () {

      int leadIndex;

      if ((leadIndex = listSelectionModel.getLeadSelectionIndex()) >= 0) {
         if (leadIndex < listModel.getSize() - 1) {
            scrollToIndex(leadIndex + 1, true);
         }
      }
   }

   public synchronized void scrollToIndex (int index, boolean select) {

      Rectangle trackingRectangle;
      Rectangle viewRectangle;
      int selectedIndex;

      if (getParent() instanceof JViewport) {
         trackingRectangle = getSquashedRectangleAtIndex(index);
         viewRectangle = ((JViewport)getParent()).getViewRect();
         if ((trackingRectangle.getX() < viewRectangle.getX()) || ((trackingRectangle.getX() + trackingRectangle.getWidth()) > (viewRectangle.getX() + viewRectangle.getWidth()))) {
            selectedIndex = listSelectionModel.getLeadSelectionIndex();
            if ((selectedIndex < 0) || (index <= selectedIndex)) {
               ((JViewport)getParent()).setViewPosition(new Point((int)trackingRectangle.getX(), 0));
            }
            else {
               ((JViewport)getParent()).setViewPosition(new Point((int)(trackingRectangle.getX() + trackingRectangle.getWidth() - viewRectangle.getWidth()), 0));
            }
         }
      }

      if (select) {
         listSelectionModel.setSelectionInterval(index, index);
      }
   }

   public synchronized void valueChanged (ListSelectionEvent listSelectionEvent) {

      repaint();
   }

   public synchronized void mouseClicked (MouseEvent mouseEvent) {

      int anchorIndex;
      int index;

      if ((index = getIndexAtPoint(mouseEvent.getPoint())) >= 0) {
         if (mouseEvent.isShiftDown()) {
            if ((anchorIndex = listSelectionModel.getAnchorSelectionIndex()) < 0) {
               listSelectionModel.setSelectionInterval(index, index);
            }
            else {
               listSelectionModel.setSelectionInterval(anchorIndex, index);
            }
         }
         else if (mouseEvent.isControlDown()) {
            if (listSelectionModel.isSelectedIndex(index)) {
               listSelectionModel.removeSelectionInterval(index, index);
            }
            else {
               listSelectionModel.addSelectionInterval(index, index);
            }
         }
         else {
            listSelectionModel.setSelectionInterval(index, index);
         }

         requestFocusInWindow();
      }

   }

   public void mousePressed (MouseEvent mouseEvent) {
   }

   public void mouseReleased (MouseEvent mouseEvent) {
   }

   public void mouseEntered (MouseEvent mouseEvent) {
   }

   public void mouseExited (MouseEvent mouseEvent) {
   }

   public synchronized Dimension getPreferredScrollableViewportSize () {

      return getPreferredSize();
   }

   public synchronized int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

      Rectangle viewRectangle;
      int index;
      int jiggleJump = 0;

      viewRectangle = ((JViewport)getParent()).getViewRect();

      if (direction < 0) {
         if (viewRectangle.getX() > 0) {
            index = getIndexAtPoint(new Point((int)viewRectangle.getX(), 0));
            jiggleJump = (int)(viewRectangle.getX() - getSquashedRectangleAtIndex(index).getX());
            if (jiggleJump == 0) {
               jiggleJump += getSquashedRectangleAtIndex(index - 1).getWidth();
            }
         }
      }
      else if (direction > 0) {
         if ((viewRectangle.getX() + viewRectangle.getWidth()) < getPreferredSize().getWidth()) {
            index = getIndexAtPoint(new Point((int)(viewRectangle.getX() + viewRectangle.getWidth()), 0));
            jiggleJump = (int)((getSquashedRectangleAtIndex(index).getX() + getSquashedRectangleAtIndex(index).getWidth()) - (viewRectangle.getX() + viewRectangle.getWidth()));
            if (jiggleJump == 0) {
               jiggleJump += getSquashedRectangleAtIndex(index + 1).getWidth();
            }
         }
      }

      return jiggleJump;
   }

   public synchronized int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

      Dimension preferredSize;
      Rectangle viewRectangle;

      viewRectangle = ((JViewport)getParent()).getViewRect();

      if (direction < 0) {
         return (int)viewRectangle.getX();
      }
      else if (direction > 0) {
         preferredSize = getPreferredSize();

         return (int)(preferredSize.getWidth() - viewRectangle.getX() + viewRectangle.getWidth());
      }

      return 0;
   }

   public synchronized boolean getScrollableTracksViewportWidth () {

      if (getParent() instanceof JViewport) {
         return (getParent().getWidth() > getPreferredSize().width);
      }

      return false;
   }

   public synchronized boolean getScrollableTracksViewportHeight () {

      if (getParent() instanceof JViewport) {
         return (getParent().getHeight() > getPreferredSize().height);
      }

      return false;
   }

   public synchronized Dimension getPreferredSize () {

      BannerRenderer bannerRenderer;
      Component renderComponent;
      int width = 0;
      int height = 0;

      bannerRenderer = getBannerRenderer();
      for (int count = 0; count < getListModel().getSize(); count++) {
         renderComponent = bannerRenderer.getBannerRendererComponent(this, getListModel().getElementAt(count), count, false);
         width += (int)renderComponent.getPreferredSize().getWidth();
         if (renderComponent.getPreferredSize().getHeight() > height) {
            height = (int)renderComponent.getPreferredSize().getHeight();
         }
      }

      return new Dimension(width, height);
   }

   public synchronized void paint (Graphics graphics) {

      BannerRenderer bannerRenderer;
      Component renderComponent;
      Dimension renderPreferredSize;
      int preferredHeight;
      int prevWidth = 0;

      preferredHeight = (int)getPreferredSize().getHeight();
      bannerRenderer = getBannerRenderer();
      for (int count = 0; count < getListModel().getSize(); count++) {
         renderComponent = bannerRenderer.getBannerRendererComponent(this, getListModel().getElementAt(count), count, listSelectionModel.isSelectedIndex(count));
         renderPreferredSize = renderComponent.getPreferredSize();
         renderComponent.setBounds(0, 0, (int)renderPreferredSize.getWidth(), preferredHeight);
         graphics.translate(prevWidth, 0);
         renderComponent.paint(graphics);
         prevWidth = (int)renderPreferredSize.getWidth();
      }
   }

   public class SelectLeftAction extends AbstractAction {

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         scrollLeft();
      }

   }

   public class SelectRightAction extends AbstractAction {

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         scrollRight();
      }

   }

}
