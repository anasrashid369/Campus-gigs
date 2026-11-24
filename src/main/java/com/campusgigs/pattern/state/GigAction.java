package com.campusgigs.pattern.state;

/** Something that can happen to a gig and may change its status. */
public enum GigAction {

    /** The client picks one freelancer and funds the gig. */
    HIRE("hire a freelancer for"),
    /** The freelancer delivers the work. */
    SUBMIT("submit work on"),
    /** The client accepts the work and the freelancer is paid. */
    APPROVE("approve"),
    /** The client asks for changes and the freelancer works again. */
    REQUEST_REVISION("request a revision of"),
    /** The client or freelancer escalates to an administrator. */
    RAISE_DISPUTE("raise a dispute on"),
    /** An administrator decides a dispute. */
    RESOLVE_DISPUTE("resolve a dispute on"),
    /** The client withdraws a gig nobody has been hired for. */
    CANCEL("cancel");

    private final String description;

    GigAction(String description) {
        this.description = description;
    }

    /** Text used in error messages: "You cannot {description} a gig that is ...". */
    String description() {
        return description;
    }
}
