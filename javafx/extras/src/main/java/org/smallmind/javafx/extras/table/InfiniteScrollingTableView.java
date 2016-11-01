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

import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.smallmind.javafx.extras.ConsolidatingChangeListener;

public class InfiniteScrollingTableView<S> extends TableView<S> {

  public InfiniteScrollingTableView () {

    ScrollEventHandler scrollEventHandler = new ScrollEventHandler();

    addEventFilter(MouseEvent.MOUSE_DRAGGED, scrollEventHandler);
    addEventFilter(ScrollEvent.ANY, scrollEventHandler);
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

  private class ScrollEventHandler implements EventHandler<Event> {

    private AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void handle (Event event) {

      if (!initialized.get()) {
        if (ScrollEvent.ANY.equals(event.getEventType()) || (MouseEvent.MOUSE_DRAGGED.equals(event.getEventType()) && event.getTarget().getClass().getName().startsWith("com.sun.javafx.scene.control.skin.ScrollBarSkin"))) {
          if (initialized.compareAndSet(false, true)) {

            final ScrollBar scrollBar;

            if ((scrollBar = getScrollbar(InfiniteScrollingTableView.this)) != null) {
              scrollBar.valueProperty().addListener(new ConsolidatingChangeListener<>(500, new ChangeListener<Number>() {

                @Override
                public void changed (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                  if (newValue.doubleValue() == scrollBar.getMax()) {
                    Platform.runLater(new Runnable() {

                      @Override
                      public void run () {

                        fireEvent(new InfiniteScrollEvent<>(InfiniteScrollEvent.ITEMS_REQUIRED, InfiniteScrollingTableView.this));
                      }
                    });
                  }
                }
              }));
            }
          }
        }
      }
    }
  }
}
