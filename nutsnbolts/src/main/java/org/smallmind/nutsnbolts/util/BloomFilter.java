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
package org.smallmind.nutsnbolts.util;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

/**
 * Bloom filter implementation backed by a {@link BitSet}. Supports configurable density and hash count.
 *
 * @param <E> element type providing bytes for hashing
 */
public class BloomFilter<E extends BloomFilterElement> implements Serializable {

  private final MessageDigest messageDigest;
  private final BitSet bitset;
  private final double bitsPerElement;
  private final int maxElements;
  private final int hashCount;
  private final int length;
  private int size;

  /**
   * @param bitsPerElement target bits per element
   * @param maxElements    maximum number of elements expected
   * @param hashCount      number of hash functions to apply
   * @throws NoSuchAlgorithmException if the SHA-1 digest cannot be instantiated
   */
  public BloomFilter (double bitsPerElement, int maxElements, int hashCount)
    throws NoSuchAlgorithmException {

    this.maxElements = maxElements;
    this.bitsPerElement = bitsPerElement;
    this.hashCount = hashCount;

    length = (int)Math.ceil(bitsPerElement * maxElements);
    bitset = new BitSet(length);
    size = 0;

    messageDigest = MessageDigest.getInstance(HashAlgorithm.SHA_1.getAlgorithmName());
  }

  /**
   * Convenience constructor that computes bits-per-element and hash count from a fixed bit length and capacity.
   */
  public BloomFilter (int length, int maxElements)
    throws NoSuchAlgorithmException {

    this(length / (double)maxElements, maxElements, (int)Math.round((length / (double)maxElements) * Math.log(2.0)));
  }

  /**
   * Convenience constructor that derives sizing from a desired false-positive probability and capacity.
   */
  public BloomFilter (double falsePositiveProbability, int maxElements)
    throws NoSuchAlgorithmException {

    this(-1 * maxElements * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2), maxElements, (int)Math.ceil((-1 * maxElements * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2)) / maxElements * Math.log(2)));
  }

  public double getBitsPerElement () {

    return this.bitsPerElement;
  }

  /**
   * @return configured maximum element count
   */
  public int getMaxElements () {

    return maxElements;
  }

  /**
   * @return number of hash functions in use
   */
  public int getHashCount () {

    return hashCount;
  }

  /**
   * @return underlying bitset length
   */
  public int length () {

    return length;
  }

  /**
   * @return number of elements added
   */
  public synchronized int size () {

    return this.size;
  }

  /**
   * Computes the current bits-per-element ratio based on {@link #size()} and length.
   */
  public double calculateCurrentBitsPerElement () {

    return this.length / (double)size;
  }

  /**
   * Estimates the current false-positive probability.
   */
  public double calculateFalsePositiveProbability () {

    // (1 - e^(-hashCount * maxElements / length)) ^ hashCount

    return Math.pow((1 - Math.exp(-hashCount * (double)maxElements / (double)length)), hashCount);
  }

  private int[] createHashes (byte[] elementBytes) {

    int[] hashes = new int[hashCount];
    int k = 0;
    byte salt = 0;

    while (k < hashCount) {

      byte[] hash;

      synchronized (messageDigest) {
        messageDigest.update(salt++);
        hash = messageDigest.digest(elementBytes);
      }

      for (int i = 0; i < hash.length / 4 && k < hashCount; i++) {

        int h = 0;

        for (int j = (i * 4); j < (i * 4) + 4; j++) {
          h <<= 8;
          h |= ((int)hash[j]) & 0xFF;
        }

        hashes[k++] = h;
      }
    }

    return hashes;
  }

  public void add (E element) {

    add(element.getBytes());
  }

  /**
   * Adds an element expressed as bytes to the filter.
   *
   * @param bytes element representation
   */
  public void add (byte[] bytes) {

    for (int hash : createHashes(bytes)) {
      bitset.set(Math.abs(hash % length), true);
    }

    size++;
  }

  /**
   * Adds all elements from the provided collection.
   *
   * @param c collection of elements to add
   */
  public void addAll (Collection<? extends E> c) {

    for (E element : c) {
      add(element);
    }
  }

  /**
   * Checks membership of an element.
   *
   * @param element element to test
   * @return {@code false} if definitely not present; {@code true} if possibly present
   */
  public boolean contains (E element) {

    return contains(element.getBytes());
  }

  /**
   * Checks membership of an element given its byte representation.
   *
   * @param bytes element representation
   * @return {@code false} if definitely not present; {@code true} if possibly present
   */
  public boolean contains (byte[] bytes) {

    for (int hash : createHashes(bytes)) {
      if (!bitset.get(Math.abs(hash % length))) {

        return false;
      }
    }

    return true;
  }

  public boolean containsAll (Collection<? extends E> c) {

    for (E element : c) {
      if (!contains(element)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Clears all bits and resets the size to zero.
   */
  public void clear () {

    bitset.clear();
    size = 0;
  }
}
