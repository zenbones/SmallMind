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
package org.smallmind.javafx.extras.dialog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import org.smallmind.javafx.extras.layout.InsetsPane;
import org.smallmind.javafx.extras.layout.ParaboxPane;
import org.smallmind.nutsnbolts.layout.Alignment;
import org.smallmind.nutsnbolts.layout.Constraint;
import org.smallmind.nutsnbolts.layout.Gap;

public class JavaErrorDialog extends Dialog<ButtonType> {

  private static final Image BUG_IMAGE = new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/extras/dialog/dialog_bug.png"));

  public JavaErrorDialog (final Object source, final Throwable throwable) {

    ParaboxPane root = new ParaboxPane();
    StringWriter errorBuffer;
    PrintWriter errorWriter;
    ScrollPane warningScroll;
    String exceptionTrace;

    setTitle("Java Error Message...");
    setGraphic(new ImageView(BUG_IMAGE));
    setResizable(true);

    errorBuffer = new StringWriter();
    errorWriter = new PrintWriter(errorBuffer);
    throwable.printStackTrace(errorWriter);
    exceptionTrace = errorBuffer.getBuffer().toString();
    errorWriter.close();

    warningScroll = new ScrollPane();
    warningScroll.setContent(new InsetsPane(new Insets(3, 3, 3, 3), new Text(exceptionTrace)));

    root.setHorizontalBox(root.parallelBox(Alignment.TRAILING).add(warningScroll, Constraint.stretch()));
    root.setVerticalBox(root.serialBox(Gap.NONE).add(root.parallelBox().add(warningScroll, Constraint.stretch())));

    getDialogPane().setContent(root);
    getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

    setWidth(600);
    setHeight(400);

    onHidingProperty().set(event -> JavaErrorDialog.this.getDialogPane().fireEvent(new ErrorEvent(ErrorEvent.OCCURRED, source, throwable)));
  }

  public static Optional<ButtonType> showJavaErrorDialog (Object source, Throwable throwable) {

    JavaErrorDialog errorDialog = new JavaErrorDialog(source, throwable);

    return errorDialog.showAndWait();
  }
}
