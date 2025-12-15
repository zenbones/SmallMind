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
 * Bulk transformation helpers that apply {@link Mutation} logic across collections/arrays/iterators into various output forms.
 */
public class MutationUtility {

  /**
   * Transforms an array to another typed array by applying a mutation to each element.
   *
   * @param array    source array; may be {@code null}
   * @param outType  component class of the output array
   * @param mutation transformation to apply
   * @return array of transformed values or {@code null} if the source was null
   * @throws MutationException if the mutation throws
   */
  public static <T, U> U[] toArray (T[] array, Class<U> outType, Mutation<? super T, U> mutation)
    throws MutationException {

    return toArray((array == null) ? null : Arrays.asList(array), outType, mutation);
  }

  /**
   * Applies a mutation to each element of an iterator, returning an array of the specified component type.
   *
   * @param iterator source iterator; may be {@code null}
   * @param outType  component class of the output array
   * @param mutation transformation to apply
   * @return array of transformed values or {@code null} if the iterator was null
   * @throws MutationException if the mutation throws
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
   * Applies a mutation to each element of a collection, returning an array of the specified component type.
   *
   * @param collection source collection; may be {@code null}
   * @param outType    component class of the output array
   * @param mutation   transformation to apply
   * @param <T>        input type
   * @param <U>        output type
   * @return array of transformed values or {@code null} if the collection was null
   * @throws MutationException if the mutation throws
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
   * Transforms elements from an array into a {@link List}.
   *
   * @param array    source array; may be {@code null}
   * @param mutation transformation to apply
   * @param <T>      input type
   * @param <U>      output type
   * @return list of transformed values or {@code null} if the array was null
   * @throws MutationException if the mutation throws
   */
  public static <T, U> List<U> toList (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toList((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms elements from an iterator into a {@link List}.
   */
  public static <T, U> List<U> toList (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toList((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms elements from an iterable into a {@link List}.
   *
   * @param iterable source iterable; may be {@code null}
   * @param mutation transformation to apply
   * @return list of transformed values or {@code null} if the iterable was null
   * @throws MutationException if the mutation throws
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
   * Transforms elements from an array into a {@link Set}.
   *
   * @param array    source array; may be {@code null}
   * @param mutation transformation to apply
   * @param <T>      input type
   * @param <U>      output type
   * @return set of transformed values or {@code null} if the array was null
   * @throws MutationException if the mutation throws
   */
  public static <T, U> Set<U> toSet (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toSet((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms elements from an iterator into a {@link Set}.
   *
   * @param iterator source iterator; may be {@code null}
   * @param mutation transformation to apply
   * @param <T>      input type
   * @param <U>      output type
   * @return set of transformed values or {@code null} if the iterator was null
   * @throws MutationException if the mutation throws
   */
  public static <T, U> Set<U> toSet (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toSet((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms elements from an iterable into a {@link Set}.
   *
   * @param iterable source iterable; may be {@code null}
   * @param mutation transformation to apply
   * @param <T>      input type
   * @param <U>      output type
   * @return set of transformed values or {@code null} if the iterable was null
   * @throws MutationException if the mutation throws
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
   * Transforms elements from an array into a {@link Map} by applying separate key and value mutations.
   *
   * @param array         source array; may be {@code null}
   * @param keyMutation   transformation for keys
   * @param valueMutation transformation for values
   * @param <T>           input type
   * @param <K>           key type
   * @param <U>           value type
   * @return map of transformed entries or {@code null} if the array was null
   * @throws MutationException if either mutation throws
   */
  public static <T, K, U> Map<K, U> toMap (T[] array, Mutation<T, K> keyMutation, Mutation<? super T, U> valueMutation)
    throws MutationException {

    return toMap((array == null) ? null : Arrays.asList(array), keyMutation, valueMutation);
  }

  /**
   * Transforms elements from an iterator into a {@link Map} by applying separate key and value mutations.
   *
   * @param iterator      source iterator; may be {@code null}
   * @param keyMutation   transformation for keys
   * @param valueMutation transformation for values
   * @param <T>           input type
   * @param <K>           key type
   * @param <U>           value type
   * @return map of transformed entries or {@code null} if the iterator was null
   * @throws MutationException if either mutation throws
   */
  public static <T, K, U> Map<K, U> toMap (Iterator<T> iterator, Mutation<T, K> keyMutation, Mutation<? super T, U> valueMutation)
    throws MutationException {

    return (iterator == null) ? null : toMap((Iterable<T>)new IterableIterator<>(iterator), keyMutation, valueMutation);
  }

  /**
   * Transforms elements from an iterable into a {@link Map} by applying separate key and value mutations.
   *
   * @param iterable      source iterable; may be {@code null}
   * @param keyMutation   transformation for keys
   * @param valueMutation transformation for values
   * @return map of transformed entries or {@code null} if the iterable was null
   * @throws MutationException if either mutation throws
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
   * Transforms elements from an array into a {@link Bag} (multiset).
   *
   * @param array    source array; may be {@code null}
   * @param mutation transformation to apply
   * @param <T>      input type
   * @param <U>      output type
   * @return bag of transformed values or {@code null} if the array was null
   * @throws MutationException if the mutation throws
   */
  public static <T, U> Bag<U> toBag (T[] array, Mutation<? super T, U> mutation)
    throws MutationException {

    return toBag((array == null) ? null : Arrays.asList(array), mutation);
  }

  /**
   * Transforms elements from an iterator into a {@link Bag} (multiset).
   */
  public static <T, U> Bag<U> toBag (Iterator<T> iterator, Mutation<? super T, U> mutation)
    throws MutationException {

    return (iterator == null) ? null : toBag((Iterable<T>)new IterableIterator<>(iterator), mutation);
  }

  /**
   * Transforms elements from an iterable into a {@link Bag} (multiset).
   *
   * @param iterable source iterable; may be {@code null}
   * @param mutation transformation to apply
   * @return bag of transformed values or {@code null} if the iterable was null
   * @throws MutationException if the mutation throws
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
