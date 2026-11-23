package com.campusgigs.pattern.state;

import com.campusgigs.model.GigStatus;

/** Work approved and the freelancer paid. Final: nothing can happen any more. */
final class ApprovedState extends GigState {

    ApprovedState() {
        super(GigStatus.APPROVED);
    }
}
