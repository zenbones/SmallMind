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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods that apply a {@link Mutation} to every element of an array, iterator, or collection and collect the results into a new array, list, set, map, or bag.
 */
public class MutationUtility {

  /**
   * Transforms each element of the source array using the mutation and returns a new typed array of the results.
   *
   * @param array    source array; returns {@code null} when {@code null}
   * @param outType  component type of the output array
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return array of transformed values, or {@code null} if {@code array} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> U[] toArray (T[] array, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    return toArray((array == null) ? null : Arrays.asList(array), outType, mutation);
  }

  /**
   * Transforms each remaining element of the iterator using the mutation and returns a new typed array of the results.
   *
   * @param iterator source iterator; returns {@code null} when {@code null}
   * @param outType  component type of the output array
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return array of transformed values, or {@code null} if {@code iterator} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> U[] toArray (Iterator<T> iterator, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    if (iterator == null) {

      return null;
    } else {

      LinkedList<T> listOfT = new LinkedList<>();

      iterator.forEachRemaining(listOfT::add);

      return toArray(listOfT, outType, mutation);
    }
  }

  /**
   * Transforms each element of the collection using the mutation and returns a new typed array of the results.
   *
   * @param collection source collection; returns {@code null} when {@code null}
   * @param outType    component type of the output array
   * @param mutation   transformation applied to each element
   * @param <T>        input element type
   * @param <U>        output element type
   * @return array of transformed values, or {@code null} if {@code collection} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> U[] toArray (Collection<T> collection, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    if (collection == null) {

      return null;
    } else {
      try {

        U[] outArray = (U[])Array.newInstance(outType, collection.size());
        int index = 0;

        for (T inType : collection) {
          outArray[index++] = mutation.apply(inType);
        }

        return outArray;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }

  /**
   * Transforms each element of the source array using the mutation and collects the results into a {@link List}.
   *
   * @param array    source array; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return list of transformed values, or {@code null} if {@code array} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> List<U> toList (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toList((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms each remaining element of the iterator using the mutation and collects the results into a {@link List}.
   *
   * @param iterator source iterator; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return list of transformed values, or {@code null} if {@code iterator} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> List<U> toList (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toList((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms each element of the iterable using the mutation and collects the results into a {@link List}.
   *
   * @param iterable source iterable; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return list of transformed values, or {@code null} if {@code iterable} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> List<U> toList (Iterable<T> iterable, Mutation<? super T, U> mutation)
    throws MutationException {

    if (iterable == null) {

      return null;
    } else {
      try {

        LinkedList<U> outList = new LinkedList<>();

        for (T inType : iterable) {
          outList.add(mutation.apply(inType));
        }

        return outList;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }

  /**
   * Transforms each element of the source array using the mutation and collects the results into a {@link Set}.
   *
   * @param array    source array; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return set of transformed values, or {@code null} if {@code array} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Set<U> toSet (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toSet((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms each remaining element of the iterator using the mutation and collects the results into a {@link Set}.
   *
   * @param iterator source iterator; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return set of transformed values, or {@code null} if {@code iterator} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Set<U> toSet (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toSet((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms each element of the iterable using the mutation and collects the results into a {@link Set}.
   *
   * @param iterable source iterable; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return set of transformed values, or {@code null} if {@code iterable} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Set<U> toSet (Iterable<T> iterable, Mutation<? super T, U> mutation)
    throws MutationException {

    if (iterable == null) {

      return null;
    } else {
      try {

        HashSet<U> outSet = new HashSet<>();

        for (T inType : iterable) {
          outSet.add(mutation.apply(inType));
        }

        return outSet;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }

  /**
   * Transforms each element of the source array into a map entry using separate key and value mutations.
   *
   * @param array         source array; returns {@code null} when {@code null}
   * @param keyMutation   transformation applied to produce map keys
   * @param valueMutation transformation applied to produce map values
   * @param <T>           input element type
   * @param <K>           map key type
   * @param <U>           map value type
   * @return map of transformed entries, or {@code null} if {@code array} is {@code null}
   * @throws MutationException if either mutation throws during any element transformation
   */
  public static <T, K, U> Map<K, U> toMap (T[] array, Mutation<T, K> keyMutation, Mutation<? super T, U> valueMutation)
    throws MutationException {

    return toMap((array == null) ? null : Arrays.asList(array), keyMutation, valueMutation);
  }

  /**
   * Transforms each remaining element of the iterator into a map entry using separate key and value mutations.
   *
   * @param iterator      source iterator; returns {@code null} when {@code null}
   * @param keyMutation   transformation applied to produce map keys
   * @param valueMutation transformation applied to produce map values
   * @param <T>           input element type
   * @param <K>           map key type
   * @param <U>           map value type
   * @return map of transformed entries, or {@code null} if {@code iterator} is {@code null}
   * @throws MutationException if either mutation throws during any element transformation
   */
  public static <T, K, U> Map<K, U> toMap (Iterator<T> iterator, Mutation<T, K> keyMutation, Mutation<? super T, U> valueMutation)
    throws MutationException {

    return (iterator == null) ? null : toMap((Iterable<T>)new IterableIterator<>(iterator), keyMutation, valueMutation);
  }

  /**
   * Transforms each element of the iterable into a map entry using separate key and value mutations.
   *
   * @param iterable      source iterable; returns {@code null} when {@code null}
   * @param keyMutation   transformation applied to produce map keys
   * @param valueMutation transformation applied to produce map values
   * @param <T>           input element type
   * @param <K>           map key type
   * @param <U>           map value type
   * @return map of transformed entries, or {@code null} if {@code iterable} is {@code null}
   * @throws MutationException if either mutation throws during any element transformation
   */
  public static <T, K, U> Map<K, U> toMap (Iterable<T> iterable, Mutation<T, K> keyMutation, Mutation<? super T, U> valueMutation)
    throws MutationException {

    if (iterable == null) {

      return null;
    } else {
      try {

        HashMap<K, U> outMap = new HashMap<>();

        for (T inType : iterable) {
          outMap.put(keyMutation.apply(inType), valueMutation.apply(inType));
        }

        return outMap;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }

  /**
   * Transforms each element of the source array using the mutation and collects the results into a {@link Bag} multiset.
   *
   * @param array    source array; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return bag of transformed values, or {@code null} if {@code array} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Bag<U> toBag (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toBag((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms each remaining element of the iterator using the mutation and collects the results into a {@link Bag} multiset.
   *
   * @param iterator source iterator; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return bag of transformed values, or {@code null} if {@code iterator} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Bag<U> toBag (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toBag((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms each element of the iterable using the mutation and collects the results into a {@link Bag} multiset.
   *
   * @param iterable source iterable; returns {@code null} when {@code null}
   * @param mutation transformation applied to each element
   * @param <T>      input element type
   * @param <U>      output element type
   * @return bag of transformed values, or {@code null} if {@code iterable} is {@code null}
   * @throws MutationException if the mutation throws during any element transformation
   */
  public static <T, U> Bag<U> toBag (Iterable<T> iterable, Mutation<? super T, U> mutation)
    throws MutationException {

    if (iterable == null) {

      return null;
    } else {
      try {

        HashBag<U> outBag = new HashBag<>();

        for (T inType : iterable) {
          outBag.add(mutation.apply(inType));
        }

        return outBag;
      } catch (Exception exception) {
        throw new MutationException(exception);
      }
    }
  }
}
