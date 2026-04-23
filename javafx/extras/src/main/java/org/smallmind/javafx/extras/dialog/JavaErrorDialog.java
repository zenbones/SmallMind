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

/**
 * A modal dialog that displays the full stack trace of a {@link Throwable} inside a scrollable
 * text area. When the dialog is closed an {@link ErrorEvent} of type {@link ErrorEvent#OCCURRED}
 * is fired on the dialog pane, carrying the original source and throwable so that registered
 * handlers may react to the dismissal.
 */
public class JavaErrorDialog extends Dialog<ButtonType> {

  private static final Image BUG_IMAGE = new Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("org/smallmind/javafx/extras/dialog/dialog_bug.png"));

  /**
   * Constructs the dialog and populates it with the stack trace from {@code throwable}. An
   * {@link ErrorEvent#OCCURRED} event is scheduled to fire on the dialog pane when the dialog hides.
   *
   * @param source    the logical object associated with the error; may be {@code null}; carried
   *                  verbatim in the resulting {@link ErrorEvent}
   * @param throwable the exception whose stack trace will be displayed; must not be {@code null}
   */
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

  /**
   * Convenience factory that creates a {@link JavaErrorDialog} and immediately shows it, blocking
   * until the user dismisses it.
   *
   * @param source    the logical object associated with the error; may be {@code null}
   * @param throwable the exception to display; must not be {@code null}
   * @return an {@link Optional} containing the {@link ButtonType} the user clicked, or
   * {@link Optional#empty()} if the dialog was closed without pressing a button
   */
  public static Optional<ButtonType> showJavaErrorDialog (Object source, Throwable throwable) {

    JavaErrorDialog errorDialog = new JavaErrorDialog(source, throwable);

    return errorDialog.showAndWait();
  }
}
