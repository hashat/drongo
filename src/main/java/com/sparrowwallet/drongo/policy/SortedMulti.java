package com.sparrowwallet.drongo.policy;

public enum SortedMulti {
    SORTED("Enabled (default)"), UNSORTED("Disabled (compatibility)");

    private String text;

    SortedMulti(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}