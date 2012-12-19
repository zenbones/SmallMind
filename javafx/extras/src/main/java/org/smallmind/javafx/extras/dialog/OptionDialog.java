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
package org.smallmind.javafx.extras.dialog;

import com.sun.javafx.scene.control.WeakEventHandler;
import javafx.beans.property.ObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;
import org.smallmind.javafx.extras.EventHandlerProperty;
import org.smallmind.javafx.extras.ImageNotFoundException;
import org.smallmind.javafx.extras.layout.ParaboxPane;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.ParallelBox;
import org.smallmind.nutsnbolts.layout.SequentialBox;

public class OptionDialog extends AbstractDialog {

  private static final OptionButton[] NO_BUTTONS = new OptionButton[0];

  private final OptionPane optionPane;
  private final ParaboxPane buttonPane;
  private final EventHandlerProperty<DialogEvent> onDialogCompletedProperty = new EventHandlerProperty<DialogEvent>() {

    @Override
    public void replaceEventHandler (EventHandler<DialogEvent> eventHandler) {

      setEventHandler(DialogEvent.DIALOG_COMPLETED, new WeakEventHandler<DialogEvent>(OptionDialog.this, DialogEvent.DIALOG_COMPLETED, eventHandler));
    }
  };

  private DialogState dialogState;

  public static OptionDialog showOptionDialog (String optionText, OptionType optionType) {

    return showOptionDialog(optionText, optionType, null, NO_BUTTONS);
  }

  public static OptionDialog showOptionDialog (String optionText, OptionType optionType, OptionButton... optionButtons) {

    return showOptionDialog(optionText, optionType, null, optionButtons);
  }

  public static OptionDialog showOptionDialog (String optionText, OptionType optionType, OptionPane optionPane, OptionButton... optionButtons) {

    OptionDialog optionDialog = new OptionDialog(optionText, optionType, optionPane, optionButtons);

    optionDialog.resizeAndRelocateAndShow();

    return optionDialog;
  }

  public OptionDialog (String optionText, OptionType optionType) {

    this(optionText, optionType, null, NO_BUTTONS);
  }

  public OptionDialog (String optionText, OptionType optionType, OptionButton... optionButtons) {

    this(optionText, optionType, null, optionButtons);
  }

  public OptionDialog (String optionText, OptionType optionType, final OptionPane optionPane, OptionButton... optionButtons) {

    ParaboxPane root = new ParaboxPane();
    ParallelBox optionHorizontalBox;
    SequentialBox optionVerticalBox;
    Separator separator = new Separator();
    Label optionLabel;
    ImageView optionImage;

    this.optionPane = optionPane;

    setTitle(optionType.getTitle() + "...");
    setScene(new Scene(root));

    try {
      optionImage = new ImageView(new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/extras/dialog/dialog_" + optionType.getImageType() + ".png")));
    }
    catch (Exception exception) {
      throw new ImageNotFoundException(exception);
    }

    optionLabel = new Label(optionText);
    optionLabel.setStyle("-fx-font-weight: bold");

    dialogState = DialogState.INCOMPLETE;

    if (optionPane != null) {
      optionPane.initialize(this);
    }

    buttonPane = new ParaboxPane(new Insets(0, 0, 0, 0));
    buttonPane.setHorizontalBox(buttonPane.sequentialBox(Justification.TRAILING));
    buttonPane.setVerticalBox(buttonPane.parallelBox());
    replaceButtons(optionButtons);

    root.setHorizontalBox(root.parallelBox()
      .add(root.sequentialBox().add(optionImage).add((optionHorizontalBox = root.parallelBox()).add(optionLabel)))
      .add(separator, Constraint.stretch())
      .add(buttonPane, Constraint.stretch()));

    root.setVerticalBox(root.sequentialBox()
      .add(root.parallelBox().add(optionImage).add((optionVerticalBox = root.sequentialBox()).add(optionLabel)))
      .add(root.sequentialBox(Gap.RELATED).add(separator).add(buttonPane)));

    if (this.optionPane != null) {
      optionHorizontalBox.add(optionPane);
      optionVerticalBox.add(optionPane);
    }

    onHidingProperty().set(new EventHandler<WindowEvent>() {

      @Override
      public void handle (WindowEvent windowEvent) {

        String validationMessage;

        if ((optionPane != null) && ((validationMessage = optionPane.validateOption(dialogState)) != null)) {
          WarningDialog.showWarningDialog(validationMessage);
        }
        else {
          fireEvent(new DialogEvent(DialogEvent.DIALOG_COMPLETED, OptionDialog.this, dialogState));
          hide();
        }
      }
    });
  }

  public void resizeAndRelocateAndShow () {

    setResizable(false);
    sizeToScene();
    centerOnScreen();
    show();
    toFront();
  }

  public synchronized void replaceButtons (OptionButton[] optionButtons) {

    buttonPane.removeAll();

    if ((optionButtons == null) || (optionButtons.length == 0)) {
      placeButton("Continue", DialogState.CONTINUE, true);
    }
    else {
      for (OptionButton optionButton : optionButtons) {
        placeButton(optionButton.getName(), optionButton.getButtonState(), false);
      }
    }

    buttonPane.requestLayout();
  }

  private void placeButton (String buttonName, final DialogState dialogState, boolean defaultAction) {

    Button button;

    button = new Button(buttonName);
    button.setDefaultButton(defaultAction);
    button.setOnAction(new EventHandler<javafx.event.ActionEvent>() {

      @Override
      public void handle (javafx.event.ActionEvent actionEvent) {

        OptionDialog.this.dialogState = dialogState;
        hide();
      }
    });

    buttonPane.getHorizontalBox().add(button);
    buttonPane.getVerticalBox().add(button, Constraint.stretch());
  }

  public EventHandler<DialogEvent> getOnDialogCompleted () {

    return onDialogCompletedProperty.get();
  }

  public void setOnDialogCompleted (EventHandler<DialogEvent> eventHandler) {

    onDialogCompletedProperty.set(eventHandler);
  }

  public ObjectProperty<EventHandler<DialogEvent>> onDialogCompletedProperty () {

    return onDialogCompletedProperty;
  }

  public synchronized DialogState getDialogState () {

    return dialogState;
  }

  public synchronized void setDialogState (DialogState dialogState) {

    this.dialogState = dialogState;
  }

  public OptionPane getOptionPane () {

    return optionPane;
  }

  /*
  public synchronized void windowClosing (WindowEvent windowEvent) {

    String validationMessage;
    WarningDialog warningDialog;

    if ((optionPane != null) && ((validationMessage = optionPane.validateOption(dialogState)) != null)) {
      warningDialog = new WarningDialog(this, validationMessage);
      warningDialog.setModal(true);
      warningDialog.setVisible(true);
    }
    else {
      fireDialogEvent();
      setVisible(false);
      dispose();
    }
  }
  */
}
