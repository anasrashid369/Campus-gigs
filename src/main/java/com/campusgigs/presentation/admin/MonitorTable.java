package com.campusgigs.presentation.admin;

import com.campusgigs.presentation.common.TablePanel;
import java.util.List;
import java.util.function.Function;

/** A read-only table with a Refresh button, used by the monitoring tabs. */
final class MonitorTable<T> extends TablePanel<T> {

    MonitorTable(String title, List<String> columns, Function<T, Object[]> toCells, Runnable refresh) {
        super(title, columns, toCells);
        addAction("Refresh", false, refresh);
    }

    void show(List<T> rows) {
        setRows(rows);
    }
}
