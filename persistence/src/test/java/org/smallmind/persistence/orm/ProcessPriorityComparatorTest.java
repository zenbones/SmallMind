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
package org.smallmind.persistence.orm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Confirms the ordering contract relied on when transaction post-processes are executed: callbacks sort by
 * declared {@link ProcessPriority}, which follows the enum's own declaration order.
 */
@Test(groups = "unit")
public class ProcessPriorityComparatorTest {

  public void testCompareReflectsDeclaredPriorityOrder () {

    ProcessPriorityComparator comparator = new ProcessPriorityComparator();

    Assert.assertTrue(comparator.compare(process(ProcessPriority.ABSOLUTELY_FIRST), process(ProcessPriority.MIDDLE)) < 0);
    Assert.assertTrue(comparator.compare(process(ProcessPriority.LAST), process(ProcessPriority.FIRST)) > 0);
    Assert.assertEquals(comparator.compare(process(ProcessPriority.MIDDLE), process(ProcessPriority.MIDDLE)), 0);
  }

  public void testSortPlacesHighestPriorityFirst () {

    List<TransactionPostProcess> processes = new ArrayList<>();

    processes.add(process(ProcessPriority.LAST));
    processes.add(process(ProcessPriority.ABSOLUTELY_FIRST));
    processes.add(process(ProcessPriority.MIDDLE));
    processes.add(process(ProcessPriority.FIRST));

    Collections.sort(processes, new ProcessPriorityComparator());

    Assert.assertEquals(processes.get(0).getPriority(), ProcessPriority.ABSOLUTELY_FIRST);
    Assert.assertEquals(processes.get(1).getPriority(), ProcessPriority.FIRST);
    Assert.assertEquals(processes.get(2).getPriority(), ProcessPriority.MIDDLE);
    Assert.assertEquals(processes.get(3).getPriority(), ProcessPriority.LAST);
  }

  private static TransactionPostProcess process (ProcessPriority priority) {

    return new FixedPriorityPostProcess(priority);
  }

  private static class FixedPriorityPostProcess extends TransactionPostProcess {

    private FixedPriorityPostProcess (ProcessPriority priority) {

      super(TransactionEndState.COMMIT, priority);
    }

    @Override
    public void process () {

    }
  }
}
