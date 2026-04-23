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
package org.smallmind.claxon.registry.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a single static (compile-time constant) key/value tag to be attached to the
 * metrics emitted by an {@link Instrumented} method or constructor.
 *
 * <p>This annotation has no {@link java.lang.annotation.ElementType} targets of its own
 * (it is annotated with an empty {@code @Target({})}), meaning it is only valid as a
 * nested annotation inside the {@link Instrumented#constants()} array.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}Instrumented(
 *     parser  = MyParser.class,
 *     constants = {
 *         {@literal @}ConstantTag(key = "region", constant = "us-east-1")
 *     }
 * )
 * public void handleRequest() { ... }
 * </pre>
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstantTag {

  /**
   * The tag key that identifies this dimension in the metrics backend.
   *
   * @return the tag key; must not be empty
   */
  String key ();

  /**
   * The static string value assigned to this tag for every invocation of the instrumented
   * method or constructor.
   *
   * @return the constant tag value; must not be empty
   */
  String constant ();
}
