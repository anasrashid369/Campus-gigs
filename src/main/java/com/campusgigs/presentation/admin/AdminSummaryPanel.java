package com.campusgigs.presentation.admin;

import com.campusgigs.model.DisputeStatus;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.service.DisputeService;
import com.campusgigs.util.SessionManager;

/** The administrator's home summary: disputes waiting for a decision. */
public class AdminSummaryPanel extends BasePanel {

    private final StatCard disputesCard = new StatCard("Open disputes");

    public AdminSummaryPanel(DisputeService disputeService) {
        useAsWidget();
        disputesCard.setMaxWidth(320);
        setLeft(disputesCard);
        long adminId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> (long) disputeService.listDisputes(adminId, DisputeStatus.OPEN).size(),
                disputesCard::setTokens);
    }
}
