package com.campusgigs.presentation.admin;

import com.campusgigs.model.DisputeStatus;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.DisputeService;
import com.campusgigs.util.SessionManager;

import java.awt.GridLayout;

/** The administrator's home summary: disputes waiting for a decision. */
public class AdminSummaryPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final StatCard disputesCard = new StatCard("Open disputes");

    public AdminSummaryPanel(DisputeService disputeService) {
        setLayout(new GridLayout(1, 1, UiConstants.SPACING_LARGE, 0));
        add(disputesCard);
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> (long) disputeService.listDisputes(adminId, DisputeStatus.OPEN).size(),
                disputesCard::setTokens);
    }
}
