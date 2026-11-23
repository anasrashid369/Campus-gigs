package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** An administrator must decide; only resolving the dispute can follow. */
final class DisputedState extends GigState {

    DisputedState() {
        super(GigStatus.DISPUTED);
        allow(GigAction.RESOLVE_DISPUTE, GigStatus.RESOLVED);
    }
}
