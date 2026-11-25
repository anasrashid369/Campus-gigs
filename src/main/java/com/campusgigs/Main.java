package com.campusgigs;

import com.campusgigs.dao.TransactionDao;
import com.campusgigs.dao.ApplicationDao;
import com.campusgigs.dao.CategoryDao;
import com.campusgigs.dao.DisputeDao;
import com.campusgigs.dao.FreelancerProfileDao;
import com.campusgigs.dao.GigDao;
import com.campusgigs.dao.TransactionManager;
import com.campusgigs.dao.UserDao;
import com.campusgigs.dao.SkillDao;
import com.campusgigs.dao.WalletDao;
import com.campusgigs.dao.jdbc.ApplicationDaoJdbc;
import com.campusgigs.dao.jdbc.CategoryDaoJdbc;
import com.campusgigs.dao.jdbc.ConnectionProvider;
import com.campusgigs.dao.jdbc.DisputeDaoJdbc;
import com.campusgigs.dao.jdbc.FreelancerProfileDaoJdbc;
import com.campusgigs.dao.jdbc.GigDaoJdbc;
import com.campusgigs.dao.jdbc.NotificationDaoJdbc;
import com.campusgigs.dao.jdbc.SettingsDaoJdbc;
import com.campusgigs.dao.jdbc.SkillDaoJdbc;
import com.campusgigs.dao.jdbc.TransactionDaoJdbc;
import com.campusgigs.dao.jdbc.TransactionHelper;
import com.campusgigs.dao.jdbc.UserDaoJdbc;
import com.campusgigs.dao.jdbc.WalletDaoJdbc;
import com.campusgigs.dto.GigSort;
import com.campusgigs.exception.ConfigurationException;
import com.campusgigs.model.Role;
import com.campusgigs.pattern.observer.EventBus;
import com.campusgigs.pattern.state.GigStateMachine;
import com.campusgigs.pattern.strategy.DeadlineSoonestRanking;
import com.campusgigs.pattern.strategy.GigRankingStrategy;
import com.campusgigs.pattern.strategy.HighestBudgetRanking;
import com.campusgigs.pattern.strategy.NewestFirstRanking;
import com.campusgigs.pattern.strategy.SkillMatchRanking;
import com.campusgigs.payment.SimulatedPaymentGateway;
import com.campusgigs.presentation.MainFrame;
import com.campusgigs.presentation.admin.CategoriesPanel;
import com.campusgigs.presentation.admin.AdminSummaryPanel;
import com.campusgigs.presentation.admin.DisputesPanel;
import com.campusgigs.presentation.admin.MonitoringPanel;
import com.campusgigs.presentation.admin.SettingsPanel;
import com.campusgigs.presentation.admin.SkillsPanel;
import com.campusgigs.presentation.admin.UsersPanel;
import com.campusgigs.presentation.auth.LoginPanel;
import com.campusgigs.presentation.auth.RegisterPanel;
import com.campusgigs.presentation.client.ClientGigsPanel;
import com.campusgigs.presentation.client.ClientSummaryPanel;
import com.campusgigs.presentation.client.GigFormPanel;
import com.campusgigs.presentation.common.NotificationBadge;
import com.campusgigs.presentation.common.ProfilePanel;
import com.campusgigs.presentation.common.Theme;
import com.campusgigs.presentation.common.WalletPanel;
import com.campusgigs.presentation.freelancer.BrowseGigsPanel;
import com.campusgigs.presentation.freelancer.EarningsPanel;
import com.campusgigs.presentation.freelancer.FreelancerGigsPanel;
import com.campusgigs.presentation.freelancer.FreelancerProfilePanel;
import com.campusgigs.presentation.freelancer.FreelancerSummaryPanel;
import com.campusgigs.presentation.freelancer.MyApplicationsPanel;
import com.campusgigs.service.AdminService;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.AuthService;
import com.campusgigs.service.DisputeService;
import com.campusgigs.service.EarningsService;
import com.campusgigs.service.EscrowService;
import com.campusgigs.service.GigLifecycleService;
import com.campusgigs.service.GigSearchService;
import com.campusgigs.service.GigService;
import com.campusgigs.service.MonitoringService;
import com.campusgigs.service.NotificationService;
import com.campusgigs.service.ProfileService;
import com.campusgigs.service.SettingsService;
import com.campusgigs.service.TransactionService;
import com.campusgigs.service.UserService;
import com.campusgigs.service.WalletService;
import com.campusgigs.service.impl.AdminServiceImpl;
import com.campusgigs.service.impl.ApplicationServiceImpl;
import com.campusgigs.service.impl.AuthServiceImpl;
import com.campusgigs.service.impl.DisputeServiceImpl;
import com.campusgigs.service.impl.EarningsServiceImpl;
import com.campusgigs.service.impl.EscrowServiceImpl;
import com.campusgigs.service.impl.GigLifecycleServiceImpl;
import com.campusgigs.service.impl.GigSearchServiceImpl;
import com.campusgigs.service.impl.GigServiceImpl;
import com.campusgigs.service.impl.MonitoringServiceImpl;
import com.campusgigs.service.impl.NotificationServiceImpl;
import com.campusgigs.service.impl.ProfileServiceImpl;
import com.campusgigs.service.impl.SettingsServiceImpl;
import com.campusgigs.service.impl.TransactionServiceImpl;
import com.campusgigs.service.impl.UserServiceImpl;
import com.campusgigs.service.impl.WalletServiceImpl;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;

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
        Theme.apply();
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
        SkillDao skillDao = new SkillDaoJdbc(db);
        CategoryDao categoryDao = new CategoryDaoJdbc(db);

        SettingsService settingsService = new SettingsServiceImpl(new SettingsDaoJdbc(db), userDao);
        WalletDao walletDao = new WalletDaoJdbc(db);
        TransactionDao transactionDao = new TransactionDaoJdbc(db);
        WalletService walletService = new WalletServiceImpl(walletDao, transactionDao,
                new SimulatedPaymentGateway(), transactions, EventBus.getInstance(), userDao);
        TransactionService transactionService = new TransactionServiceImpl(walletDao, transactionDao);
        GigDao gigDao = new GigDaoJdbc(db);
        DisputeDao disputeDao = new DisputeDaoJdbc(db);
        NotificationServiceImpl notificationService = new NotificationServiceImpl(new NotificationDaoJdbc(db),
                userDao, gigDao);
        notificationService.subscribeTo(EventBus.getInstance());
        EscrowService escrowService = new EscrowServiceImpl(walletDao, transactionDao, transactions);
        GigStateMachine gigStateMachine = new GigStateMachine();
        GigLifecycleService lifecycleService = new GigLifecycleServiceImpl(gigDao, disputeDao, userDao,
                escrowService, settingsService, gigStateMachine, transactions, EventBus.getInstance());
        DisputeService disputeService = new DisputeServiceImpl(disputeDao, gigDao, userDao, escrowService,
                settingsService, gigStateMachine, transactions, EventBus.getInstance());
        EarningsService earningsService = new EarningsServiceImpl(userDao, walletDao, transactionDao, gigDao);
        GigService gigService = new GigServiceImpl(gigDao, categoryDao, skillDao, userDao, gigStateMachine,
                transactions, Clock.systemDefaultZone());
        ApplicationDao applicationDao = new ApplicationDaoJdbc(db);
        ApplicationService applicationService = new ApplicationServiceImpl(applicationDao, gigDao, userDao,
                settingsService, lifecycleService, transactions, EventBus.getInstance());
        FreelancerProfileDao profileDao = new FreelancerProfileDaoJdbc(db);
        GigSearchService gigSearchService = new GigSearchServiceImpl(gigDao, userDao, profileDao,
                rankingStrategies());
        MonitoringService monitoringService = new MonitoringServiceImpl(gigDao, applicationDao, transactionDao,
                userDao);
        AuthService authService = new AuthServiceImpl(userDao, walletService, settingsService, transactions);
        UserService userService = new UserServiceImpl(userDao);
        AdminService adminService = new AdminServiceImpl(userDao, categoryDao, skillDao);
        ProfileService profileService = new ProfileServiceImpl(userDao, profileDao, skillDao, transactions);

        MainFrame frame = new MainFrame();
        frame.addCard(MainFrame.LOGIN_CARD, new LoginPanel(authService, frame));
        frame.addCard(MainFrame.REGISTER_CARD, new RegisterPanel(authService, frame));
        registerScreens(frame, new Services(userService, adminService, profileService, settingsService,
                walletService, transactionService, earningsService, disputeService, lifecycleService,
                gigService, applicationService, gigSearchService, monitoringService, notificationService));
        frame.showCard(MainFrame.LOGIN_CARD);
        frame.setVisible(true);
    }

    /** The services the screens need; add a field when a new screen needs another service. */
    private record Services(UserService users, AdminService admin, ProfileService profiles,
                            SettingsService settings, WalletService wallets, TransactionService transactions,
                            EarningsService earnings, DisputeService disputes,
                            GigLifecycleService lifecycle, GigService gigs,
                            ApplicationService applications, GigSearchService search,
                            MonitoringService monitoring, NotificationService notifications) {
    }

    /** One ranking per sort option; add a class here to offer a new ordering. */
    private static Map<GigSort, GigRankingStrategy> rankingStrategies() {
        Map<GigSort, GigRankingStrategy> strategies = new EnumMap<>(GigSort.class);
        strategies.put(GigSort.NEWEST, new NewestFirstRanking());
        strategies.put(GigSort.BUDGET_HIGH, new HighestBudgetRanking());
        strategies.put(GigSort.DEADLINE_SOON, new DeadlineSoonestRanking());
        strategies.put(GigSort.SKILL_MATCH, new SkillMatchRanking());
        return strategies;
    }

    /** Navigation buttons per role, in menu order. Add new screens here. */
    private static void registerScreens(MainFrame frame, Services services) {
        for (Role role : Role.values()) {
            frame.addNavigationItem(role, "My Profile", () -> new ProfilePanel(services.users()));
            frame.addHeaderWidget(role, () -> new NotificationBadge(services.notifications()));
        }
        frame.addNavigationItem(Role.CLIENT, "Wallet",
                () -> new WalletPanel(services.wallets(), services.transactions()));
        frame.addNavigationItem(Role.CLIENT, "Post a Gig",
                () -> new GigFormPanel(services.gigs(), services.profiles()));
        frame.addNavigationItem(Role.CLIENT, "My Gigs",
                () -> new ClientGigsPanel(services.gigs(), services.profiles(), services.applications(),
                        services.lifecycle()));
        frame.addNavigationItem(Role.FREELANCER, "Find Gigs",
                () -> new BrowseGigsPanel(services.search(), services.gigs(), services.applications()));
        frame.addNavigationItem(Role.FREELANCER, "My Applications",
                () -> new MyApplicationsPanel(services.applications()));
        frame.addNavigationItem(Role.FREELANCER, "My Gigs",
                () -> new FreelancerGigsPanel(services.gigs(), services.lifecycle()));
        frame.addNavigationItem(Role.FREELANCER, "Earnings",
                () -> new EarningsPanel(services.earnings(), services.wallets(), services.transactions()));
        frame.addNavigationItem(Role.FREELANCER, "Skills and Portfolio",
                () -> new FreelancerProfilePanel(services.profiles()));
        frame.addNavigationItem(Role.ADMIN, "Users", () -> new UsersPanel(services.admin()));
        frame.addNavigationItem(Role.ADMIN, "Categories", () -> new CategoriesPanel(services.admin()));
        frame.addNavigationItem(Role.ADMIN, "Skills", () -> new SkillsPanel(services.admin()));
        frame.addNavigationItem(Role.ADMIN, "Disputes", () -> new DisputesPanel(services.disputes()));
        frame.addNavigationItem(Role.ADMIN, "Monitoring", () -> new MonitoringPanel(services.monitoring()));
        frame.addNavigationItem(Role.ADMIN, "Settings", () -> new SettingsPanel(services.settings()));

        frame.addHomeWidget(Role.CLIENT, () -> new ClientSummaryPanel(services.gigs()));
        frame.addHomeWidget(Role.FREELANCER, () -> new FreelancerSummaryPanel(services.applications(),
                services.gigs(), services.earnings()));
        frame.addHomeWidget(Role.ADMIN, () -> new AdminSummaryPanel(services.disputes()));
    }
}
