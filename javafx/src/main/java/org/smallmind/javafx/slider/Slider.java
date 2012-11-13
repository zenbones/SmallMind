package org.smallmind.javafx.slider;

public class Slider extends javafx.scene.control.Slider {

  public Slider () {

    setSkinClassName(SliderSkin.class.getName());
  }

  public Slider (double min, double max, double value) {

    super(min, max, value);

    setSkinClassName(SliderSkin.class.getName());
  }
}
