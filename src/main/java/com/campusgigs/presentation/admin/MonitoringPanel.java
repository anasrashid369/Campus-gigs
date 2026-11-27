package com.campusgigs.presentation.admin;

import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.service.MonitoringService;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Admin monitoring (F9): tabs for all gigs, all applications and the token ledger. */
public class MonitoringPanel extends BasePanel {

    public MonitoringPanel(MonitoringService monitoringService) {
        TabPane tabs = new TabPane(
                tab("Gigs", new GigsMonitorTab(monitoringService)),
                tab("Applications", new ApplicationsMonitorTab(monitoringService)),
                tab("Ledger", new LedgerMonitorTab(monitoringService)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        setCenter(tabs);
    }

    private static Tab tab(String title, Node content) {
        return new Tab(title, content);
    }
}
