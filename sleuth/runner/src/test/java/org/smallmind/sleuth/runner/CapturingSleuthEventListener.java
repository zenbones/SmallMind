/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.sleuth.runner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEventListener;
import org.smallmind.sleuth.runner.event.SleuthEventType;

/**
 * Test-only {@link SleuthEventListener} that records every event fired during a run so assertions
 * can be made about outcomes, ordering, and skipped work. The backing list is synchronised because
 * Sleuth fires events from the suite and test runner threads concurrently.
 */
public class CapturingSleuthEventListener implements SleuthEventListener {

  private final List<SleuthEvent> events = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void handle (SleuthEvent event) {

    events.add(event);
  }

  public List<SleuthEvent> getEvents () {

    return events;
  }

  public synchronized int countOfType (SleuthEventType type) {

    int count = 0;

    for (SleuthEvent event : events) {
      if (event.getType() == type) {
        count++;
      }
    }

    return count;
  }

  public synchronized boolean hasEvent (SleuthEventType type, String methodName) {

    for (SleuthEvent event : events) {
      if ((event.getType() == type) && methodName.equals(event.getMethodName())) {

        return true;
      }
    }

    return false;
  }

  public synchronized boolean hasMethod (String methodName) {

    for (SleuthEvent event : events) {
      if (methodName.equals(event.getMethodName())) {

        return true;
      }
    }

    return false;
  }

  public synchronized int indexOf (SleuthEventType type, String methodName) {

    for (int index = 0; index < events.size(); index++) {
      if ((events.get(index).getType() == type) && methodName.equals(events.get(index).getMethodName())) {

        return index;
      }
    }

    return -1;
  }
}
