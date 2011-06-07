/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.swing.catalog;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.swing.MultiListSelectionEvent;
import org.smallmind.swing.MultiListSelectionListener;
import org.smallmind.nutsnbolts.util.DirectionalComparator;
import org.smallmind.nutsnbolts.util.NaturalDirectionalComparator;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DefaultMultiListSelectionModel<T extends Comparable<T>> implements MultiListSelectionModel<T> {

   private transient WeakEventListenerList<MultiListSelectionListener<T>> listenerList;

   private NaturalDirectionalComparator<T> comparator;
   private TreeMap<T, MultiListDataProvider<T>> dataProviderMap;
   private TreeMap<T, LinkedList<Range>> rangeMap;
   private MultiListSelection<T> anchorSelection;
   private MultiListSelection<T> leadSelection;
   private SelctionMode selectionMode;
   private boolean valueIsAdjusting;

   public DefaultMultiListSelectionModel () {

      this(DirectionalComparator.Direction.DESCENDING);
   }

   public DefaultMultiListSelectionModel (DirectionalComparator.Direction direction) {

      valueIsAdjusting = false;
      selectionMode = SelctionMode.SINGLE_SELECTION;
      anchorSelection = null;
      leadSelection = null;

      listenerList = new WeakEventListenerList<MultiListSelectionListener<T>>();
      setDirection(direction);
   }

   public synchronized void setDirection (DirectionalComparator.Direction direction) {

      TreeMap<T, MultiListDataProvider<T>> prevDataProviderMap;
      TreeMap<T, LinkedList<Range>> prevRangeMap;

      comparator = new NaturalDirectionalComparator<T>(direction);

      prevDataProviderMap = dataProviderMap;
      prevRangeMap = rangeMap;

      dataProviderMap = new TreeMap<T, MultiListDataProvider<T>>(comparator);
      rangeMap = new TreeMap<T, LinkedList<Range>>(comparator);

      if (prevDataProviderMap != null) {
         dataProviderMap.putAll(prevDataProviderMap);
      }

      if (prevRangeMap != null) {
         rangeMap.putAll(prevRangeMap);
      }
   }

   public synchronized void clearMultiListDataProviders () {

      dataProviderMap.clear();
   }

   public synchronized void addMultiListDataProvider (MultiListDataProvider<T> dataProvider) {

      dataProviderMap.put(dataProvider.getKey(), dataProvider);
   }

   public synchronized void removeMultiListDataProvider (MultiListDataProvider<T> dataProvider) {

      dataProviderMap.remove(dataProvider.getKey());
   }

   public synchronized void clearMultiListSelectionListeners () {

      listenerList.removeAllListeners();
   }

   public synchronized void addMultiListSelectionListener (MultiListSelectionListener<T> selectionListener) {

      listenerList.addListener(selectionListener);
   }

   public synchronized void removeMultiListSelectionListener (MultiListSelectionListener<T> selectionListener) {

      listenerList.removeListener(selectionListener);
   }

   public synchronized SelctionMode getSelectionMode () {

      return selectionMode;
   }

   public synchronized void setSelectionMode (SelctionMode selectionMode) {

      this.selectionMode = selectionMode;
   }

   public synchronized void setValueIsAdjusting (boolean valueIsAdjusting) {

      this.valueIsAdjusting = valueIsAdjusting;
   }

   public synchronized boolean getValueIsAdjusting () {

      return valueIsAdjusting;
   }

   public synchronized boolean isSelectionEmpty () {

      return rangeMap.isEmpty();
   }

   public synchronized void clearSelection () {

      rangeMap.clear();
      anchorSelection = null;
      leadSelection = null;
   }

   public synchronized MultiListSelection<T> getMinSelection () {

      T firstKey;

      if (isSelectionEmpty()) {
         return null;
      }

      firstKey = rangeMap.firstKey();

      return new MultiListSelection<T>(firstKey, rangeMap.get(firstKey).getFirst().getFirstIndex());
   }

   public synchronized MultiListSelection<T> getMaxSelection () {

      T lastKey;

      if (isSelectionEmpty()) {
         return null;
      }

      lastKey = rangeMap.lastKey();

      return new MultiListSelection<T>(lastKey, rangeMap.get(lastKey).getLast().getLastIndex());
   }

   public synchronized int getSelectedIndex (T key) {

      LinkedList<Range> rangeList;

      if ((rangeList = rangeMap.get(key)) != null) {
         if (!rangeList.isEmpty()) {
            return rangeList.getFirst().getFirstIndex();
         }
      }

      return -1;
   }

   public synchronized boolean isSelected (MultiListSelection<T> selection) {

      LinkedList<Range> rangeList;
      Iterator<Range> rangeIter;

      if ((rangeList = rangeMap.get(selection.getComparable())) != null) {
         rangeIter = rangeList.iterator();
         while (rangeIter.hasNext()) {
            if (rangeIter.next().isIncluded(selection.getIndex())) {
               return true;
            }
         }
      }

      return false;
   }

   public synchronized MultiListSelection<T> findStableSelection (MultiListSelection<T> selection) {

      MultiListSelection<T> stableSelection;

      if ((stableSelection = getPreviousSelection(selection)) == null) {
         stableSelection = getNextSelection(selection);
      }

      return stableSelection;
   }

   public synchronized MultiListSelection<T> getPreviousSelection (MultiListSelection<T> selection) {

      Iterator<T> keyIter;
      T key;
      T lesserKey = null;

      if (dataProviderMap.containsKey(selection.getComparable()) && (dataProviderMap.get(selection.getComparable()).getElementCount() > 0) && (selection.getIndex() > 0)) {
         return new MultiListSelection<T>(selection.getComparable(), Math.min(selection.getIndex() - 1, dataProviderMap.get(selection.getComparable()).getElementCount() - 1));
      }

      keyIter = dataProviderMap.keySet().iterator();
      while (keyIter.hasNext()) {
         key = keyIter.next();
         if (comparator.compare(key, selection.getComparable()) >= 0) {
            break;
         }

         lesserKey = key;
      }

      if (lesserKey != null) {
         return new MultiListSelection<T>(lesserKey, dataProviderMap.get(lesserKey).getElementCount() - 1);
      }

      return null;
   }

   public synchronized MultiListSelection<T> getNextSelection (MultiListSelection<T> selection) {

      Iterator<T> keyIter;
      T key;

      if (dataProviderMap.containsKey(selection.getComparable())) {
         if (selection.getIndex() < (dataProviderMap.get(selection.getComparable()).getElementCount() - 1)) {
            return new MultiListSelection<T>(selection.getComparable(), selection.getIndex() + 1);
         }
      }

      keyIter = dataProviderMap.keySet().iterator();
      while (keyIter.hasNext()) {
         key = keyIter.next();
         if (comparator.compare(key, selection.getComparable()) > 0) {
            return new MultiListSelection<T>(key, 0);
         }
      }

      return null;
   }

   public synchronized void setSilentAnchorSelection (MultiListSelection<T> anchorSelection) {

      this.anchorSelection = anchorSelection;
   }

   public synchronized void setAnchorSelection (MultiListSelection<T> anchorSelection) {

      MultiListSelection<T> oldAnchorSelection = this.anchorSelection;

      if (oldAnchorSelection == null) {
         oldAnchorSelection = anchorSelection;
      }

      this.anchorSelection = anchorSelection;
      fireValueChanged(MultiListSelectionEvent.EventType.CHANGE, new MultiListSelectionRange<T>(oldAnchorSelection, anchorSelection, comparator.getDirection()));
   }

   public synchronized MultiListSelection<T> getAnchorSelection () {

      return anchorSelection;
   }

   public synchronized void setSilentLeadSelection (MultiListSelection<T> leadSelection) {

      this.leadSelection = leadSelection;
   }

   public synchronized void setLeadSelection (MultiListSelection<T> leadSelection) {

      MultiListSelection<T> oldLeadSelection = this.leadSelection;
      MultiListSelectionStack<T> selectionStack = new MultiListSelectionStack<T>(comparator.getDirection());

      if (anchorSelection == null) {
         anchorSelection = leadSelection;
         oldLeadSelection = leadSelection;
      }

      if (isSelected(anchorSelection)) {
         removeSelectionInterval(anchorSelection, oldLeadSelection);
         addSelectionInterval(anchorSelection, leadSelection);
      }
      else {
         addSelectionInterval(anchorSelection, oldLeadSelection);
         removeSelectionInterval(anchorSelection, leadSelection);
      }

      selectionStack.addMultiListSelection(anchorSelection);
      selectionStack.addMultiListSelection(oldLeadSelection);
      selectionStack.addMultiListSelection(leadSelection);

      this.leadSelection = leadSelection;
      fireValueChanged(MultiListSelectionEvent.EventType.CHANGE, new MultiListSelectionRange<T>(selectionStack.getFirst(), selectionStack.getLast(), comparator.getDirection()));
   }

   public synchronized MultiListSelection<T> getLeadSelection () {

      return leadSelection;
   }

   public synchronized void addSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1) {

      addSelectionInterval(selection0, selection1, false);
   }

   private void addSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1, boolean set) {

      MultiListSelection<T> oldAnchorSelection = anchorSelection;
      MultiListSelection<T> oldLeadSelection = leadSelection;
      MultiListSelectionRange<T> selectionRange;
      MultiListSelectionStack<T> selectionStack;

      switch (selectionMode) {
         case SINGLE_SELECTION:
            if (oldLeadSelection == null) {
               oldLeadSelection = selection1;
            }

            clearSelection();
            selectionRange = new MultiListSelectionRange<T>(selection1, selection1, comparator.getDirection());
            addMultiListSelectionRange(selectionRange);

            anchorSelection = selection0;
            leadSelection = selection1;
            fireValueChanged(MultiListSelectionEvent.EventType.INSERT, new MultiListSelectionRange<T>(oldLeadSelection, selection1, comparator.getDirection()));
            break;
         case SINGLE_INTERVAL_SELECTION:
            clearSelection();
            selectionRange = new MultiListSelectionRange<T>(selection0, selection1, comparator.getDirection());
            addMultiListSelectionRange(selectionRange);

            selectionStack = new MultiListSelectionStack<T>(comparator.getDirection());
            selectionStack.addMultiListSelection(oldAnchorSelection);
            selectionStack.addMultiListSelection(oldLeadSelection);
            selectionStack.addMultiListSelection(selection0);
            selectionStack.addMultiListSelection(selection1);

            anchorSelection = selection0;
            leadSelection = selection1;
            fireValueChanged(MultiListSelectionEvent.EventType.INSERT, new MultiListSelectionRange<T>(selectionStack.getFirst(), selectionStack.getLast(), comparator.getDirection()));
            break;
         case MULTIPLE_INTERVAL_SELECTION:
            selectionStack = new MultiListSelectionStack<T>(comparator.getDirection());

            if (set) {
               selectionStack.addMultiListSelection(getMinSelection());
               selectionStack.addMultiListSelection(getMaxSelection());
               clearSelection();
            }

            selectionRange = new MultiListSelectionRange<T>(selection0, selection1, comparator.getDirection());
            addMultiListSelectionRange(selectionRange);

            selectionStack.addMultiListSelection(selection0);
            selectionStack.addMultiListSelection(selection1);

            anchorSelection = selection0;
            leadSelection = selection1;
            fireValueChanged(MultiListSelectionEvent.EventType.INSERT, new MultiListSelectionRange<T>(selectionStack.getFirst(), selectionStack.getLast(), comparator.getDirection()));
            break;
         default:
            throw new UnknownSwitchCaseException(selectionMode.name());
      }
   }

   private void addMultiListSelectionRange (MultiListSelectionRange<T> selectionRange) {

      Iterator<T> keyIter;
      T key;

      keyIter = dataProviderMap.keySet().iterator();
      while (keyIter.hasNext()) {
         key = keyIter.next();
         switch (selectionRange.getContainment(key)) {
            case SURROUNDS:
               addRange(key, new Range(selectionRange.getFirstSelection().getIndex(), selectionRange.getLastSelection().getIndex()));
               break;
            case HEAD:
               addRange(key, new Range(selectionRange.getFirstSelection().getIndex(), dataProviderMap.get(key).getElementCount() - 1));
               break;
            case TAIL:
               addRange(key, new Range(0, selectionRange.getLastSelection().getIndex()));
               break;
            case WITHIN:
               addRange(key, new Range(0, dataProviderMap.get(key).getElementCount() - 1));
            case OUT:
               break;
            default:
               throw new UnknownSwitchCaseException(selectionRange.getContainment(key).name());
         }
      }
   }

   private void addRange (T key, Range addRange) {

      LinkedList<Range> curRangeList;
      LinkedList<Range> addRangeList;
      Iterator<Range> curRangeIter;
      Range curRange;
      boolean integrated = false;

      addRangeList = new LinkedList<Range>();

      if ((curRangeList = rangeMap.get(key)) != null) {
         curRangeIter = curRangeList.iterator();
         while (curRangeIter.hasNext()) {
            curRange = curRangeIter.next();

            if (integrated) {
               addRangeList.add(curRange);
            }
            else if (addRange.getFirstIndex() < curRange.getFirstIndex()) {
               if (addRange.getLastIndex() < (curRange.getFirstIndex() - 1)) {
                  addRangeList.add(addRange);
                  addRangeList.add(curRange);
                  integrated = true;
               }
               else {
                  curRange.setFirstIndex(addRange.getFirstIndex());
                  curRange.setLastIndex(Math.max(addRange.getLastIndex(), curRange.getLastIndex()));
                  addRange = curRange;
               }
            }
            else if (addRange.getFirstIndex() > (curRange.getLastIndex() + 1)) {
               addRangeList.add(curRange);
            }
            else {
               curRange.setLastIndex(Math.max(addRange.getLastIndex(), curRange.getLastIndex()));
               addRange = curRange;
            }
         }
      }

      if (!integrated) {
         addRangeList.add(addRange);
      }

      if (addRangeList.isEmpty()) {
         rangeMap.remove(key);
      }
      else {
         rangeMap.put(key, addRangeList);
      }
   }

   public synchronized void setSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1) {

      addSelectionInterval(selection0, selection1, true);
   }

   public synchronized void removeSelectionInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1) {

      MultiListSelectionRange<T> selectionRange;
      Iterator<T> keyIter;
      T key;

      selectionRange = new MultiListSelectionRange<T>(selection0, selection1, comparator.getDirection());

      keyIter = dataProviderMap.keySet().iterator();
      while (keyIter.hasNext()) {
         key = keyIter.next();
         switch (selectionRange.getContainment(key)) {
            case SURROUNDS:
               removeRange(key, new Range(selectionRange.getFirstSelection().getIndex(), selectionRange.getLastSelection().getIndex()));
               break;
            case HEAD:
               removeRange(key, new Range(selectionRange.getFirstSelection().getIndex(), dataProviderMap.get(key).getElementCount() - 1));
               break;
            case TAIL:
               removeRange(key, new Range(0, selectionRange.getLastSelection().getIndex()));
               break;
            case WITHIN:
               removeRange(key, new Range(0, dataProviderMap.get(key).getElementCount() - 1));
            case OUT:
               break;
            default:
               throw new UnknownSwitchCaseException(selectionRange.getContainment(key).name());
         }
      }

      anchorSelection = selection0;
      leadSelection = selection1;
      fireValueChanged(MultiListSelectionEvent.EventType.REMOVE, selectionRange);
   }

   private void removeRange (T key, Range delRange) {

      LinkedList<Range> curRangeList;
      LinkedList<Range> delRangeList;
      Iterator<Range> curRangeIter;
      Range curRange;
      int lastIndex;

      delRangeList = new LinkedList<Range>();

      if ((curRangeList = rangeMap.get(key)) != null) {
         curRangeIter = curRangeList.iterator();
         while (curRangeIter.hasNext()) {
            curRange = curRangeIter.next();

            if (delRange.getFirstIndex() <= curRange.getFirstIndex()) {
               if (delRange.getLastIndex() >= curRange.getFirstIndex()) {
                  if (delRange.getLastIndex() < curRange.getLastIndex()) {
                     curRange.setFirstIndex(delRange.getLastIndex() + 1);
                     delRangeList.add(curRange);
                  }
               }
               else {
                  delRangeList.add(curRange);
               }
            }
            else if (delRange.getFirstIndex() <= curRange.getLastIndex()) {
               lastIndex = curRange.getLastIndex();
               curRange.setLastIndex(delRange.getFirstIndex() - 1);
               delRangeList.add(curRange);

               if (delRange.getLastIndex() < lastIndex) {
                  delRangeList.add(new Range(delRange.getLastIndex() + 1, lastIndex));
               }
            }
            else {
               delRangeList.add(curRange);
            }
         }
      }

      if (delRangeList.isEmpty()) {
         rangeMap.remove(key);
      }
      else {
         rangeMap.put(key, delRangeList);
      }
   }

   public synchronized void insertIndexInterval (MultiListSelection<T> selection, int length, boolean before) {

      LinkedList<Range> curRangeList;
      Iterator<Range> curRangeIter;
      Range curRange;
      int targetIndex;

      targetIndex = (before) ? selection.getIndex() : selection.getIndex() + 1;

      if ((curRangeList = rangeMap.get(selection.getComparable())) != null) {
         curRangeIter = curRangeList.iterator();
         while (curRangeIter.hasNext()) {
            curRange = curRangeIter.next();
            if (curRange.getFirstIndex() >= targetIndex) {
               curRange.setFirstIndex(curRange.getFirstIndex() + length);
            }
            if (curRange.getLastIndex() >= targetIndex) {
               curRange.setLastIndex(curRange.getLastIndex() + length);
            }
         }
      }
   }

   public synchronized void removeIndexInterval (MultiListSelection<T> selection0, MultiListSelection<T> selection1) {

      removeSelectionInterval(selection0, selection1);
   }

   private void fireValueChanged (MultiListSelectionEvent.EventType eventType, MultiListSelectionRange<T> selectionRange) {

      Iterator<MultiListSelectionListener<T>> listenerIter = listenerList.getListeners();
      MultiListSelectionListener<T> listener;

      while (listenerIter.hasNext()) {
         listener = listenerIter.next();
         switch (selectionRange.getContainment(listener.getKey())) {
            case SURROUNDS:
               listener.valueChanged(new MultiListSelectionEvent(this, eventType, selectionRange.getFirstSelection().getIndex(), selectionRange.getLastSelection().getIndex(), valueIsAdjusting));
               break;
            case HEAD:
               listener.valueChanged(new MultiListSelectionEvent(this, eventType, selectionRange.getFirstSelection().getIndex(), dataProviderMap.get(listener.getKey()).getElementCount() - 1, valueIsAdjusting));
               break;
            case TAIL:
               listener.valueChanged(new MultiListSelectionEvent(this, eventType, 0, selectionRange.getLastSelection().getIndex(), valueIsAdjusting));
               break;
            case WITHIN:
               listener.valueChanged(new MultiListSelectionEvent(this, eventType, 0, dataProviderMap.get(listener.getKey()).getElementCount() - 1, valueIsAdjusting));
               break;
            case OUT:
               break;
            default:
               throw new UnknownSwitchCaseException(selectionRange.getContainment(listener.getKey()).name());
         }
      }
   }

   private class Range {

      private int firstIndex;
      private int lastIndex;

      public Range (int firstIndex, int lastIndex) {

         this.firstIndex = firstIndex;
         this.lastIndex = lastIndex;
      }

      public int getFirstIndex () {

         return firstIndex;
      }

      public void setFirstIndex (int firstIndex) {

         this.firstIndex = firstIndex;
      }

      public int getLastIndex () {

         return lastIndex;
      }

      public void setLastIndex (int lastIndex) {

         this.lastIndex = lastIndex;
      }

      public boolean isIncluded (int index) {

         return ((index >= firstIndex) && (index <= lastIndex));
      }

   }

}
