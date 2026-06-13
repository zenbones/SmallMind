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
package org.smallmind.web.jersey.json;

import java.lang.annotation.Annotation;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers {@link XmlAdapterParamConverterProvider}: returning {@code null} when no {@link XmlJavaTypeAdapter} is
 * present, building a converter that delegates {@code fromString}/{@code toString} to the declared adapter, and
 * surfacing adapter failures as {@link XmlAdapterParamConversionException}.
 */
@Test(groups = "unit")
public class XmlAdapterParamConverterProviderTest {

  public static class TrimAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal (String value) {

      return value.trim();
    }

    @Override
    public String marshal (String value) {

      return "[" + value + "]";
    }
  }

  public static class BoomAdapter extends XmlAdapter<String, String> {

    @Override
    public String unmarshal (String value)
      throws Exception {

      throw new Exception("unmarshal failed");
    }

    @Override
    public String marshal (String value)
      throws Exception {

      throw new Exception("marshal failed");
    }
  }

  @XmlJavaTypeAdapter(TrimAdapter.class)
  private String trimmed;
  @XmlJavaTypeAdapter(BoomAdapter.class)
  private String boom;
  @Deprecated
  private String noAdapter;

  private Annotation[] annotationsOf (String fieldName)
    throws NoSuchFieldException {

    return XmlAdapterParamConverterProviderTest.class.getDeclaredField(fieldName).getAnnotations();
  }

  private ParamConverter<String> converterFor (XmlAdapterParamConverterProvider provider, String fieldName)
    throws Exception {

    return provider.getConverter(String.class, String.class, annotationsOf(fieldName));
  }

  public void testReturnsNullWithoutAdapterAnnotation ()
    throws Exception {

    Assert.assertNull(new XmlAdapterParamConverterProvider().getConverter(String.class, String.class, annotationsOf("noAdapter")));
  }

  public void testFirstCallReturnsConverter ()
    throws Exception {

    Assert.assertNotNull(new XmlAdapterParamConverterProvider().getConverter(String.class, String.class, annotationsOf("trimmed")));
  }

  public void testConverterDelegatesToAdapter ()
    throws Exception {

    ParamConverter<String> converter = converterFor(new XmlAdapterParamConverterProvider(), "trimmed");

    Assert.assertNotNull(converter);
    Assert.assertEquals(converter.fromString("  hi  "), "hi");
    Assert.assertEquals(converter.toString("hi"), "[hi]");
  }

  public void testConverterIsCachedPerAdapterClass ()
    throws Exception {

    XmlAdapterParamConverterProvider provider = new XmlAdapterParamConverterProvider();

    ParamConverter<String> first = converterFor(provider, "trimmed");
    ParamConverter<String> second = provider.getConverter(String.class, String.class, annotationsOf("trimmed"));

    Assert.assertSame(second, first);
  }

  @Test(expectedExceptions = XmlAdapterParamConversionException.class)
  public void testFromStringFailureWrapsException ()
    throws Exception {

    converterFor(new XmlAdapterParamConverterProvider(), "boom").fromString("anything");
  }

  @Test(expectedExceptions = XmlAdapterParamConversionException.class)
  public void testToStringFailureWrapsException ()
    throws Exception {

    converterFor(new XmlAdapterParamConverterProvider(), "boom").toString("anything");
  }
}
