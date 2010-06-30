package org.smallmind.persistence.orm;

import java.util.Comparator;

public class ProcessPriorityComparator implements Comparator<TransactionPostProcess> {

   public int compare (TransactionPostProcess postProcess1, TransactionPostProcess postProcess2) {

      return postProcess1.getPriority().compareTo(postProcess2.getPriority());
   }
}
