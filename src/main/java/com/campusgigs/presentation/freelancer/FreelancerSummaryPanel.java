package com.campusgigs.presentation.freelancer;

import com.campusgigs.dto.ApplicationDto;
import com.campusgigs.dto.EarningsDto;
import com.campusgigs.dto.GigDto;
import com.campusgigs.model.ApplicationStatus;
import com.campusgigs.model.GigStatus;
import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.presentation.common.StatCard;
import com.campusgigs.service.ApplicationService;
import com.campusgigs.service.EarningsService;
import com.campusgigs.service.GigService;
import com.campusgigs.util.SessionManager;
import java.util.List;

/** The freelancer's home summary: pending applications, gigs in progress and tokens available. */
public class FreelancerSummaryPanel extends BasePanel {

    /** Everything the summary needs, loaded in one background call. */
    private record Loaded(List<ApplicationDto> applications, List<GigDto> gigs, EarningsDto earnings) {
    }

    private final StatCard pendingCard = new StatCard("Pending applications");
    private final StatCard progressCard = new StatCard("Gigs in progress");
    private final StatCard tokensCard = new StatCard("Available tokens");

    public FreelancerSummaryPanel(ApplicationService applicationService, GigService gigService,
                                  EarningsService earningsService) {
        useAsWidget();
        setCenter(StatCard.row(pendingCard, progressCard, tokensCard));
        long freelancerId = SessionManager.getInstance().getCurrentUser().userId();
        runInBackground(() -> new Loaded(applicationService.listForFreelancer(freelancerId),
                gigService.listFreelancerGigs(freelancerId), earningsService.getEarnings(freelancerId)), this::show);
    }

    /** How many applications are still waiting for an answer. */
    public static long pending(List<ApplicationDto> applications) {
        return applications.stream().filter(a -> a.status() == ApplicationStatus.PENDING).count();
    }

    /** How many gigs the freelancer is working on or has handed in for review. */
    public static long inProgress(List<GigDto> gigs) {
        return gigs.stream().filter(g -> g.status() == GigStatus.ASSIGNED || g.status() == GigStatus.SUBMITTED).count();
    }

    private void show(Loaded loaded) {
        pendingCard.setTokens(pending(loaded.applications()));
        progressCard.setTokens(inProgress(loaded.gigs()));
        tokensCard.setTokens(loaded.earnings().availableBalance());
    }
}
