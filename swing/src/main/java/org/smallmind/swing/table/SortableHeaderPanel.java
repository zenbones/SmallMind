package org.smallmind.swing.table;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.SystemColor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.swing.LayoutManagerConstructionException;
import org.smallmind.swing.LayoutManagerFactory;
import org.smallmind.swing.label.PlainLabel;

public class SortableHeaderPanel<E extends Enum> extends JPanel {

   private static ImageIcon NONE;
   private static ImageIcon DESCENDING;
   private static ImageIcon ASCENDING;

   private SortableTable<E> sortableTable;
   private E enumDataType;
   private PlainLabel sortingLabel;
   private boolean returnToNeutral;
   private boolean showOrder;

   private int clickWidth = 0;

   static {

      NONE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/16x16/plain/unknown.png"));
      ASCENDING = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/business and data/16x16/plain/sort_ascending.png"));
      DESCENDING = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/business and data/16x16/plain/sort_descending.png"));
   }

   public SortableHeaderPanel (SortableTable<E> sortableTable, E enumDataType, JComponent headerComponent, boolean returnToNeutral, boolean showOrder)
      throws LayoutManagerConstructionException {

      super(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));

      GridBagConstraints constraint;

      this.sortableTable = sortableTable;
      this.enumDataType = enumDataType;
      this.returnToNeutral = returnToNeutral;
      this.showOrder = showOrder;

      setBackground(SystemColor.control);

      headerComponent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SystemColor.controlDkShadow), BorderFactory.createMatteBorder(0, 1, 0, 0, SystemColor.controlLtHighlight)), BorderFactory.createEmptyBorder(2, 2, 2, 2)));

      sortingLabel = new PlainLabel();
      sortingLabel.setFontSize(12.0F);
      sortingLabel.setHorizontalAlignment(JLabel.RIGHT);
      sortingLabel.setVerticalAlignment(JLabel.BOTTOM);
      sortingLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, SystemColor.controlDkShadow), BorderFactory.createEmptyBorder(2, 2, 2, 5)));

      constraint = new GridBagConstraints();

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.BOTH;
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 1;
      add(headerComponent, constraint);

      constraint.anchor = GridBagConstraints.EAST;
      constraint.fill = GridBagConstraints.VERTICAL;
      constraint.gridx = 1;
      constraint.gridy = 0;
      constraint.weightx = 0;
      constraint.weighty = 1;
      add(sortingLabel, constraint);

      setToolTipText(headerComponent.getToolTipText());

      displaySortingState();
   }

   public synchronized int getClickWidth () {

      return clickWidth;
   }

   public synchronized void displaySortingState () {

      switch (sortableTable.getSortDirection(enumDataType)) {
         case NONE:
            sortingLabel.setIcon(NONE);
            sortingLabel.setText(null);
            break;
         case DESCENDING:
            sortingLabel.setIcon(DESCENDING);
            if (showOrder) {
               sortingLabel.setText("[" + (sortableTable.getSortOrder(enumDataType) + 1) + "]");
            }
            break;
         case ASCENDING:
            sortingLabel.setIcon(ASCENDING);
            if (showOrder) {
               sortingLabel.setText("[" + (sortableTable.getSortOrder(enumDataType) + 1) + "]");
            }
            break;
         default:
            throw new UnknownSwitchCaseException(sortableTable.getSortDirection(enumDataType).name());
      }

      clickWidth = (int)sortingLabel.getPreferredSize().getWidth();
   }

   public synchronized void incSortingState () {

      switch (sortableTable.getSortDirection(enumDataType)) {
         case NONE:
            sortableTable.setSortDirection(enumDataType, SortableDirection.DESCENDING);
            break;
         case DESCENDING:
            sortableTable.setSortDirection(enumDataType, SortableDirection.ASCENDING);
            break;
         case ASCENDING:
            sortableTable.setSortDirection(enumDataType, (returnToNeutral) ? SortableDirection.NONE : SortableDirection.DESCENDING);
            break;
         default:
            throw new UnknownSwitchCaseException(sortableTable.getSortDirection(enumDataType).name());
      }

      sortableTable.sortTableData();
   }

}
