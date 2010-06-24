package org.smallmind.nutsnbolts.context;

import java.util.LinkedList;

public class ContextStack {

   private final LinkedList<Context> contextList;

   public ContextStack () {

      contextList = new LinkedList<Context>();
   }

   public synchronized boolean isEmpty () {

      return contextList.isEmpty();
   }

   public synchronized void push (Context context) {

      contextList.addFirst(context);
   }

   public synchronized Context peek () {

      if (contextList.isEmpty()) {
         return null;
      }

      return contextList.getFirst();
   }

   public synchronized Context pop () {

      if (!contextList.isEmpty()) {
         return contextList.removeFirst();
      }

      return null;
   }
}