package com.campusgigs.presentation.common;

import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;

/**
 * Shows errors to the user without exposing technical details.
 * <p>
 * {@link ValidationException} and {@link ResourceNotFoundException} carry messages
 * written for the user, so those are shown as they are. Anything else is logged
 * and replaced by a generic message.
 */
public final class ErrorDialog {

    static final String GENERIC_MESSAGE = "Something went wrong. Please try again.";

    private static final Logger LOG = Logger.getLogger(ErrorDialog.class.getName());

    private ErrorDialog() {
    }

    /**
     * Shows an error message dialog. Safe to call from any thread.
     *
     * @param owner the node whose window the dialog belongs to, may be {@code null}
     * @param error the failure to report
     */
    public static void show(Node owner, Throwable error) {
        String message = messageFor(error);
        if (!isUserFacing(error)) {
            LOG.log(Level.WARNING, "Unexpected error shown to the user as a generic message", error);
        }
        Runnable dialog = () -> Dialogs.error(owner, message);
        if (Platform.isFxApplicationThread()) {
            dialog.run();
        } else {
            Platform.runLater(dialog);
        }
    }

    /** The text the user sees for an error. */
    static String messageFor(Throwable error) {
        return isUserFacing(error) ? error.getMessage() : GENERIC_MESSAGE;
    }

    private static boolean isUserFacing(Throwable error) {
        return (error instanceof ValidationException || error instanceof ResourceNotFoundException)
                && error.getMessage() != null && !error.getMessage().isBlank();
    }
}
