package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.EarningsDto;
import com.campusgigs.dto.TransactionDto;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.FormLayout;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.presentation.common.TokenFormat;
import com.campusgigs.presentation.common.TransactionHistoryPanel;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.EarningsService;
import com.campusgigs.service.TransactionService;
import com.campusgigs.service.WalletService;
import com.campusgigs.util.SessionManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Freelancer earnings screen (F26, F27): available tokens, lifetime earnings after fees,
 * tokens still waiting for approval, a simulated withdrawal form and the transaction history.
 * The rules (freelancers only, amount limits, available balance) live in the services.
 */
public class EarningsPanel extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String AMOUNT_HINT = "Enter a whole number of tokens";

    private final transient EarningsService earningsService;
    private final transient WalletService walletService;
    private final transient TransactionService transactionService;
    private final StatCard availableCard = new StatCard("Available to withdraw");
    private final StatCard earnedCard = new StatCard("Total earned (after fees)");
    private final StatCard pendingCard = new StatCard("Pending approval");
    private final StatCard withdrawnCard = new StatCard("Total withdrawn");
    private final JTextField amountField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS / 2);
    private final JButton withdrawButton = new JButton("Withdraw");
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final TransactionHistoryPanel historyPanel = new TransactionHistoryPanel();

    public EarningsPanel(EarningsService earningsService, WalletService walletService,
                         TransactionService transactionService) {
        this.earningsService = earningsService;
        this.walletService = walletService;
        this.transactionService = transactionService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_LARGE));

        JPanel cards = new JPanel(new GridLayout(1, 4, UiConstants.SPACING_LARGE, 0));
        cards.setOpaque(false);
        cards.add(availableCard);
        cards.add(earnedCard);
        cards.add(pendingCard);
        cards.add(withdrawnCard);

        JPanel top = new JPanel(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        top.setOpaque(false);
        top.add(cards, BorderLayout.NORTH);
        top.add(createWithdrawForm(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(historyPanel, BorderLayout.CENTER);

        reload();
    }

    private JPanel createWithdrawForm() {
        JPanel form = new JPanel(new BorderLayout(0, UiConstants.SPACING_SMALL));
        form.setOpaque(false);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        row.setOpaque(false);
        JLabel caption = new JLabel("Withdraw tokens (simulated, at least " + WalletService.MIN_WITHDRAWAL + "):");
        caption.setFont(UiConstants.BODY_FONT);
        caption.setLabelFor(amountField);
        row.add(caption);
        row.add(amountField);
        row.add(withdrawButton);
        form.add(row, BorderLayout.NORTH);
        form.add(messageLabel, BorderLayout.CENTER);
        withdrawButton.addActionListener(e -> submitWithdrawal(amountField.getText()));
        return form;
    }

    private void reload() {
        long userId = currentUserId();
        runInBackground(() -> snapshot(userId), this::show);
    }

    /** Validates the typed amount, then withdraws and refreshes the screen. */
    void submitWithdrawal(String text) {
        long amount;
        try {
            amount = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            messageLabel.setForeground(UiConstants.ERROR);
            messageLabel.setText(AMOUNT_HINT);
            return;
        }
        long userId = currentUserId();
        withdrawButton.setEnabled(false);
        runInBackground(() -> {
            walletService.withdraw(userId, amount);
            return snapshot(userId);
        }, snapshot -> {
            show(snapshot);
            amountField.setText("");
            withdrawButton.setEnabled(true);
            messageLabel.setForeground(UiConstants.SUCCESS);
            messageLabel.setText(TokenFormat.tokens(amount) + " tokens withdrawn");
        }, error -> {
            withdrawButton.setEnabled(true);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private Snapshot snapshot(long userId) {
        return new Snapshot(earningsService.getEarnings(userId), transactionService.getHistory(userId));
    }

    private void show(Snapshot snapshot) {
        EarningsDto earnings = snapshot.earnings();
        availableCard.setTokens(earnings.availableBalance());
        earnedCard.setTokens(earnings.totalEarned());
        pendingCard.setTokens(earnings.pendingPayout());
        withdrawnCard.setTokens(earnings.totalWithdrawn());
        historyPanel.showHistory(snapshot.history());
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** Earnings and ledger loaded together so the screen updates in one step. */
    private record Snapshot(EarningsDto earnings, List<TransactionDto> history) {
    }
}
