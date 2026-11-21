package com.campusgigs.presentation.common;

import com.campusgigs.exception.DataAccessException;
import com.campusgigs.exception.ResourceNotFoundException;
import com.campusgigs.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorDialogTest {

    @Test
    void showsValidationMessageAsItIs() {
        assertEquals("Email is already registered",
                ErrorDialog.messageFor(new ValidationException("Email is already registered")));
    }

    @Test
    void showsNotFoundMessageAsItIs() {
        assertEquals("Gig not found", ErrorDialog.messageFor(new ResourceNotFoundException("Gig not found")));
    }

    @Test
    void hidesTechnicalErrors() {
        assertEquals(ErrorDialog.GENERIC_MESSAGE,
                ErrorDialog.messageFor(new DataAccessException("Communications link failure on jdbc:mysql://...")));
        assertEquals(ErrorDialog.GENERIC_MESSAGE, ErrorDialog.messageFor(new NullPointerException("x is null")));
    }

    @Test
    void usesGenericTextWhenUserMessageIsMissing() {
        assertEquals(ErrorDialog.GENERIC_MESSAGE, ErrorDialog.messageFor(new ValidationException(null)));
        assertEquals(ErrorDialog.GENERIC_MESSAGE, ErrorDialog.messageFor(new ValidationException(" ")));
    }
}
