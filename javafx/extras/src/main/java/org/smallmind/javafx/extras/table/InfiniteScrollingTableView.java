/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.javafx.extras.table;

import java.util.concurrent.atomic.AtomicReference;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;
import javafx.scene.input.ScrollEvent;

public class InfiniteScrollingTableView<S> extends TableView<S> {

  public InfiniteScrollingTableView () {

    // TODO: Should be WeakEventHandler
    setOnScroll(new EventHandler<ScrollEvent>() {

      private AtomicReference<ScrollBar> scrollBarRef = new AtomicReference<>();

      @Override
      public void handle (ScrollEvent scrollEvent) {

        if (scrollEvent.getSource() != null) {
          if (scrollBarRef.get() == null) {
            scrollBarRef.set(getScrollbar(InfiniteScrollingTableView.this));
          }
          if (scrollEvent.getSource().equals(scrollBarRef.get())) {

            if (scrollBarRef.get().getValue() == scrollBarRef.get().getMax()) {

              int currentSize = getItems().size();

              fireEvent(new InfiniteScrollEvent<>(InfiniteScrollEvent.ITEMS_REQUIRED, InfiniteScrollingTableView.this));
              scrollBarRef.get().setValue(currentSize / getItems().size() * scrollBarRef.get().getMax());
            }
          }
        }
      }
    });
  }

  public InfiniteScrollingTableView (ObservableList<S> items) {

    super(items);
  }

  private static ScrollBar getScrollbar (TableView<?> tableView) {

    return getScrollbar(tableView, Orientation.VERTICAL);
  }

  private static ScrollBar getScrollbar (TableView<?> tableView, Orientation orientation) {

    for (Node node : tableView.lookupAll(".scroll-bar")) {
      if (node instanceof ScrollBar) {

        if (((ScrollBar)node).getOrientation().equals(orientation)) {

          return ((ScrollBar)node);
        }
      }
    }

    return null;
  }
}
