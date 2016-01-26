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
package org.smallmind.nutsnbolts.context;

import java.util.LinkedList;
import java.util.List;

public class ContextStack<C extends Context> {

  private final LinkedList<C> contextList;

  public ContextStack () {

    contextList = new LinkedList<C>();
  }

  public ContextStack (ContextStack<C> contextStack) {

    contextList = new LinkedList<>(contextStack.getInternalList());
  }

  private List<C> getInternalList () {

    return contextList;
  }

  public synchronized boolean isEmpty () {

    return contextList.isEmpty();
  }

  public synchronized C peek () {

    if (contextList.isEmpty()) {
      return null;
    }

    return contextList.getFirst();
  }

  public synchronized void push (C context) {

    if (context instanceof LifecycleAware) {
      ((LifecycleAware)context).beforePush();
    }

    contextList.addFirst(context);
  }

  public synchronized C pop () {

    if (!contextList.isEmpty()) {

      C context = contextList.removeFirst();

      if (context instanceof LifecycleAware) {
        ((LifecycleAware)context).afterPop();
      }

      return context;
    }

    return null;
  }
}