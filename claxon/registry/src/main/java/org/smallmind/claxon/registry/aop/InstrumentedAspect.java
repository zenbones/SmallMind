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
package org.smallmind.claxon.registry.aop;

import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.smallmind.claxon.registry.Identifier;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.WithResultExecutable;
import org.smallmind.claxon.registry.meter.MeterBuilder;
import org.smallmind.nutsnbolts.reflection.aop.AOPUtility;

@Aspect
public class InstrumentedAspect {

  // Should not grow large as the json representing builders should have very low cardinality
  private static final ConcurrentHashMap<ParsedKey, MeterBuilder<?>> PARSED_MAP = new ConcurrentHashMap<>();

  @Around(value = "(execution(@Instrumented * * (..)) || initialization(@Instrumented new(..))) && @annotation(instrumented)", argNames = "thisJoinPoint, instrumented")
  public Object aroundInstrumentedMethod (ProceedingJoinPoint thisJoinPoint, Instrumented instrumented)
    throws Throwable {

    MeterBuilder<?> builder;
    Tag[] tags = new Tag[instrumented.constants().length + instrumented.parameters().length];
    ParsedKey parsedKey = new ParsedKey(instrumented.parser(), instrumented.json());
    int index = 0;

    for (ConstantTag constantTag : instrumented.constants()) {
      tags[index++] = new Tag(constantTag.key(), constantTag.constant());
    }
    for (ParameterTag parameterTag : instrumented.parameters()) {
      tags[index++] = new Tag(parameterTag.key(), AOPUtility.getParameterValue(thisJoinPoint, parameterTag.parameter(), false).toString());
    }

    if ((builder = PARSED_MAP.get(parsedKey)) == null) {
      synchronized (PARSED_MAP) {
        if ((builder = PARSED_MAP.get(parsedKey)) == null) {
          PARSED_MAP.put(parsedKey, builder = instrumented.parser().getConstructor().newInstance().parse(instrumented.json()));
        }
      }
    }

    return Instrument.with(Identifier.instance(instrumented.identifier()), builder, tags)
             .as(instrumented.timeUnit())
             .on((WithResultExecutable<Object>)thisJoinPoint::proceed);
  }

  private static class ParsedKey {

    private Class<?> parser;
    private String json;

    public ParsedKey (Class<?> parser, String json) {

      this.parser = parser;
      this.json = json;
    }

    public Class<?> getParser () {

      return parser;
    }

    public String getJson () {

      return json;
    }

    @Override
    public int hashCode () {

      return (parser.hashCode() * 31) + json.hashCode();
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof ParsedKey) && ((ParsedKey)obj).getParser().equals(parser) && ((ParsedKey)obj).getJson().equals(json);
    }
  }
}
