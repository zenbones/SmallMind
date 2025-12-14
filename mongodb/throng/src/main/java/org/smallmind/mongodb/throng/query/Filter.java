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
package org.smallmind.mongodb.throng.query;

import java.util.regex.Pattern;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.smallmind.nutsnbolts.util.MutationUtility;

/**
 * Fluent builder for MongoDB filters that wraps driver filters while preventing invalid states.
 */
public class Filter {

  private final String fieldName;
  private Bson bson;

  /**
   * Internal constructor used for composed filters.
   *
   * @param filterName descriptive name for error reporting
   * @param bson       underlying BSON filter
   */
  private Filter (String filterName, Bson bson) {

    this(filterName);

    this.bson = bson;
  }

  /**
   * Creates a filter builder scoped to a specific field.
   *
   * @param fieldName field to compare
   */
  public Filter (String fieldName) {

    this.fieldName = fieldName;
  }

  /**
   * Starts a filter builder for the provided field name.
   *
   * @param fieldName field to compare
   * @return new {@link Filter}
   */
  public static Filter where (String fieldName) {

    return new Filter(fieldName);
  }

  /**
   * @return an empty filter that matches all documents
   */
  public static Filter empty () {

    return new Filter("empty", com.mongodb.client.model.Filters.empty());
  }

  /**
   * Combines multiple filters with a logical AND.
   *
   * @param filters filters to combine
   * @return composed filter
   * @throws IllegalFilterStateException if any filter is incomplete
   */
  public static Filter and (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter("and", com.mongodb.client.model.Filters.empty());
    } else if (filters.length == 1) {
      if (filters[0].bson == null) {
        throw new IllegalFilterStateException("The filter(%s) is incomplete", filters[0].fieldName);
      } else {

        return filters[0];
      }
    } else {

      return new Filter("and", com.mongodb.client.model.Filters.and(MutationUtility.toArray(filters, Bson.class, filter -> {

        if (filter.bson == null) {
          throw new IllegalFilterStateException("The filter(%s) is incomplete", filter.fieldName);
        } else {

          return filter.bson;
        }
      })));
    }
  }

  /**
   * Combines multiple filters with a logical OR.
   *
   * @param filters filters to combine
   * @return composed filter
   * @throws IllegalFilterStateException if any filter is incomplete
   */
  public static Filter or (Filter... filters) {

    if ((filters == null) || (filters.length == 0)) {

      return new Filter("or", com.mongodb.client.model.Filters.empty());
    } else if (filters.length == 1) {
      if (filters[0].bson == null) {
        throw new IllegalFilterStateException("The filter(%s) is incomplete", filters[0].fieldName);
      } else {

        return filters[0];
      }
    } else {

      return new Filter("or", com.mongodb.client.model.Filters.or(MutationUtility.toArray(filters, Bson.class, filter -> {

        if (filter.bson == null) {
          throw new IllegalFilterStateException("The filter(%s) is incomplete", filter.fieldName);
        } else {

          return filter.bson;
        }
      })));
    }
  }

  /**
   * Adds an equals comparison for the field.
   *
   * @param value value to compare against
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter eq (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.eq(fieldName, value);

      return this;
    }
  }

  /**
   * Adds a not-equals comparison for the field.
   *
   * @param value value to compare against
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter ne (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.ne(fieldName, value);

      return this;
    }
  }

  /**
   * Adds a greater-than comparison.
   *
   * @param value lower bound value
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter gt (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.gt(fieldName, value);

      return this;
    }
  }

  /**
   * Adds a greater-than-or-equal comparison.
   *
   * @param value lower bound value
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter gte (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.gte(fieldName, value);

      return this;
    }
  }

  /**
   * Adds a less-than comparison.
   *
   * @param value upper bound value
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter lt (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.lt(fieldName, value);

      return this;
    }
  }

  /**
   * Adds a less-than-or-equal comparison.
   *
   * @param value upper bound value
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter lte (Object value) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.lte(fieldName, value);

      return this;
    }
  }

  /**
   * Adds an existence check for the field.
   *
   * @param exists whether the field must exist
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter exists (boolean exists) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.exists(fieldName, exists);

      return this;
    }
  }

  /**
   * Adds an inclusion check against an iterable of values.
   *
   * @param iterable values to match
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter in (Iterable<?> iterable) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.in(fieldName, iterable);

      return this;
    }
  }

  /**
   * Adds an inclusion check against provided values.
   *
   * @param values values to match
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter in (Object... values) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.in(fieldName, values);

      return this;
    }
  }

  /**
   * Adds a not-in comparison.
   *
   * @param values values to exclude
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter nin (Object... values) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.nin(fieldName, values);

      return this;
    }
  }

  /**
   * Adds a regular expression comparison for the field.
   *
   * @param pattern pattern to match
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has already been finalized
   */
  public Filter regex (Pattern pattern) {

    if (bson != null) {
      throw new IllegalFilterStateException("The filter(%s) is closed", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.regex(fieldName, pattern);

      return this;
    }
  }

  /**
   * Negates the current filter.
   *
   * @return this filter for chaining
   * @throws IllegalFilterStateException if the filter has not been initialized
   */
  public Filter not () {

    if (bson == null) {
      throw new IllegalFilterStateException("The filter(%s) is incomplete", fieldName);
    } else {

      bson = com.mongodb.client.model.Filters.not(bson);

      return this;
    }
  }

  /**
   * Converts the built filter into a BSON document suitable for driver operations.
   *
   * @param documentClass class of the target document
   * @param codecRegistry registry for encoding values
   * @return BSON representation of the filter
   * @throws UnsupportedOperationException if the filter has not been finalized
   */
  public Bson toBsonDocument (Class<?> documentClass, CodecRegistry codecRegistry) {

    if (bson == null) {
      throw new UnsupportedOperationException();
    } else {

      return bson.toBsonDocument(documentClass, codecRegistry);
    }
  }
}
