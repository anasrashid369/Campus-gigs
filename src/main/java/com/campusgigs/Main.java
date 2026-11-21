package com.campusgigs;

import com.campusgigs.presentation.auth.LoginFrame;

import javax.swing.SwingUtilities;

/** Application entry point: opens the login window on the Event Dispatch Thread. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
