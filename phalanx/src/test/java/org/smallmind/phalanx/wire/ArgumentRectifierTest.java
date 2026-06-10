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
package org.smallmind.phalanx.wire;

import java.lang.reflect.Method;
import java.util.HashMap;
import org.smallmind.phalanx.wire.signal.Function;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.JavaSerializationSignalCodec;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers both directions of {@link ArgumentRectifier}: packing a call's argument array into the
 * name-keyed wire map ({@code induceMap}, including the {@link java.io.Serializable} guard) and
 * rebuilding the ordered argument array from an inbound signal ({@code constructArray}, including
 * codec extraction, null handling, and the name-mismatch failure).
 */
@Test(groups = "unit")
public class ArgumentRectifierTest {

  public interface Sample {

    String echo (@Argument("text") String text);

    Integer combine (@Argument("text") String text, @Argument("count") int count);

    void take (@Argument("text") String text);
  }

  private Method method (String name, Class<?>... parameterTypes)
    throws NoSuchMethodException {

    return Sample.class.getMethod(name, parameterTypes);
  }

  @Test
  public void testInduceMapReturnsNullForNoArguments ()
    throws TransportException {

    Assert.assertNull(ArgumentRectifier.induceMap(new String[] {"a"}, null));
    Assert.assertNull(ArgumentRectifier.induceMap(new String[] {"a"}, new Object[0]));
  }

  @Test
  public void testInduceMapPacksValuesByName ()
    throws TransportException {

    HashMap<String, Object> argumentMap = ArgumentRectifier.induceMap(new String[] {"a", "b"}, new Object[] {1, "x"});

    Assert.assertEquals(argumentMap.size(), 2);
    Assert.assertEquals(argumentMap.get("a"), 1);
    Assert.assertEquals(argumentMap.get("b"), "x");
  }

  @Test
  public void testInduceMapPreservesNullElements ()
    throws TransportException {

    HashMap<String, Object> argumentMap = ArgumentRectifier.induceMap(new String[] {"a"}, new Object[] {null});

    Assert.assertTrue(argumentMap.containsKey("a"));
    Assert.assertNull(argumentMap.get("a"));
  }

  @Test(expectedExceptions = TransportException.class)
  public void testInduceMapRejectsNonSerializableArgument ()
    throws TransportException {

    ArgumentRectifier.induceMap(new String[] {"a"}, new Object[] {new Object()});
  }

  @Test
  public void testConstructArrayRoundTripsThroughCodec ()
    throws Exception {

    SignalCodec codec = new JavaSerializationSignalCodec();
    Method targetMethod = method("echo", String.class);
    Function function = new Function(targetMethod);
    Methodology methodology = new Methodology(Sample.class, targetMethod);

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("text", "hi");

    byte[] encoded = codec.encode(new InvocationSignal(false, new Route(1, "Sample", function), arguments));
    InvocationSignal decoded = codec.decode(encoded, 0, encoded.length, InvocationSignal.class);

    Object[] reconstructed = ArgumentRectifier.constructArray(codec, decoded, function, methodology);

    Assert.assertEquals(reconstructed.length, 1);
    Assert.assertEquals(reconstructed[0], "hi");
  }

  @Test
  public void testConstructArrayConvertsValuesToParameterTypes ()
    throws Exception {

    SignalCodec codec = new JsonSignalCodec();
    Method targetMethod = method("combine", String.class, int.class);
    Function function = new Function(targetMethod);
    Methodology methodology = new Methodology(Sample.class, targetMethod);

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("text", "hi");
    arguments.put("count", 3);

    Object[] reconstructed = ArgumentRectifier.constructArray(codec, new InvocationSignal(false, new Route(1, "Sample", function), arguments), function, methodology);

    Assert.assertEquals(reconstructed.length, 2);
    Assert.assertEquals(reconstructed[0], "hi");
    Assert.assertEquals(reconstructed[1], 3);
  }

  @Test
  public void testConstructArrayLeavesNullArgumentsUnset ()
    throws Exception {

    SignalCodec codec = new JsonSignalCodec();
    Method targetMethod = method("take", String.class);
    Function function = new Function(targetMethod);
    Methodology methodology = new Methodology(Sample.class, targetMethod);

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("text", null);

    Object[] reconstructed = ArgumentRectifier.constructArray(codec, new InvocationSignal(false, new Route(1, "Sample", function), arguments), function, methodology);

    Assert.assertEquals(reconstructed.length, 1);
    Assert.assertNull(reconstructed[0]);
  }

  @Test(expectedExceptions = MismatchedArgumentException.class)
  public void testConstructArrayRejectsUnknownArgumentName ()
    throws Exception {

    SignalCodec codec = new JsonSignalCodec();
    Method targetMethod = method("echo", String.class);
    Function function = new Function(targetMethod);
    Methodology methodology = new Methodology(Sample.class, targetMethod);

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("bogus", "x");

    ArgumentRectifier.constructArray(codec, new InvocationSignal(false, new Route(1, "Sample", function), arguments), function, methodology);
  }

  @Test
  public void testInduceMapMarshalsWireAnnotatedArgument ()
    throws TransportException {

    HashMap<String, Object> argumentMap = ArgumentRectifier.induceMap(new String[] {"w"}, new Object[] {new Widget("hello")});

    Assert.assertEquals(argumentMap.get("w"), "hello");
  }

  @Test(expectedExceptions = TransportException.class)
  public void testInduceMapRejectsNonSerializableAdapterValueType ()
    throws TransportException {

    ArgumentRectifier.induceMap(new String[] {"w"}, new Object[] {new BadWidget()});
  }

  @Test
  public void testConstructArrayUnmarshalsWireAnnotatedArgument ()
    throws Exception {

    SignalCodec codec = new JsonSignalCodec();
    Method targetMethod = AdapterSample.class.getMethod("consume", Widget.class);
    Function function = new Function(targetMethod);
    Methodology methodology = new Methodology(AdapterSample.class, targetMethod);

    HashMap<String, Object> arguments = new HashMap<>();
    arguments.put("w", "hello");

    Object[] reconstructed = ArgumentRectifier.constructArray(codec, new InvocationSignal(false, new Route(1, "AdapterSample", function), arguments), function, methodology);

    Assert.assertTrue(reconstructed[0] instanceof Widget);
    Assert.assertEquals(((Widget)reconstructed[0]).getLabel(), "hello");
  }

  public interface AdapterSample {

    void consume (@Argument("w") Widget widget);
  }

  @Wire(adapter = WidgetAdapter.class)
  public static class Widget {

    private final String label;

    public Widget (String label) {

      this.label = label;
    }

    public String getLabel () {

      return label;
    }
  }

  public static class WidgetAdapter extends WireAdapter<String, Widget> {

    @Override
    public Class<String> getValueType () {

      return String.class;
    }

    @Override
    public Widget unmarshal (String obj) {

      return new Widget(obj);
    }

    @Override
    public String marshal (Widget obj) {

      return obj.getLabel();
    }
  }

  @Wire(adapter = BadAdapter.class)
  public static class BadWidget {

  }

  public static class BadAdapter extends WireAdapter<Object, BadWidget> {

    @Override
    public Class<Object> getValueType () {

      return Object.class;
    }

    @Override
    public BadWidget unmarshal (Object obj) {

      return new BadWidget();
    }

    @Override
    public Object marshal (BadWidget obj) {

      return new Object();
    }
  }
}
