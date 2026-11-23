package com.campusgigs.presentation.common;

import com.campusgigs.dto.TransactionDto;
import com.campusgigs.dto.WalletDto;
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
 * Client wallet screen (F5, F6, F20): the available and escrowed balances, a top-up form
 * and the transaction history. All rules (amount range, client-only top-up, the simulated
 * payment) live in {@link WalletService}; this panel only collects the amount, calls the
 * service and shows the result.
 */
public class WalletPanel extends BasePanel {

    private static final long serialVersionUID = 1L;
    private static final String AMOUNT_HINT = "Enter a whole number of tokens";

    private final transient WalletService walletService;
    private final transient TransactionService transactionService;
    private final StatCard availableCard = new StatCard("Available tokens");
    private final StatCard escrowCard = new StatCard("Held in escrow");
    private final JTextField amountField = new JTextField(UiConstants.TEXT_FIELD_COLUMNS / 2);
    private final JButton topUpButton = new JButton("Top up");
    private final JLabel messageLabel = FormLayout.messageLabel();
    private final TransactionHistoryPanel historyPanel = new TransactionHistoryPanel();

    public WalletPanel(WalletService walletService, TransactionService transactionService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        setLayout(new BorderLayout(0, UiConstants.SPACING_LARGE));

        JPanel balances = new JPanel(new GridLayout(1, 2, UiConstants.SPACING_LARGE, 0));
        balances.setOpaque(false);
        balances.add(availableCard);
        balances.add(escrowCard);

        JPanel top = new JPanel(new BorderLayout(0, UiConstants.SPACING_MEDIUM));
        top.setOpaque(false);
        top.add(balances, BorderLayout.NORTH);
        top.add(createTopUpForm(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(historyPanel, BorderLayout.CENTER);

        reload();
    }

    private JPanel createTopUpForm() {
        JPanel form = new JPanel(new BorderLayout(0, UiConstants.SPACING_SMALL));
        form.setOpaque(false);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiConstants.SPACING_MEDIUM, 0));
        row.setOpaque(false);
        JLabel caption = new JLabel("Top up (" + WalletService.MIN_TOP_UP + " - "
                + TokenFormat.tokens(WalletService.MAX_TOP_UP) + " tokens):");
        caption.setFont(UiConstants.BODY_FONT);
        caption.setLabelFor(amountField);
        row.add(caption);
        row.add(amountField);
        row.add(topUpButton);
        form.add(row, BorderLayout.NORTH);
        form.add(messageLabel, BorderLayout.CENTER);
        topUpButton.addActionListener(e -> submitTopUp(amountField.getText()));
        return form;
    }

    /** Loads the wallet and its history in the background. */
    private void reload() {
        long userId = currentUserId();
        runInBackground(() -> snapshot(userId), this::show);
    }

    /** Validates the typed amount, then tops up and refreshes the screen. */
    void submitTopUp(String text) {
        long amount;
        try {
            amount = Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            messageLabel.setForeground(UiConstants.ERROR);
            messageLabel.setText(AMOUNT_HINT);
            return;
        }
        long userId = currentUserId();
        topUpButton.setEnabled(false);
        runInBackground(() -> {
            walletService.topUp(userId, amount);
            return snapshot(userId);
        }, snapshot -> {
            show(snapshot);
            amountField.setText("");
            topUpButton.setEnabled(true);
            messageLabel.setForeground(UiConstants.SUCCESS);
            messageLabel.setText(TokenFormat.tokens(amount) + " tokens added");
        }, error -> {
            topUpButton.setEnabled(true);
            FormLayout.showFailure(this, messageLabel, error);
        });
    }

    private Snapshot snapshot(long userId) {
        return new Snapshot(walletService.getWallet(userId), transactionService.getHistory(userId));
    }

    private void show(Snapshot snapshot) {
        availableCard.setTokens(snapshot.wallet().availableBalance());
        escrowCard.setTokens(snapshot.wallet().escrowBalance());
        historyPanel.showHistory(snapshot.history());
    }

    private static long currentUserId() {
        return SessionManager.getInstance().getCurrentUser().userId();
    }

    /** Wallet balances and ledger loaded together so the screen updates in one step. */
    private record Snapshot(WalletDto wallet, List<TransactionDto> history) {
    }
}
