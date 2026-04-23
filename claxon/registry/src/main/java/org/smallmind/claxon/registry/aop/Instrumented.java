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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;
import org.smallmind.claxon.registry.meter.Meter;

/**
 * Marks a method or constructor for automatic metrics instrumentation via the
 * {@link InstrumentedAspect} AOP advice.
 *
 * <p>When the annotated join point is intercepted, {@link InstrumentedAspect} performs the
 * following steps:</p>
 * <ol>
 *   <li>Resolves the {@link #caller()} class to use as the metric name prefix.</li>
 *   <li>Assembles the full tag set from {@link #constants()} and {@link #parameters()}.</li>
 *   <li>Instantiates {@link #parser()} via its no-arg constructor and calls
 *       {@link InstrumentedParser#parse(String)} with {@link #json()} to obtain a meter builder.</li>
 *   <li>Records execution time through the resulting meter in the unit specified by
 *       {@link #timeUnit()}.</li>
 * </ol>
 *
 * <p>Instrumentation can be disabled at any time by setting {@link #active()} to {@code false},
 * which causes the aspect to pass through to the join point with no overhead.</p>
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Instrumented {

  /**
   * The class used to derive the metric name prefix for this join point.
   *
   * <p>When left as the default ({@link Instrumented}{@code .class}), the aspect uses the
   * declaring type of the intercepted join point instead.</p>
   *
   * @return the caller class, or {@link Instrumented}{@code .class} to use the declaring type
   */
  Class<?> caller () default Instrumented.class;

  /**
   * The {@link InstrumentedParser} implementation responsible for converting {@link #json()}
   * into a concrete {@link org.smallmind.claxon.registry.meter.MeterBuilder}.
   *
   * <p>The class must expose a public no-argument constructor so the aspect can instantiate it
   * via reflection.</p>
   *
   * @return the parser class; must not be {@code null}
   */
  Class<? extends InstrumentedParser<? extends Meter>> parser ();

  /**
   * A JSON string passed verbatim to {@link #parser()} to configure the meter builder.
   *
   * <p>The exact schema is defined by the chosen {@link InstrumentedParser} implementation.
   * An empty JSON object ({@code "{}"}) is accepted as a valid default by most parsers.</p>
   *
   * @return JSON configuration for the parser; defaults to {@code "{}"}
   */
  String json () default "{}";

  /**
   * Static key/value tags that are attached to every metric emitted by this join point.
   *
   * <p>Tag values are fixed at annotation-declaration time and do not vary between invocations.
   * Use {@link #parameters()} for tags whose values must be extracted from method arguments.</p>
   *
   * @return array of constant tags; defaults to an empty array
   */
  ConstantTag[] constants () default {};

  /**
   * Dynamic key/value tags whose values are extracted from named or indexed method parameters
   * at each invocation.
   *
   * <p>Each {@link ParameterTag} identifies the tag key and the source parameter by name or
   * index expression. The resolved parameter value is converted to a string and attached as
   * the tag value for that invocation.</p>
   *
   * @return array of parameter-derived tags; defaults to an empty array
   */
  ParameterTag[] parameters () default {};

  /**
   * The {@link TimeUnit} in which execution duration is reported to the meter.
   *
   * @return time unit for timing measurements; defaults to {@link TimeUnit#MILLISECONDS}
   */
  TimeUnit timeUnit () default TimeUnit.MILLISECONDS;

  /**
   * Controls whether the aspect intercepts this join point.
   *
   * <p>Setting this to {@code false} disables all instrumentation overhead; the aspect
   * simply delegates to {@link org.aspectj.lang.ProceedingJoinPoint#proceed()} immediately.</p>
   *
   * @return {@code true} to enable instrumentation (default), {@code false} to bypass it
   */
  boolean active () default true;
}
