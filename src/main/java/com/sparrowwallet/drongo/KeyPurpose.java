package com.sparrowwallet.drongo;

import com.sparrowwallet.drongo.crypto.ChildNumber;

public class KeyPurpose {

    private final ChildNumber pathIndex;

    public KeyPurpose(int index) {
        this.pathIndex = new ChildNumber(index);
    }

    public ChildNumber getPathIndex() {
        return pathIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyPurpose that = (KeyPurpose) o;
        return this.pathIndex.i() == that.pathIndex.i();
    }
}
