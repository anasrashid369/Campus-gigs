package com.campusgigs.presentation.common;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Parent class of every screen. Gives all panels the same background and
 * padding, and a helper that runs slow work (service and database calls) on a
 * background thread so the window never freezes.
 * <p>
 * Typical use inside an action listener:
 * <pre>{@code
 * runInBackground(() -> walletService.getWallet(userId), this::showWallet);
 * }</pre>
 */
public abstract class BasePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    protected BasePanel() {
        setBackground(UiConstants.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(UiConstants.SPACING_LARGE, UiConstants.SPACING_LARGE,
                UiConstants.SPACING_LARGE, UiConstants.SPACING_LARGE));
    }

    /**
     * Runs {@code task} in a SwingWorker, then calls {@code onSuccess} or
     * {@code onFailure} on the Event Dispatch Thread.
     *
     * @param task      the slow work, for example a service call; runs off the EDT
     * @param onSuccess receives the task's result on the EDT
     * @param onFailure receives the exception the task threw, on the EDT
     */
    protected <T> void runInBackground(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onFailure.accept(unwrap(e));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onFailure.accept(e);
                }
            }
        }.execute();
    }

    /**
     * Same as {@link #runInBackground(Callable, Consumer, Consumer)}, showing
     * any failure with {@link ErrorDialog}.
     */
    protected <T> void runInBackground(Callable<T> task, Consumer<T> onSuccess) {
        runInBackground(task, onSuccess, error -> ErrorDialog.show(this, error));
    }

    private static Exception unwrap(ExecutionException e) {
        Throwable cause = e.getCause();
        return cause instanceof Exception exception ? exception : e;
    }
}
