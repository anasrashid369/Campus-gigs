package com.campusgigs.presentation.admin;

import com.campusgigs.presentation.common.BasePanel;
import com.campusgigs.service.MonitoringService;

import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** Admin monitoring (F9): tabs for all gigs, all applications and the token ledger. */
public class MonitoringPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    public MonitoringPanel(MonitoringService monitoringService) {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gigs", new GigsMonitorTab(monitoringService));
        tabs.addTab("Applications", new ApplicationsMonitorTab(monitoringService));
        tabs.addTab("Ledger", new LedgerMonitorTab(monitoringService));
        add(tabs, BorderLayout.CENTER);
    }
}
