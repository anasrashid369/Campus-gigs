package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** Work delivered; the client approves, asks for a revision, or disputes. */
final class SubmittedState extends GigState {

    SubmittedState() {
        super(GigStatus.SUBMITTED);
        allow(GigAction.APPROVE, GigStatus.APPROVED);
        allow(GigAction.REQUEST_REVISION, GigStatus.ASSIGNED);
        allow(GigAction.RAISE_DISPUTE, GigStatus.DISPUTED);
    }
}
