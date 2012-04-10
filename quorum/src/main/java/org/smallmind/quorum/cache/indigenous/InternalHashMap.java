/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.quorum.cache.indigenous;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.smallmind.quorum.cache.CacheException;

public class InternalHashMap<K, V> {

   private static int MAXIMUM_CAPACITY = 1 << 30;

   private Lock readLock;
   private Lock writeLock;

   private ReentrantLock[] stripeLocks;
   private Entry[] table;
   private AtomicInteger size = new AtomicInteger(0);
   private AtomicInteger threshold;
   private float loadFactor;

   public InternalHashMap (ReentrantLock[] stripeLocks, int initialCapacity, float loadFactor)
      throws CacheException {

      ReadWriteLock readWriteLock;
      int capacity = 1;

      this.stripeLocks = stripeLocks;
      this.loadFactor = loadFactor;

      if (initialCapacity < 0) {
         throw new CacheException("Initial capacity(%d) must be >= 0", initialCapacity);
      }
      else if (initialCapacity > MAXIMUM_CAPACITY) {
         initialCapacity = MAXIMUM_CAPACITY;
      }
      else if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
         throw new CacheException("Load factor(%d) must be> 0 and a valid nuumber", loadFactor);
      }

      // Find a power of 2 >= initialCapacity && concurrencyLevel
      while ((capacity < initialCapacity) || (capacity < stripeLocks.length)) {
         capacity <<= 1;
      }

      threshold = new AtomicInteger((int)(capacity * loadFactor));
      table = new Entry[capacity];

      readWriteLock = new ReentrantReadWriteLock();
      readLock = readWriteLock.readLock();
      writeLock = readWriteLock.writeLock();
   }

   private static int hash (int h) {

      h ^= (h >>> 20) ^ (h >>> 12);

      return h ^ (h >>> 7) ^ (h >>> 4);
   }

   private int indexFor (K key, int h, int length) {

      return (((h & (length - 1)) / stripeLocks.length) * stripeLocks.length) + Math.abs(key.hashCode() % stripeLocks.length);
   }

   public int size () {

      return size.get();
   }

   public boolean isEmpty () {

      return size.get() == 0;
   }

   public V get (K key) {

      Entry<K, V> entry;

      return ((entry = getEntry(key)) == null) ? null : entry.value;
   }

   public boolean containsKey (K key) {

      return getEntry(key) != null;
   }

   private Entry<K, V> getEntry (K key) {

      if (key == null) {
         throw new NullPointerException("AbstractCache does not accept null keys");
      }

      int hash = hash(key.hashCode());

      readLock.lock();
      try {
         for (Entry<K, V> entry = table[indexFor(key, hash, table.length)]; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && ((entry.key == key) || key.equals(entry.key)))

               return entry;
         }
      }
      finally {
         readLock.unlock();
      }

      return null;
   }

   public V put (K key, V value) {

      return putEntry(key, value, false);
   }

   public V putIfAbsent (K key, V value) {

      return putEntry(key, value, true);
   }

   public V putEntry (K key, V value, boolean onlyIfAbsent) {

      if (key == null) {
         throw new NullPointerException("AbstractCache does not accept null keys");
      }

      int hash = hash(key.hashCode());

      readLock.lock();
      try {

         int bucketIndex = indexFor(key, hash, table.length);

         for (Entry<K, V> entry = table[bucketIndex]; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && ((entry.key == key) || key.equals(entry.key))) {

               V prevValue = entry.value;

               if (!onlyIfAbsent) {
                  entry.value = value;
               }

               return prevValue;
            }
         }
      }
      finally {
         readLock.unlock();
      }

      addEntry(hash, key, value);

      return null;
   }

   private void addEntry (int hash, K key, V value) {

      readLock.lock();
      try {

         int bucketIndex = indexFor(key, hash, table.length);

         Entry<K, V> entry = table[bucketIndex];

         table[bucketIndex] = new Entry<K, V>(hash, key, value, entry);
      }
      finally {
         readLock.unlock();
      }

      if (size.getAndIncrement() >= threshold.get()) {
         resize();
      }
   }

   private void resize () {

      writeLock.lock();
      try {
         if (size.getAndIncrement() >= threshold.get()) {

            Entry[] oldTable = table;
            int oldCapacity = oldTable.length;
            int newCapacity = oldTable.length * 2;

            if (oldCapacity == MAXIMUM_CAPACITY) {
               threshold.set(Integer.MAX_VALUE);
            }
            else {

               Entry[] newTable = new Entry[newCapacity];

               transfer(newTable);
               table = newTable;
               threshold.set((int)(newCapacity * loadFactor));
            }
         }
      }
      finally {
         writeLock.unlock();
      }
   }

   private void transfer (Entry[] newTable) {

      int newCapacity = newTable.length;

      for (int count = 0; count < table.length; count++) {

         Entry<K, V> entry = table[count];

         if (entry != null) {
            table[count] = null;
            do {

               Entry<K, V> nextEntry = entry.next;
               int bucketIndex = indexFor(entry.key, entry.hash, newCapacity);

               entry.next = newTable[bucketIndex];
               newTable[bucketIndex] = entry;
               entry = nextEntry;
            } while (entry != null);
         }
      }
   }

   public V remove (K key) {

      Entry<K, V> entry;

      return ((entry = removeEntryForKey(key)) == null ? null : entry.value);
   }

   private Entry<K, V> removeEntryForKey (K key) {

      if (key == null) {

         return null;
      }

      readLock.lock();
      try {
         int hash = hash(key.hashCode());
         int bucketIndex = indexFor(key, hash, table.length);

         Entry<K, V> prevEntry = table[bucketIndex];
         Entry<K, V> entry = prevEntry;

         while (entry != null) {

            Entry<K, V> nextEntry = entry.next;

            if ((entry.hash == hash) && ((entry.key == key) || key.equals(entry.key))) {
               size.decrementAndGet();
               if (prevEntry == entry) {
                  table[bucketIndex] = nextEntry;
               }
               else {
                  prevEntry.next = nextEntry;
               }

               return entry;
            }

            prevEntry = entry;
            entry = nextEntry;
         }

         return null;
      }
      finally {
         readLock.unlock();
      }
   }

   public void clear () {

      writeLock.lock();
      try {
         for (int i = 0; i < table.length; i++) {
            table[i] = null;
         }

         size.set(0);
      }
      finally {
         writeLock.unlock();
      }
   }

   public KeyIterator keyIterator () {

      return new KeyIterator();
   }

   private static class Entry<K, V> {

      private K key;
      private int hash;

      private Entry<K, V> next;
      private V value;

      Entry (int hash, K key, V value, Entry<K, V> next) {

         this.hash = hash;
         this.key = key;
         this.value = value;
         this.next = next;
      }

      public K getKey () {

         return key;
      }

      public V getValue () {

         return value;
      }

      public V setValue (V newValue) {

         V oldValue = value;
         value = newValue;

         return oldValue;
      }

      public boolean equals (Object obj) {

         if (obj instanceof Entry) {

            Entry entry = (Entry)obj;
            K key1 = getKey();
            Object key2 = entry.getKey();

            if (key1 == key2 || ((key1 != null) && key1.equals(key2))) {

               V value1 = getValue();
               Object value2 = entry.getValue();

               if (value1 == value2 || ((value1 != null) && value1.equals(value2))) {
                  return true;
               }
            }
         }

         return false;
      }

      public int hashCode () {

         return key.hashCode() ^ (value == null ? 0 : value.hashCode());
      }
   }

   private class KeyIterator implements Iterator<K>, Iterable<K> {

      Entry<K, V> nextEntry;
      int index = 0;

      KeyIterator () {

         if (size.get() > 0) {
            findNextBucket();
         }
      }

      private void findNextBucket () {

         writeLock.lock();
         try {
            while (index < table.length && ((nextEntry = table[index++]) == null)) ;
         }
         finally {
            writeLock.unlock();
         }
      }

      public Iterator<K> iterator () {

         return this;
      }

      public boolean hasNext () {

         return nextEntry != null;
      }

      public K next () {

         if (nextEntry == null) {
            throw new NoSuchElementException();
         }

         ReentrantLock stripeLock = stripeLocks[Math.abs(nextEntry.key.hashCode() % stripeLocks.length)];
         Entry<K, V> entry = nextEntry;

         stripeLock.lock();
         try {
            readLock.lock();
            try {
               nextEntry = entry.next;
            }
            finally {
               readLock.unlock();
            }
         }
         finally {
            stripeLock.unlock();
         }

         if (nextEntry == null) {
            findNextBucket();
         }

         return entry.getKey();
      }

      public void remove () {

         throw new UnsupportedOperationException();
      }
   }
}
