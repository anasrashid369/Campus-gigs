package com.campusgigs;

import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.jdbc.ConnectionProvider;
import com.campusgigs.dao.jdbc.SettingsDaoJdbc;
import com.campusgigs.dao.jdbc.TransactionDaoJdbc;
import com.campusgigs.dao.jdbc.TransactionHelper;
import com.campusgigs.dao.jdbc.UserDaoJdbc;
import com.campusgigs.dao.jdbc.WalletDaoJdbc;
import com.campusgigs.exception.ConfigurationException;
import com.campusgigs.model.Role;
import com.campusgigs.payment.SimulatedPaymentGateway;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.auth.LoginPanel;
import com.campusgigs.presentation.auth.RegisterPanel;
import com.campusgigs.presentation.common.ProfilePanel;
import com.campusgigs.service.AuthService;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.UserService;
import com.campusgigs.service.WalletService;
import com.campusgigs.service.impl.AuthServiceImpl;
import com.campusgigs.service.impl.SettingsServiceImpl;
import com.campusgigs.service.impl.UserServiceImpl;
import com.campusgigs.service.impl.WalletServiceImpl;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Application entry point and the one place where concrete classes are wired
 * together: DAOs into services, services into screens. Team members register their
 * screens in {@link #registerScreens} with {@code frame.addNavigationItem(...)}.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        ConnectionProvider db;
        try {
            db = ConnectionProvider.getInstance();
        } catch (ConfigurationException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Campus Gigs cannot start",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        TransactionManager transactions = new TransactionHelper(db);
        UserDao userDao = new UserDaoJdbc(db);

        SettingsService settingsService = new SettingsServiceImpl(new SettingsDaoJdbc(db), userDao);
        WalletService walletService = new WalletServiceImpl(new WalletDaoJdbc(db), new TransactionDaoJdbc(db),
                new SimulatedPaymentGateway(), transactions);
        AuthService authService = new AuthServiceImpl(userDao, walletService, settingsService, transactions);
        UserService userService = new UserServiceImpl(userDao);

        MainFrame frame = new MainFrame();
        frame.addCard(MainFrame.LOGIN_CARD, new LoginPanel(authService, frame));
        frame.addCard(MainFrame.REGISTER_CARD, new RegisterPanel(authService, frame));
        registerScreens(frame, userService);
        frame.showCard(MainFrame.LOGIN_CARD);
        frame.setVisible(true);
    }

    /** Navigation buttons per role. Add new screens here. */
    private static void registerScreens(MainFrame frame, UserService userService) {
        for (Role role : Role.values()) {
            frame.addNavigationItem(role, "My Profile", () -> new ProfilePanel(userService));
        }
    }
}
