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
package org.smallmind.quorum.bucket;

/**
 * Strategy that determines which child bucket in a {@link TokenBucket} hierarchy should govern a given input.
 * <p>
 * When a parent bucket has registered child buckets via
 * {@link TokenBucket#add(BucketKey, BucketFactory)}, this selector is consulted on every
 * {@link TokenBucket#allowed(Object)} call to identify the key whose child must also
 * permit the input before the parent deducts tokens. Returning {@code null} bypasses
 * the child-lookup step for that input.
 *
 * @param <T> the type of value inspected to produce a key
 */
public interface BucketSelector<T> {

  /**
   * Determines the child-bucket key that should apply to {@code input}.
   *
   * @param input the value being evaluated for rate-limiting
   * @return the {@link BucketKey} identifying the child bucket that must also permit {@code input},
   * or {@code null} if no child constraint applies
   */
  BucketKey<T> selection (T input);
}
