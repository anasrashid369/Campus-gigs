package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** A freelancer is hired and working; the only way forward is to deliver the work. */
final class AssignedState extends GigState {

    AssignedState() {
        super(GigStatus.ASSIGNED);
        allow(GigAction.SUBMIT, GigStatus.SUBMITTED);
    }
}
