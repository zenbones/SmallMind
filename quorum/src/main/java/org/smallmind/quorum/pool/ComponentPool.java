package org.smallmind.quorum.pool;

import java.util.LinkedList;

public class ComponentPool<T> {

   private PoolMode poolMode;
   private ComponentFactory<T> componentFactory;
   private LinkedList<T> usedList;
   private LinkedList<T> freeList;
   private int size;

   public ComponentPool (ComponentFactory<T> componentFactory, int size, PoolMode poolMode) {

      this.componentFactory = componentFactory;
      this.size = size;
      this.poolMode = poolMode;

      usedList = new LinkedList<T>();
      freeList = new LinkedList<T>();
   }

   public synchronized T getComponent ()
      throws ComponentPoolException {

      T component = null;

      if (freeList.isEmpty()) {
         if ((usedList.size() < size) || poolMode.equals(PoolMode.EXPANDING_POOL)) {
            try {
               component = componentFactory.createComponent();
            }
            catch (Exception e) {
               throw new ComponentPoolException(e);
            }
         }
         else if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            try {
               do {
                  wait();

                  if (!freeList.isEmpty()) {
                     component = freeList.remove(0);
                  }
               } while (component == null);
            }
            catch (InterruptedException i) {
               throw new ComponentPoolException(i);
            }
         }
         else {
            throw new ComponentPoolException("Fixed ComponentPool(%s) is completely booked", componentFactory.getClass().getSimpleName());
         }
      }
      else {
         component = freeList.remove(0);
      }

      usedList.add(component);

      return component;
   }

   public synchronized void returnComponent (T component) {

      usedList.remove(component);

      if ((usedList.size() + freeList.size()) < size) {
         freeList.add(component);

         if (poolMode.equals(PoolMode.BLOCKING_POOL)) {
            notify();
         }
      }
   }

   public synchronized int poolSize () {

      return freeList.size() + usedList.size();
   }

   public synchronized int freeSize () {

      return freeList.size();
   }
}
