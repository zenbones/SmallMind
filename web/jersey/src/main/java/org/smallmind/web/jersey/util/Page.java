package org.smallmind.nutsnbolts.json;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "page")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Page<T> implements Iterable<T> {

  private T[] values;
  private long totalResults;
  private int firstResult;
  private int maxResults;
  private int resultSize;

  public Page () {

  }

  public Page (List<T> listOfValues, int firstResult, int maxResults, long totalResults) {

    this(fromList(listOfValues), firstResult, maxResults, totalResults);
  }

  public Page (T[] values, int firstResult, int maxResults, long totalResults) {

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

  public <U> Page<U> mutate (PageMutation<? super T, U> mutation)
    throws Exception {

    U[] outArray = (U[])Array.newInstance(mutation.getMutatedClass(), values.length);
    int index = 0;

    for (T inType : this) {
      outArray[index++] = mutation.mutate(inType);
    }

    return new Page<>(outArray, firstResult, maxResults, totalResults);
  }

  public Page<T> jsonConvert (Class<?> arrayClass) {

    setValues((T[])JsonCodec.convert(getValues(), arrayClass));

    return this;
  }

  @Override
  public Iterator<T> iterator () {

    return Arrays.asList(values).iterator();
  }

  @XmlElement(name = "first_result", required = true, nillable = false)
  public int getFirstResult () {

    return firstResult;
  }

  public void setFirstResult (int firstResult) {

    this.firstResult = firstResult;
  }

  @XmlElement(name = "max_results", required = true, nillable = false)
  public int getMaxResults () {

    return maxResults;
  }

  public void setMaxResults (int maxResults) {

    this.maxResults = maxResults;
  }

  @XmlElement(name = "result_size", required = true, nillable = false)
  public int getResultSize () {

    return resultSize;
  }

  public void setResultSize (int resultSize) {

    this.resultSize = resultSize;
  }

  @XmlElement(name = "total_results", required = true, nillable = false)
  public long getTotalResults () {

    return totalResults;
  }

  public void setTotalResults (long totalResults) {

    this.totalResults = totalResults;
  }

  @XmlElement(name = "values", required = true, nillable = false)
  @XmlAnyElement
  public T[] getValues () {

    return values;
  }

  public void setValues (T[] values) {

    this.values = values;
  }
}