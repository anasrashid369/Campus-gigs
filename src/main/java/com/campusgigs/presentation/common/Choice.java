package com.campusgigs.presentation.common;

/**
 * An entry of a filter combo box: a label and the value it stands for. A null value means "all".
 *
 * @param <T> the type of the value
 */
public record Choice<T>(String label, T value) {

    @Override
    public String toString() {
        return label;
    }
}
