/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.javafx.extras.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.WindowEvent;
import org.smallmind.javafx.extras.ImageNotFoundException;
import org.smallmind.javafx.extras.layout.ParaboxPane;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;
import org.smallmind.nutsnbolts.layout.Justification;
import org.smallmind.nutsnbolts.layout.ParallelBox;
import org.smallmind.nutsnbolts.layout.SerialBox;

public class OptionDialog extends AbstractDialog {

  private static final OptionButton[] NO_BUTTONS = new OptionButton[0];

  private final OptionPane optionPane;
  private final ParaboxPane buttonPane;

  private DialogState dialogState;

  public OptionDialog (String optionText, OptionType optionType) {

    this(optionText, optionType, null, NO_BUTTONS);
  }

  public OptionDialog (String optionText, OptionType optionType, OptionButton... optionButtons) {

    this(optionText, optionType, null, optionButtons);
  }

  public OptionDialog (String optionText, OptionType optionType, final OptionPane optionPane, OptionButton... optionButtons) {

    ParaboxPane root = new ParaboxPane();
    ParallelBox optionHorizontalBox;
    SerialBox optionVerticalBox;
    Separator separator = new Separator();
    Label optionLabel;
    ImageView optionImage;

    this.optionPane = optionPane;

    setTitle(optionType.getTitle() + "...");
    setScene(new Scene(root));

    try {
      optionImage = new ImageView(new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/extras/dialog/dialog_" + optionType.getImageType() + ".png")));
    } catch (Exception exception) {
      throw new ImageNotFoundException(exception);
    }

    optionLabel = new Label(optionText);
    optionLabel.setStyle("-fx-font-weight: bold");

    dialogState = DialogState.INCOMPLETE;

    if (optionPane != null) {
      optionPane.initialize(this);
    }

    buttonPane = new ParaboxPane(new Insets(0, 0, 0, 0));
    buttonPane.setHorizontalBox(buttonPane.serialBox(Justification.TRAILING));
    buttonPane.setVerticalBox(buttonPane.parallelBox());
    replaceButtons(optionButtons);

    root.setHorizontalBox(root.parallelBox()
                            .add(root.serialBox().add(optionImage).add((optionHorizontalBox = root.parallelBox()).add(optionLabel)))
                            .add(separator, Constraint.stretch())
                            .add(buttonPane, Constraint.stretch()));

    root.setVerticalBox(root.serialBox()
                          .add(root.parallelBox().add(optionImage).add((optionVerticalBox = root.serialBox()).add(optionLabel)))
                          .add(root.serialBox(Gap.RELATED).add(separator).add(buttonPane)));

    if (this.optionPane != null) {
      optionHorizontalBox.add(optionPane);
      optionVerticalBox.add(optionPane);
    }

    // TODO: Should be WeakEventHandler
    onHidingProperty().set(new EventHandler<WindowEvent>() {

      @Override
      public void handle (WindowEvent windowEvent) {

        String validationMessage;

        if ((optionPane != null) && ((validationMessage = optionPane.validateOption(dialogState)) != null)) {
          WarningDialog.showWarningDialog(validationMessage);
        } else {
          fireEvent(new DialogEvent(DialogEvent.COMPLETED, OptionDialog.this, dialogState));
          hide();
        }
      }
    });
  }

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
    } else {
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
    // TODO: Should be WeakEventHandler
    button.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle (javafx.event.ActionEvent actionEvent) {

        OptionDialog.this.dialogState = dialogState;
        hide();
      }
    });

    buttonPane.getHorizontalBox().add(button);
    buttonPane.getVerticalBox().add(button, Constraint.stretch());
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
}
