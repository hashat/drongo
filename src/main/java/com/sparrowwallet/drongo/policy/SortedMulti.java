package com.sparrowwallet.drongo.policy;

public enum SortedMulti {
    SORTED("Enabled (default)"), UNSORTED("Disabled (compatibility)");

    private String textLabel;

    SortedMulti(String textLabel) {
        this.textLabel = textLabel;
    }

    @Override
    public String toString() {
        return textLabel;
    }
}