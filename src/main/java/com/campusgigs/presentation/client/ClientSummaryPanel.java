package com.campusgigs.presentation.client;

import com.campusgigs.dto.GigDto;
import com.campusgigs.model.GigStatus;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.presentation.common.UiConstants;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;

import java.awt.GridLayout;
import java.util.List;

/** The client's home summary: open gigs, gigs in progress and work waiting for their review. */
public class ClientSummaryPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private final StatCard openCard = new StatCard("Open gigs");
    private final StatCard progressCard = new StatCard("In progress");
    private final StatCard reviewCard = new StatCard("Waiting for your review");

    public ClientSummaryPanel(GigService gigService) {
        setLayout(new GridLayout(1, 3, UiConstants.SPACING_LARGE, 0));
        add(openCard);
        add(progressCard);
        add(reviewCard);
        long clientId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> gigService.listClientGigs(clientId), this::show);
    }

    /** How many of the gigs are in the given status. */
    public static long count(List<GigDto> gigs, GigStatus status) {
        return gigs.stream().filter(gig -> gig.status() == status).count();
    }

    private void show(List<GigDto> gigs) {
        openCard.setTokens(count(gigs, GigStatus.OPEN));
        progressCard.setTokens(count(gigs, GigStatus.ASSIGNED));
        reviewCard.setTokens(count(gigs, GigStatus.SUBMITTED));
    }
}
