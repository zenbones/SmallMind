/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.persistence.orm.data.mongo.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.lang.Nullable;

public class Filter implements Criterion {

  private final Criteria criteria;
  private boolean complete;

  protected Filter (String key) {

    criteria = Criteria.where(key);
  }

  public static Filter where (String key) {

    return new Filter(key);
  }

  @Override
  public Criteria as () {

    return criteria;
  }

  public Filter and (String key) {

    if (!complete) {
      throw new UnsupportedOperationException();
    } else {
      criteria.and(key);
      complete = false;

      return this;
    }
  }

  public Filter isNull () {

    return eq(null);
  }

  public Filter lt (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.lt(value);
      complete = true;

      return this;
    }
  }

  public Filter lte (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.lte(value);
      complete = true;

      return this;
    }
  }

  public Filter eq (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.is(value);
      complete = true;

      return this;
    }
  }

  public Filter ne (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.ne(value);
      complete = true;

      return this;
    }
  }

  public Filter gte (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.gte(value);
      complete = true;

      return this;
    }
  }

  public Filter gt (Object value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.gt(value);
      complete = true;

      return this;
    }
  }

  public Filter in (Object... values) {

    return in(Arrays.asList(values));
  }

  public Filter in (Collection<?> values) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.in(values);
      complete = true;

      return this;
    }
  }

  public Filter nin (Object... values) {

    return nin(Arrays.asList(values));
  }

  public Filter nin (Collection<?> values) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.nin(values);
      complete = true;

      return this;
    }
  }

  public Filter exists (boolean value) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.exists(value);
      complete = true;

      return this;
    }
  }

  public Filter not () {

    if (!complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.not();

      return this;
    }
  }

  public Filter regex (String regex) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.regex(regex);
      complete = true;

      return this;
    }
  }

  public Filter regex (String regex, @Nullable String options) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.regex(regex, options);
      complete = true;

      return this;
    }
  }

  public Filter regex (Pattern pattern) {

    if (complete) {
      throw new UnsupportedOperationException();
    } else {

      criteria.regex(pattern);
      complete = true;

      return this;
    }
  }
}
