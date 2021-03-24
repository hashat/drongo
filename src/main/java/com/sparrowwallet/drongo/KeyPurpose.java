package com.sparrowwallet.drongo;

import com.sparrowwallet.drongo.crypto.ChildNumber;

public enum KeyPurpose {
    RECEIVE(new ChildNumber(20)), CHANGE(new ChildNumber(21));

    private final ChildNumber pathIndex;

    KeyPurpose(ChildNumber pathIndex) {
        this.pathIndex = pathIndex;
    }

    public ChildNumber getPathIndex() {
        return pathIndex;
    }

    public static KeyPurpose fromChildNumber(ChildNumber childNumber) {
        for(KeyPurpose keyPurpose : values()) {
            if(keyPurpose.getPathIndex().equals(childNumber)) {
                return keyPurpose;
            }
        }

        return null;
    }
}
