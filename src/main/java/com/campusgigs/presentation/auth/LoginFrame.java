package com.campusgigs.presentation.auth;

import com.campusgigs.presentation.common.UiConstants;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;

/**
 * First window the user sees. For now it only shows the title; the login and
 * register forms are added with the authentication screens.
 */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public LoginFrame() {
        super(UiConstants.APP_TITLE + " - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(UiConstants.LOGIN_WIDTH, UiConstants.LOGIN_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(UiConstants.BACKGROUND);
        setLayout(new BorderLayout());

        JLabel title = new JLabel(UiConstants.APP_TITLE, SwingConstants.CENTER);
        title.setFont(UiConstants.TITLE_FONT);
        title.setForeground(UiConstants.PRIMARY);
        add(title, BorderLayout.CENTER);
    }
}
