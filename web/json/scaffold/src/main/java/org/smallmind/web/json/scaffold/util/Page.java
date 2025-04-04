/*
 * Copyright (c) 2007 through 2024 David Berkman
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

@XmlRootElement(name = "page", namespace = "http://org.smallmind/web/json/scaffold/fault")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Page<T> implements Iterable<T> {

  private T[] values;
  private long totalResults;
  private long firstResult;
  private int maxResults;
  private int resultSize;

  public Page () {

  }

  public Page (List<T> listOfValues, long firstResult, int maxResults, long totalResults) {

    this(fromList(listOfValues), firstResult, maxResults, totalResults);
  }

  public Page (T[] values, long firstResult, int maxResults, long totalResults) {

    this.values = values;
    this.firstResult = firstResult;
    this.maxResults = maxResults;
    this.totalResults = totalResults;

    resultSize = values.length;
  }

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

  public <U> Page<U> mutate (Class<U> outType, Mutation<? super T, U> mutation)
    throws Exception {

    U[] outArray = (U[])Array.newInstance(outType, values.length);
    int index = 0;

    for (T inType : this) {
      outArray[index++] = mutation.apply(inType);
    }

    return new Page<>(outArray, firstResult, maxResults, totalResults);
  }

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

  @Override
  public Iterator<T> iterator () {

    return Arrays.asList(values).iterator();
  }

  @XmlElement(name = "firstResult", required = true, nillable = false)
  public long getFirstResult () {

    return firstResult;
  }

  public void setFirstResult (long firstResult) {

    this.firstResult = firstResult;
  }

  @XmlElement(name = "maxResults", required = true, nillable = false)
  public int getMaxResults () {

    return maxResults;
  }

  public void setMaxResults (int maxResults) {

    this.maxResults = maxResults;
  }

  @XmlElement(name = "resultSize", required = true, nillable = false)
  public int getResultSize () {

    return resultSize;
  }

  public void setResultSize (int resultSize) {

    this.resultSize = resultSize;
  }

  @XmlElement(name = "totalResults", required = true, nillable = false)
  public long getTotalResults () {

    return totalResults;
  }

  public void setTotalResults (long totalResults) {

    this.totalResults = totalResults;
  }

  @XmlAnyElement()
  public T[] getValues () {

    return values;
  }

  public void setValues (T[] values) {

    this.values = values;
  }
}
