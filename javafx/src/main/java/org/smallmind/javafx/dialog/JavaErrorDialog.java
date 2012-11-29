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
package org.smallmind.javafx.dialog;

import java.io.PrintWriter;
import java.io.StringWriter;
import com.sun.javafx.scene.control.WeakEventHandler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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
import org.smallmind.javafx.layout.InsetsPane;
import org.smallmind.javafx.layout.ParaboxPane;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Constraint;

public class JavaErrorDialog extends AbstractDialog {

  private static final Image BUG_IMAGE = new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/dialog/dialog_bug.png"));

  private ObjectProperty<EventHandler<ErrorEvent>> onErrorProperty = new SimpleObjectProperty<EventHandler<ErrorEvent>>() {

    @Override
    public void bind (ObservableValue<? extends EventHandler<ErrorEvent>> observableValue) {

      if (observableValue.getValue() != null) {
        setEventHandler(ErrorEvent.ERROR_OCCURRED, new WeakEventHandler<ErrorEvent>(JavaErrorDialog.this, ErrorEvent.ERROR_OCCURRED, observableValue.getValue()));
      }

      super.bind(observableValue);
    }

    @Override
    public void set (EventHandler<ErrorEvent> eventHandler) {

      setEventHandler(ErrorEvent.ERROR_OCCURRED, new WeakEventHandler<ErrorEvent>(JavaErrorDialog.this, ErrorEvent.ERROR_OCCURRED, eventHandler));

      super.set(eventHandler);
    }
  };

  public static JavaErrorDialog showJavaErrorDialog (Object source, Exception exception) {

    JavaErrorDialog errorDialog = new JavaErrorDialog(source, exception);

    errorDialog.centerOnScreen();
    errorDialog.show();
    errorDialog.toFront();

    return errorDialog;
  }

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
    continueButton.setOnAction(new EventHandler<ActionEvent>() {

      @Override
      public void handle (ActionEvent actionEvent) {

        JavaErrorDialog.this.close();
      }
    });

    root.setHorizontalBox(root.parallelBox(Alignment.TRAILING).add(root.sequentialBox().add(exceptionImageView).add(warningScroll, Constraint.stretch())).add(continueButton));
    root.setVerticalBox(root.sequentialBox().add(root.parallelBox().add(exceptionImageView).add(warningScroll, Constraint.stretch())).add(continueButton));

    setOnHiding(new EventHandler<WindowEvent>() {

      @Override
      public void handle (WindowEvent windowEvent) {

        fireEvent(new ErrorEvent(ErrorEvent.ERROR_OCCURRED, source, exception));
      }
    });
  }

  public EventHandler<ErrorEvent> getOnError () {

    return onErrorProperty.get();
  }

  public void setOnError (EventHandler<ErrorEvent> eventHandler) {

    onErrorProperty.set(eventHandler);
  }

  public ObjectProperty<EventHandler<ErrorEvent>> onErrorProperty () {

    return onErrorProperty;
  }
}
