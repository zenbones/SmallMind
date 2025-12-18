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
package org.smallmind.web.json.scaffold.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.smallmind.nutsnbolts.util.Mutation;

/**
 * Generic container representing a page of results along with pagination metadata.
 *
 * @param <T> element type within the page
 */
@XmlRootElement(name = "page", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Page<T> implements Iterable<T> {

  private T[] values;
  private long totalResults;
  private long firstResult;
  private int maxResults;
  private int resultSize;

  /**
   * No-arg constructor for JAXB/Jackson.
   */
  public Page () {

  }

  /**
   * Builds a page from a list of values.
   *
   * @param listOfValues page contents
   * @param firstResult  index of the first result in the overall result set
   * @param maxResults   page size
   * @param totalResults total number of results across all pages
   */
  public Page (List<T> listOfValues, long firstResult, int maxResults, long totalResults) {

    this(fromList(listOfValues), firstResult, maxResults, totalResults);
  }

  /**
   * Builds a page from an array of values.
   *
   * @param values       page contents
   * @param firstResult  index of the first result in the overall result set
   * @param maxResults   page size
   * @param totalResults total number of results across all pages
   */
  public Page (T[] values, long firstResult, int maxResults, long totalResults) {

    this.values = values;
    this.firstResult = firstResult;
    this.maxResults = maxResults;
    this.totalResults = totalResults;

    resultSize = values.length;
  }

  /**
   * Creates an empty page for the given component type.
   *
   * @param arrayClass component class for the values array
   * @param <T>        component type
   * @return empty page
   */
  public static <T> Page<T> empty (Class<T> arrayClass) {

    return new Page<>((T[])Array.newInstance(arrayClass, 0), 0, 0, 0);
  }

  private static <T> T[] fromList (List<T> listOfValues) {

    T[] values = (T[])new Object[(listOfValues == null) ? 0 : listOfValues.size()];

    if (listOfValues != null) {
      listOfValues.toArray(values);
    }

    return values;
  }

  /**
   * Transforms the contents of this page into another type using the supplied mutation function.
   *
   * @param outType  target component class
   * @param mutation transformation applied to each element
   * @param <U>      target element type
   * @return a new page containing the transformed elements and the same paging metadata
   * @throws Exception if the mutation function throws
   */
  public <U> Page<U> mutate (Class<U> outType, Mutation<? super T, U> mutation)
    throws Exception {

    U[] outArray = (U[])Array.newInstance(outType, values.length);
    int index = 0;

    for (T inType : this) {
      outArray[index++] = mutation.apply(inType);
    }

    return new Page<>(outArray, firstResult, maxResults, totalResults);
  }

  /**
   * Converts the underlying value array to the requested component type via JSON conversion.
   *
   * @param componentClass target component type
   * @return this page for chaining
   */
  public Page<T> jsonConvert (Class<T> componentClass) {

    if ((values == null) || (!values.getClass().getComponentType().equals(componentClass))) {

      T[] convertedArray = (T[])Array.newInstance(componentClass, (values == null) ? 0 : values.length);

      int index = 0;

      for (Object obj : getValues()) {
        convertedArray[index++] = JsonCodec.convert(obj, componentClass);
      }

      values = convertedArray;
    }

    return this;
  }

  /**
   * @return iterator over the page values
   */
  @Override
  public Iterator<T> iterator () {

    return Arrays.asList(values).iterator();
  }

  /**
   * @return zero-based index of the first result
   */
  @XmlElement(name = "firstResult", required = true, nillable = false)
  public long getFirstResult () {

    return firstResult;
  }

  /**
   * Sets the zero-based index of the first result.
   *
   * @param firstResult first result index
   */
  public void setFirstResult (long firstResult) {

    this.firstResult = firstResult;
  }

  /**
   * @return configured maximum number of results per page
   */
  @XmlElement(name = "maxResults", required = true, nillable = false)
  public int getMaxResults () {

    return maxResults;
  }

  /**
   * Sets the maximum number of results per page.
   *
   * @param maxResults page size
   */
  public void setMaxResults (int maxResults) {

    this.maxResults = maxResults;
  }

  /**
   * @return number of results contained in this page
   */
  @XmlElement(name = "resultSize", required = true, nillable = false)
  public int getResultSize () {

    return resultSize;
  }

  /**
   * Sets the number of results contained in this page.
   *
   * @param resultSize result count
   */
  public void setResultSize (int resultSize) {

    this.resultSize = resultSize;
  }

  /**
   * @return total number of results across all pages
   */
  @XmlElement(name = "totalResults", required = true, nillable = false)
  public long getTotalResults () {

    return totalResults;
  }

  /**
   * Sets the total number of results across all pages.
   *
   * @param totalResults total result count
   */
  public void setTotalResults (long totalResults) {

    this.totalResults = totalResults;
  }

  /**
   * @return array of page values
   */
  @XmlAnyElement()
  public T[] getValues () {

    return values;
  }

  /**
   * Sets the page values.
   *
   * @param values array of values
   */
  public void setValues (T[] values) {

    this.values = values;
  }
}
