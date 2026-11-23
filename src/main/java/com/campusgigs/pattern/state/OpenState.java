package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** Posted and waiting for a freelancer: it can be filled or cancelled. */
final class OpenState extends GigState {

    OpenState() {
        super(GigStatus.OPEN);
        allow(GigAction.HIRE, GigStatus.ASSIGNED);
        allow(GigAction.CANCEL, GigStatus.CANCELLED);
    }
}
