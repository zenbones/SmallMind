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

import java.io.PrintWriter;
import java.io.StringWriter;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.WindowEvent;
import org.smallmind.javafx.extras.layout.InsetsPane;
import org.smallmind.javafx.extras.layout.ParaboxPane;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Constraint;

public class JavaErrorDialog extends AbstractDialog {

  private static final Image BUG_IMAGE = new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/extras/dialog/dialog_bug.png"));

  public JavaErrorDialog (final Object source, final Exception exception) {

    ParaboxPane root = new ParaboxPane();
    StringWriter errorBuffer;
    PrintWriter errorWriter;
    ScrollPane warningScroll;
    InsetsPane exceptionInsetsPane;
    Text exceptionText;
    Button continueButton;
    ImageView exceptionImageView;
    String exceptionTrace;

    setTitle("Java Error Message...");
    setScene(new Scene(root));
    setWidth(600);
    setHeight(300);

    errorBuffer = new StringWriter();
    errorWriter = new PrintWriter(errorBuffer);
    exception.printStackTrace(errorWriter);
    exceptionTrace = errorBuffer.getBuffer().toString();
    errorWriter.close();

    exceptionText = new Text(exceptionTrace);

    exceptionInsetsPane = new InsetsPane(new Insets(3, 3, 3, 3), exceptionText);

    warningScroll = new ScrollPane();
    warningScroll.setContent(exceptionInsetsPane);

    exceptionImageView = new ImageView(BUG_IMAGE);

    continueButton = new Button("Continue");
    continueButton.setDefaultButton(true);
    // TODO: Should be WeakEventHandler
    continueButton.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle (ActionEvent actionEvent) {

        JavaErrorDialog.this.close();
      }
    });

    root.setHorizontalBox(root.parallelBox(Alignment.TRAILING).add(root.serialBox().add(exceptionImageView).add(warningScroll, Constraint.stretch())).add(continueButton));
    root.setVerticalBox(root.serialBox().add(root.parallelBox().add(exceptionImageView).add(warningScroll, Constraint.stretch())).add(continueButton));

    // TODO: Should be WeakEventHandler
    setOnHiding(new EventHandler<WindowEvent>() {

      @Override
      public void handle (WindowEvent windowEvent) {

        fireEvent(new ErrorEvent(ErrorEvent.ERROR_OCCURRED, source, exception));
      }
    });
  }

  public static JavaErrorDialog showJavaErrorDialog (Object source, Exception exception) {

    JavaErrorDialog errorDialog = new JavaErrorDialog(source, exception);

    errorDialog.centerOnScreen();
    errorDialog.show();
    errorDialog.toFront();

    return errorDialog;
  }
}
