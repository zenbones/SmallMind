/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.extras.slider;

import java.util.ArrayList;
import java.util.List;
import com.sun.javafx.Utils;
import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.KeyBinding;
import com.sun.javafx.scene.control.behavior.OrientedKeyBinding;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.END;
import static javafx.scene.input.KeyCode.F4;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.KP_DOWN;
import static javafx.scene.input.KeyCode.KP_LEFT;
import static javafx.scene.input.KeyCode.KP_RIGHT;
import static javafx.scene.input.KeyCode.KP_UP;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.TAB;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;

public class SliderBehavior extends BehaviorBase<Slider> {

  /**
   * ***********************************************************************
   * Setup KeyBindings                             *
   * *
   * We manually specify the focus traversal keys because Slider has        *
   * different usage for up/down arrow keys.                                *
   * ***********************************************************************
   */
  protected static final List<KeyBinding> SLIDER_BINDINGS = new ArrayList<KeyBinding>();

  static {
    SLIDER_BINDINGS.add(new KeyBinding(TAB, "TraverseNext"));
    SLIDER_BINDINGS.add(new KeyBinding(TAB, "TraversePrevious").shift());
    // TODO XXX DEBUGGING ONLY
    SLIDER_BINDINGS.add(new KeyBinding(F4, "TraverseDebug").alt().ctrl().shift());

    SLIDER_BINDINGS.add(new SliderKeyBinding(LEFT, "DecrementValue"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_LEFT, "DecrementValue"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(UP, "IncrementValue").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_UP, "IncrementValue").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(RIGHT, "IncrementValue"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_RIGHT, "IncrementValue"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(DOWN, "DecrementValue").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_DOWN, "DecrementValue").vertical());

    SLIDER_BINDINGS.add(new SliderKeyBinding(LEFT, "TraverseLeft").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_LEFT, "TraverseLeft").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(UP, "TraverseUp"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_UP, "TraverseUp"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(RIGHT, "TraverseRight").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_RIGHT, "TraverseRight").vertical());
    SLIDER_BINDINGS.add(new SliderKeyBinding(DOWN, "TraverseDown"));
    SLIDER_BINDINGS.add(new SliderKeyBinding(KP_DOWN, "TraverseDown"));

    SLIDER_BINDINGS.add(new KeyBinding(HOME, KEY_RELEASED, "Home"));
    SLIDER_BINDINGS.add(new KeyBinding(END, KEY_RELEASED, "End"));
  }

  @Override
  protected void callAction (String name) {

    if ("Home".equals(name)) home();
    else if ("End".equals(name)) end();
    else if ("IncrementValue".equals(name)) incrementValue();
    else if ("DecrementValue".equals(name)) decrementValue();
    else super.callAction(name);
  }

  public SliderBehavior (Slider slider) {

    super(slider);
  }

  @Override
  protected List<KeyBinding> createKeyBindings () {

    return SLIDER_BINDINGS;
  }

  /**************************************************************************
   *                         State and Functions                            *
   *************************************************************************/

  /**
   * Invoked by the Slider {@link Skin} implementation whenever a mouse press
   * occurs on the "track" of the slider. This will cause the thumb to be
   * moved by some amount.
   *
   * @param position The mouse position on track with 0.0 being beginning of
   *                 track and 1.0 being the end
   */
  public void trackPress (MouseEvent e, double position) {
    // determine the percentage of the way between min and max
    // represented by this mouse event
    final Slider slider = getControl();
    // If not already focused, request focus
    if (!slider.isFocused()) slider.requestFocus();
    if (slider.getOrientation().equals(Orientation.HORIZONTAL)) {
      slider.adjustValue(position * (slider.getMax() - slider.getMin()) + slider.getMin());
    }
    else {
      slider.adjustValue((1 - position) * (slider.getMax() - slider.getMin()) + slider.getMin());
    }
  }

  /**
   */
  public void trackRelease (MouseEvent e, double position) {

  }

  /**
   * @param position The mouse position on track with 0.0 being beginning of
   *                 track and 1.0 being the end
   */
  public void thumbPressed (MouseEvent e, double position) {
    // If not already focused, request focus
    final Slider slider = getControl();
    if (!slider.isFocused()) slider.requestFocus();
    slider.setValueChanging(true);
  }

  /**
   * @param position The mouse position on track with 0.0 being beginning of
   *                 track and 1.0 being the end
   */
  public void thumbDragged (MouseEvent e, double position) {

    final Slider slider = getControl();
    slider.setValue(Utils.clamp(slider.getMin(), (position * (slider.getMax() - slider.getMin())) + slider.getMin(), slider.getMax()));
  }

  private double snapValueToTicks (double val) {

    final Slider slider = getControl();
    double v = val;
    double tickSpacing = 0;
    // compute the nearest tick to this value
    if (slider.getMinorTickCount() != 0) {
      tickSpacing = slider.getMajorTickUnit() / (Math.max(slider.getMinorTickCount(), 0) + 1);
    }
    else {
      tickSpacing = slider.getMajorTickUnit();
    }
    int prevTick = (int)((v - slider.getMin()) / tickSpacing);
    double prevTickValue = (prevTick) * tickSpacing + slider.getMin();
    double nextTickValue = (prevTick + 1) * tickSpacing + slider.getMin();
    v = Utils.nearest(prevTickValue, v, nextTickValue);
    return Utils.clamp(slider.getMin(), v, slider.getMax());
  }

  /**
   * When thumb is released valueChanging should be set to false.
   */
  public void thumbReleased (MouseEvent e) {

    final Slider slider = getControl();
    slider.setValueChanging(false);
    // RT-15207 When snapToTicks is true, slider value calculated in drag
    // is then snapped to the nearest tick on mouse release.
    if (slider.isSnapToTicks()) {
      slider.setValue(snapValueToTicks(slider.getValue()));
    }
  }

  void home () {

    final Slider slider = getControl();
    slider.adjustValue(slider.getMin());
  }

  void decrementValue () {

    final Slider slider = getControl();
    // RT-8634 If snapToTicks is true and block increment is less than
    // tick spacing, tick spacing is used as the decrement value.
    if (slider.isSnapToTicks()) {
      slider.adjustValue(slider.getValue() - computeIncrement());
    }
    else {
      slider.decrement();
    }

  }

  void end () {

    final Slider slider = getControl();
    slider.adjustValue(slider.getMax());
  }

  void incrementValue () {

    final Slider slider = getControl();
    // RT-8634 If snapToTicks is true and block increment is less than
    // tick spacing, tick spacing is used as the increment value.
    if (slider.isSnapToTicks()) {
      slider.adjustValue(slider.getValue() + computeIncrement());
    }
    else {
      slider.increment();
    }
  }

  // Used only if snapToTicks is true.
  double computeIncrement () {

    final Slider slider = getControl();
    double tickSpacing = 0;
    if (slider.getMinorTickCount() != 0) {
      tickSpacing = slider.getMajorTickUnit() / (Math.max(slider.getMinorTickCount(), 0) + 1);
    }
    else {
      tickSpacing = slider.getMajorTickUnit();
    }

    if (slider.getBlockIncrement() > 0 && slider.getBlockIncrement() < tickSpacing) {
      return tickSpacing;
    }

    return slider.getBlockIncrement();
  }

  public static class SliderKeyBinding extends OrientedKeyBinding {

    public SliderKeyBinding (KeyCode code, String action) {

      super(code, action);
    }

    public SliderKeyBinding (KeyCode code, EventType<KeyEvent> type, String action) {

      super(code, type, action);
    }

    public
    @Override
    boolean getVertical (Control control) {

      return ((Slider)control).getOrientation() == Orientation.VERTICAL;
    }
  }
}
