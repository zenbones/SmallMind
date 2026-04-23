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
 * A probabilistic membership filter backed by a {@link BitSet} that can report definite non-membership
 * or probable membership with a configurable false-positive rate.
 *
 * @param <E> the element type, which must supply a byte representation for hashing
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
   * Constructs a bloom filter with explicit sizing parameters.
   *
   * @param bitsPerElement the target number of bits allocated per expected element
   * @param maxElements    the maximum number of elements the filter is expected to hold
   * @param hashCount      the number of independent hash functions to apply per element
   * @throws NoSuchAlgorithmException if the SHA-1 message digest algorithm is unavailable
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
   * Constructs a bloom filter sized by a total bit length and expected capacity, deriving bits-per-element
   * and an optimal hash count automatically.
   *
   * @param length      the total number of bits in the filter
   * @param maxElements the maximum number of elements the filter is expected to hold
   * @throws NoSuchAlgorithmException if the SHA-1 message digest algorithm is unavailable
   */
  public BloomFilter (int length, int maxElements)
    throws NoSuchAlgorithmException {

    this(length / (double)maxElements, maxElements, (int)Math.round((length / (double)maxElements) * Math.log(2.0)));
  }

  /**
   * Constructs a bloom filter sized to achieve the given false-positive probability for the expected capacity,
   * computing the required bit length and hash count automatically.
   *
   * @param falsePositiveProbability the desired probability of a false positive (between 0 and 1)
   * @param maxElements              the maximum number of elements the filter is expected to hold
   * @throws NoSuchAlgorithmException if the SHA-1 message digest algorithm is unavailable
   */
  public BloomFilter (double falsePositiveProbability, int maxElements)
    throws NoSuchAlgorithmException {

    this(-1 * maxElements * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2), maxElements, (int)Math.ceil((-1 * maxElements * Math.log(falsePositiveProbability) / Math.pow(Math.log(2), 2)) / maxElements * Math.log(2)));
  }

  /**
   * Returns the configured bits-per-element density used when this filter was constructed.
   *
   * @return the bits-per-element value
   */
  public double getBitsPerElement () {

    return this.bitsPerElement;
  }

  /**
   * Returns the maximum number of elements this filter is configured to hold.
   *
   * @return the maximum element count
   */
  public int getMaxElements () {

    return maxElements;
  }

  /**
   * Returns the number of hash functions applied to each element.
   *
   * @return the hash function count
   */
  public int getHashCount () {

    return hashCount;
  }

  /**
   * Returns the total number of bits in the underlying bitset.
   *
   * @return the bitset length
   */
  public int length () {

    return length;
  }

  /**
   * Returns the number of elements that have been added to this filter.
   *
   * @return the current element count
   */
  public synchronized int size () {

    return this.size;
  }

  /**
   * Computes the actual bits-per-element ratio based on the current number of added elements.
   *
   * @return the current bits-per-element ratio
   */
  public double calculateCurrentBitsPerElement () {

    return this.length / (double)size;
  }

  /**
   * Estimates the current false-positive probability based on the configured hash count, maximum elements, and bit length.
   *
   * @return the estimated false-positive probability
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

  /**
   * Adds an element to the filter by hashing its byte representation.
   *
   * @param element the element to add
   */
  public void add (E element) {

    add(element.getBytes());
  }

  /**
   * Adds an element expressed as a raw byte array to the filter.
   *
   * @param bytes the byte representation of the element to add
   */
  public void add (byte[] bytes) {

    for (int hash : createHashes(bytes)) {
      bitset.set(Math.abs(hash % length), true);
    }

    size++;
  }

  /**
   * Adds all elements in the given collection to this filter.
   *
   * @param c the collection of elements to add
   */
  public void addAll (Collection<? extends E> c) {

    for (E element : c) {
      add(element);
    }
  }

  /**
   * Tests whether the given element is possibly present in the filter.
   *
   * @param element the element to test
   * @return {@code false} if the element is definitely not present; {@code true} if it is probably present
   */
  public boolean contains (E element) {

    return contains(element.getBytes());
  }

  /**
   * Tests whether an element represented by the given byte array is possibly present in the filter.
   *
   * @param bytes the byte representation of the element to test
   * @return {@code false} if the element is definitely not present; {@code true} if it is probably present
   */
  public boolean contains (byte[] bytes) {

    for (int hash : createHashes(bytes)) {
      if (!bitset.get(Math.abs(hash % length))) {

        return false;
      }
    }

    return true;
  }

  /**
   * Tests whether all elements in the given collection are possibly present in the filter.
   *
   * @param c the collection of elements to test
   * @return {@code true} if all elements are probably present; {@code false} if any element is definitely absent
   */
  public boolean containsAll (Collection<? extends E> c) {

    for (E element : c) {
      if (!contains(element)) {

        return false;
      }
    }

    return true;
  }

  /**
   * Resets the filter by clearing all bits and setting the element count to zero.
   */
  public void clear () {

    bitset.clear();
    size = 0;
  }
}
