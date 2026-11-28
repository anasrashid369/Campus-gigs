package com.campusgigs.presentation.common;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Parent class of every screen. Gives all screens the same background and padding, and a
 * helper that runs slow work (service and database calls) on a background thread so the
 * window never freezes.
 * <p>
 * Typical use inside a button handler:
 * <pre>{@code
 * runInBackground(() -> walletService.getWallet(userId), this::showWallet);
 * }</pre>
 */
public abstract class BasePanel extends BorderPane {

    /** Daemon threads, so an unfinished call never keeps the app running after the window closes. */
    private static final ExecutorService BACKGROUND = Executors.newCachedThreadPool(work -> {
        Thread thread = new Thread(work, "campus-gigs-background");
        thread.setDaemon(true);
        return thread;
    });

    protected BasePanel() {
        getStyleClass().add("screen");
    }

    /**
     * Runs {@code task} on a background thread, then calls {@code onSuccess} or
     * {@code onFailure} on the JavaFX Application Thread.
     *
     * @param task      the slow work, for example a service call; runs off the UI thread
     * @param onSuccess receives the task's result on the UI thread
     * @param onFailure receives the exception the task threw, on the UI thread
     */
    protected <T> void runInBackground(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onFailure, "onFailure");
        BACKGROUND.execute(() -> {
            T result;
            try {
                result = task.call();
            } catch (Exception e) {
                Platform.runLater(() -> onFailure.accept(e));
                return;
            }
            Platform.runLater(() -> onSuccess.accept(result));
        });
    }

    /**
     * Same as {@link #runInBackground(Callable, Consumer, Consumer)}, showing
     * any failure with {@link ErrorDialog}.
     */
    protected <T> void runInBackground(Callable<T> task, Consumer<T> onSuccess) {
        runInBackground(task, onSuccess, error -> ErrorDialog.show(this, error));
    }

    /** Use for small parts of a screen (summary cards, the notification badge): no screen padding. */
    protected final void useAsWidget() {
        getStyleClass().setAll("widget");
    }

    /** Shows a form card centred on the screen, scrolling when the window is too small for it. */
    protected final void setCenteredContent(Region card) {
        card.setMaxWidth(Region.USE_PREF_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane holder = new StackPane(card);
        holder.getStyleClass().add("form-holder");
        ScrollPane scroll = new ScrollPane(holder);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("form-scroll");
        setCenter(scroll);
    }
}
