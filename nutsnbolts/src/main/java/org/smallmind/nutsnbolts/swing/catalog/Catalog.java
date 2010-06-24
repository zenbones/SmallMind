package org.smallmind.nutsnbolts.swing.catalog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import org.smallmind.nutsnbolts.swing.event.CatalogDataEvent;
import org.smallmind.nutsnbolts.swing.event.CatalogDataListener;
import org.smallmind.nutsnbolts.swing.event.MultiListSelectionEvent;
import org.smallmind.nutsnbolts.swing.event.MultiListSelectionListener;
import org.smallmind.nutsnbolts.swing.layout.ListLayout;

public class Catalog<T extends Comparable<T>, D> extends JLayeredPane implements Scrollable, CatalogDataListener, HierarchyBoundsListener, ComponentListener, MouseListener, MouseMotionListener {

   private JPanel glassPanel;
   private JPanel catalogPanel;
   private CatalogModel<D> model;
   private MultiListSelectionModel<T> selectionModel;
   private CatalogMultiListDataProvider<T> multiListDataProvider;
   private CatalogCellRenderer renderer;
   private CatalogScrollModel scrollModel;
   private Component lastTargetedComponent = null;

   public Catalog (T key, CatalogModel<D> model) {

      super();

      this.model = model;

      setFocusable(true);

      glassPanel = new JPanel();
      glassPanel.setOpaque(false);
      glassPanel.setSize(getSize());

      catalogPanel = new JPanel(new ListLayout(0));
      catalogPanel.setOpaque(true);
      catalogPanel.setSize(getSize());

      add(catalogPanel, 10);
      add(glassPanel, 11);

      multiListDataProvider = new CatalogMultiListDataProvider<T>(key, this);
      setSelectionModel(new DefaultMultiListSelectionModel<T>());

      model.addCatalogDataListener(this);

      setCellRenderer(new DefaultCatalogCellRenderer());
      setScrollModel(new DefaultCatalogScrollModel(this));

      addHierarchyBoundsListener(this);
      addComponentListener(this);

      glassPanel.addMouseListener(this);
      glassPanel.addMouseMotionListener(this);
   }

   public CatalogModel<D> getModel () {

      return model;
   }

   public synchronized void setCellRenderer (CatalogCellRenderer renderer) {

      this.renderer = renderer;

      catalogPanel.removeAll();
      for (int count = 0; count < model.getSize(); count++) {
         catalogPanel.add(getRenderedComponent(count), count);
      }

      catalogPanel.invalidate();
   }

   public synchronized CatalogCellRenderer getCellRenderer () {

      return renderer;
   }

   public synchronized void setScrollModel (CatalogScrollModel scrollModel) {

      this.scrollModel = scrollModel;
   }

   public synchronized CatalogScrollModel getScrollModel () {

      return scrollModel;
   }

   public synchronized void setSelectionModel (MultiListSelectionModel<T> selectionModel) {

      if (this.selectionModel != null) {
         this.selectionModel.removeMultiListDataProvider(multiListDataProvider);
         this.selectionModel.removeMultiListSelectionListener(multiListDataProvider);
      }

      if (selectionModel != null) {
         this.selectionModel = selectionModel;
         selectionModel.addMultiListDataProvider(multiListDataProvider);
         selectionModel.addMultiListSelectionListener(multiListDataProvider);

         reRenderAllComponents();
      }
   }

   public synchronized MultiListSelectionModel<T> getSelectionModel () {

      return selectionModel;
   }

   public synchronized MultiListSelectionModel.SelctionMode getSelectionMode () {

      return selectionModel.getSelectionMode();
   }

   public synchronized void setSelectionMode (MultiListSelectionModel.SelctionMode selectionMode) {

      selectionModel.setSelectionMode(selectionMode);
   }

   public synchronized void addMultiListSelectionListener (MultiListSelectionListener<T> listener) {

      selectionModel.addMultiListSelectionListener(listener);
   }

   public synchronized void removeMultiListSelectionListener (MultiListSelectionListener<T> listener) {

      selectionModel.removeMultiListSelectionListener(listener);
   }

   public synchronized void clearSelection () {

      selectionModel.clearSelection();
   }

   public synchronized void addSelectionInterval (int startIndex, int endIndex) {

      selectionModel.addSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), startIndex), new MultiListSelection<T>(multiListDataProvider.getKey(), startIndex));
   }

   public synchronized MultiListSelection<T> getAnchorSelectionIndex () {

      return selectionModel.getAnchorSelection();
   }

   public synchronized MultiListSelection<T> getLeadSelectionIndex () {

      return selectionModel.getLeadSelection();
   }

   public synchronized MultiListSelection<T> getMinSelectionIndex () {

      return selectionModel.getMinSelection();
   }

   public synchronized MultiListSelection<T> getMaxSelectionIndex () {

      return selectionModel.getMaxSelection();
   }

   public synchronized int getSelectedIndex () {

      return selectionModel.getSelectedIndex(multiListDataProvider.getKey());
   }

   public synchronized int[] getSelectedIndices () {

      ArrayList<Integer> indexList;
      int[] indices;

      indexList = new ArrayList<Integer>();
      for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
         if (selectionModel.isSelected(new MultiListSelection<T>(multiListDataProvider.getKey(), count))) {
            indexList.add(count);
         }
      }

      indices = new int[indexList.size()];
      for (int count = 0; count < indexList.size(); count++) {
         indices[count] = indexList.get(count);
      }

      return indices;
   }

   public synchronized D getSelectedValue () {

      int index;

      if ((index = getSelectedIndex()) >= 0) {
         return model.getElementAt(index);
      }

      return null;
   }

   public synchronized Object[] getSelectedValues () {

      LinkedList<D> valueList;

      valueList = new LinkedList<D>();
      for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
         if (selectionModel.isSelected(new MultiListSelection<T>(multiListDataProvider.getKey(), count))) {
            valueList.add(model.getElementAt(count));
         }
      }

      return valueList.toArray();
   }

   public synchronized boolean isSelectedIndex (int index) {

      return selectionModel.isSelected(new MultiListSelection<T>(multiListDataProvider.getKey(), index));
   }

   public synchronized boolean isSelectedValue (D element) {

      return selectionModel.isSelected(new MultiListSelection<T>(multiListDataProvider.getKey(), model.indexOf(element)));
   }

   public synchronized boolean isSelectionEmpty () {

      return selectionModel.isSelectionEmpty();
   }

   public synchronized void removeSelectionInterval (int startIndex, int endIndex) {

      selectionModel.removeSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), startIndex), new MultiListSelection<T>(multiListDataProvider.getKey(), endIndex));
   }

   public synchronized void setSelectedIndex (int index) {

      selectionModel.setSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), index), new MultiListSelection<T>(multiListDataProvider.getKey(), index));
   }

   public synchronized void setSelectedIndices (int[] indices) {

      selectionModel.clearSelection();
      for (int index : indices) {
         selectionModel.addSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), index), new MultiListSelection<T>(multiListDataProvider.getKey(), index));
      }
   }

   public synchronized void setSelectionInterval (int startIndex, int endIndex) {

      selectionModel.setSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), startIndex), new MultiListSelection<T>(multiListDataProvider.getKey(), endIndex));
   }

   private void reRenderAllComponents () {

      for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
         reRenderComponent(count);
      }
   }

   private void reRenderComponent (int index) {

      getRenderedComponent(index).repaint();
   }

   public Component getRenderedComponent (int index) {

      return renderer.getCatalogCellRendererComponent(this, model.getElementAt(index), index, isSelectedIndex(index), false);
   }

   public synchronized void itemAdded (CatalogDataEvent catalogDataEvent) {

      Component component;
      int index;

      index = catalogDataEvent.getEndIndex();
      component = getRenderedComponent(index);
      catalogPanel.add(component, index);

      adjustToContents();

      for (int count = catalogDataEvent.getEndIndex() + 1; count < catalogPanel.getComponentCount(); count++) {
         reRenderComponent(count);
      }

      selectionModel.insertIndexInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), index), 1, true);
   }

   public synchronized void intervalRemoved (CatalogDataEvent catalogDataEvent) {

      for (int count = catalogDataEvent.getStartIndex(); count <= catalogDataEvent.getEndIndex(); count++) {
         catalogPanel.remove(catalogDataEvent.getStartIndex());
      }

      adjustToContents();

      for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
         reRenderComponent(count);
      }

      selectionModel.removeIndexInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), catalogDataEvent.getStartIndex()), new MultiListSelection<T>(multiListDataProvider.getKey(), catalogDataEvent.getEndIndex()));
   }

   public synchronized void intervalChanged (CatalogDataEvent catalogDataEvent) {

      for (int count = catalogDataEvent.getStartIndex(); count <= catalogDataEvent.getEndIndex(); count++) {
         catalogPanel.remove(catalogDataEvent.getStartIndex());
      }

      for (int count = catalogDataEvent.getEndIndex(); count >= catalogDataEvent.getStartIndex(); count--) {
         catalogPanel.add(getRenderedComponent(count), catalogDataEvent.getStartIndex());
      }

      adjustToContents();

      for (int count = catalogDataEvent.getStartIndex(); count <= catalogDataEvent.getEndIndex(); count++) {
         reRenderComponent(count);
      }
   }

   private void adjustToContents () {

      Rectangle bounds;
      double preferredWidth;
      double preferredHeight = 0;

      if (getParent() != null) {
         if (getParent() instanceof JViewport) {
            preferredWidth = ((JViewport)getParent()).getViewSize().getWidth();
         }
         else {
            preferredWidth = getParent().getWidth();
         }

         for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
            preferredHeight += catalogPanel.getComponent(count).getPreferredSize().getHeight();
         }

         bounds = new Rectangle(new Point(0, 0), new Dimension((int)preferredWidth, (int)preferredHeight));
         setPreferredSize(bounds.getSize());
         catalogPanel.setBounds(bounds);
         glassPanel.setBounds(bounds);
      }
   }

   public synchronized void revalidate () {

      adjustToContents();
      super.revalidate();
   }

   public synchronized void repaint () {

      reRenderAllComponents();
   }

   public synchronized int getIndexAtPoint (Point point) {

      int height = 0;

      for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
         height += catalogPanel.getComponent(count).getPreferredSize().getHeight();
         if (point.getY() <= height) {
            return count;
         }
      }

      return -1;
   }

   public synchronized Rectangle getSquashedRectangleAtIndex (int index) {

      int yPos = 0;

      for (int count = 0; count < index; count++) {
         yPos += catalogPanel.getComponent(count).getPreferredSize().getHeight();
      }

      return new Rectangle(0, yPos, 0, (int)catalogPanel.getComponent(index).getPreferredSize().getHeight());
   }

   public synchronized Dimension getPreferredScrollableViewportSize () {

      return getPreferredSize();
   }

   public boolean getScrollableTracksViewportWidth () {

      return true;
   }

   public boolean getScrollableTracksViewportHeight () {

      return false;
   }

   public synchronized int getScrollableUnitIncrement (Rectangle visibleRect, int orientation, int direction) {

      return scrollModel.getScrollableUnitIncrement(visibleRect, orientation, direction);
   }

   public synchronized int getScrollableBlockIncrement (Rectangle visibleRect, int orientation, int direction) {

      return scrollModel.getScrollableBlockIncrement(visibleRect, orientation, direction);
   }

   public synchronized void valueChanged (MultiListSelectionEvent multiListSelectionEvent) {

      for (int count = multiListSelectionEvent.getFirstIndex(); count <= multiListSelectionEvent.getLastIndex(); count++) {
         if (count >= catalogPanel.getComponentCount()) {
            break;
         }
         else {
            reRenderComponent(count);
         }
      }
   }

   public synchronized void ancestorResized (HierarchyEvent hierarchyEvent) {

      adjustToContents();
   }

   public void ancestorMoved (HierarchyEvent hierarchyEvent) {
   }

   public synchronized void componentHidden (ComponentEvent componentEvnt) {

      adjustToContents();
   }

   public void componentMoved (ComponentEvent componentEvnt) {
   }

   public synchronized void componentShown (ComponentEvent componentEvnt) {

      adjustToContents();
   }

   public synchronized void componentResized (ComponentEvent componentEvnt) {

      adjustToContents();
   }

   private void dispatchGlassMouseEvent (MouseEvent mouseEvent, boolean moved) {

      Component targetComponent;
      MouseEvent targetEvent;
      Point mousePoint;

      if ((targetComponent = SwingUtilities.getDeepestComponentAt(catalogPanel, mouseEvent.getX(), mouseEvent.getY())) != null) {
         if (moved && (targetComponent != lastTargetedComponent)) {
            if (lastTargetedComponent != null) {
               mousePoint = SwingUtilities.convertPoint(glassPanel, mouseEvent.getPoint(), lastTargetedComponent);
               lastTargetedComponent.dispatchEvent(new MouseEvent(lastTargetedComponent, MouseEvent.MOUSE_EXITED, mouseEvent.getWhen(), 0, (int)mousePoint.getX(), (int)mousePoint.getY(), mouseEvent.getClickCount(), mouseEvent.isPopupTrigger(), mouseEvent.getButton()));
            }

            mousePoint = SwingUtilities.convertPoint(glassPanel, mouseEvent.getPoint(), targetComponent);
            targetComponent.dispatchEvent(new MouseEvent(targetComponent, MouseEvent.MOUSE_ENTERED, mouseEvent.getWhen(), 0, (int)mousePoint.getX(), (int)mousePoint.getY(), mouseEvent.getClickCount(), mouseEvent.isPopupTrigger(), mouseEvent.getButton()));
         }

         targetEvent = SwingUtilities.convertMouseEvent(glassPanel, mouseEvent, targetComponent);
         targetComponent.dispatchEvent(targetEvent);
         lastTargetedComponent = targetComponent;
      }
   }

   private int getComponentIndex (Point point) {

      Component cellComponent;

      if ((cellComponent = catalogPanel.getComponentAt(point)) != null) {
         for (int count = 0; count < catalogPanel.getComponentCount(); count++) {
            if (catalogPanel.getComponent(count) == cellComponent) {
               return count;
            }
         }
      }

      return -1;
   }

   public synchronized void mouseClicked (MouseEvent mouseEvent) {

      dispatchGlassMouseEvent(mouseEvent, false);
   }

   public synchronized void mousePressed (MouseEvent mouseEvent) {

      MultiListSelection<T> anchorSelection;
      int index;

      if ((index = getComponentIndex(mouseEvent.getPoint())) >= 0) {
         if (mouseEvent.isShiftDown()) {
            if ((anchorSelection = selectionModel.getAnchorSelection()) == null) {
               setSelectedIndex(index);
            }
            else {
               selectionModel.setSelectionInterval(anchorSelection, new MultiListSelection<T>(multiListDataProvider.getKey(), index));
            }
         }
         else if (mouseEvent.isControlDown()) {
            if (isSelectedIndex(index)) {
               selectionModel.removeSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), index), new MultiListSelection<T>(multiListDataProvider.getKey(), index));
            }
            else {
               selectionModel.addSelectionInterval(new MultiListSelection<T>(multiListDataProvider.getKey(), index), new MultiListSelection<T>(multiListDataProvider.getKey(), index));
            }
         }
         else {
            setSelectedIndex(index);
         }
         requestFocusInWindow();
      }

      dispatchGlassMouseEvent(mouseEvent, false);
   }

   public synchronized void mouseReleased (MouseEvent mouseEvent) {

      dispatchGlassMouseEvent(mouseEvent, false);
   }

   public synchronized void mouseEntered (MouseEvent mouseEvent) {

      lastTargetedComponent = null;
   }

   public synchronized void mouseExited (MouseEvent mouseEvent) {
   }

   public synchronized void mouseDragged (MouseEvent mouseEvent) {

      dispatchGlassMouseEvent(mouseEvent, true);
   }

   public synchronized void mouseMoved (MouseEvent mouseEvent) {

      dispatchGlassMouseEvent(mouseEvent, true);
   }

}
