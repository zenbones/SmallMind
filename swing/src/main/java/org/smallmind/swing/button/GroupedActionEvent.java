package org.smallmind.swing.button;

import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;

public class GroupedActionEvent extends ActionEvent {

  private ButtonGroup buttonGroup;

  public GroupedActionEvent (ButtonGroup buttonGroup, ActionEvent actionEvent) {

    super(actionEvent.getSource(), actionEvent.getID(), actionEvent.getActionCommand(), actionEvent.getWhen(), actionEvent.getModifiers());

    this.buttonGroup = buttonGroup;
  }

  public ButtonGroup getButtonGroup () {

    return buttonGroup;
  }
}
