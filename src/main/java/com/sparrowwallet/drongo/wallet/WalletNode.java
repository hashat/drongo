package com.sparrowwallet.drongo.wallet;

import com.sparrowwallet.drongo.KeyDerivation;
import com.sparrowwallet.drongo.KeyPurpose;
import com.sparrowwallet.drongo.crypto.ChildNumber;

import java.util.*;
import java.util.stream.Collectors;

public class WalletNode implements Comparable<WalletNode> {
    private final String derivationPath;
    private String label;
    private TreeSet<WalletNode> children = new TreeSet<>();
    private TreeSet<BlockTransactionHashIndex> transactionOutputs = new TreeSet<>();

    private transient KeyPurpose keyPurpose;
    private transient int index = -1;
    private transient List<ChildNumber> derivation;

    public WalletNode(String derivationPath) {
        this.derivationPath = derivationPath;
        parseDerivation();
    }

    public WalletNode(KeyPurpose keyPurpose) {
        this.derivation = List.of(keyPurpose.getPathIndex());
        this.derivationPath = KeyDerivation.writePath(derivation);
        this.keyPurpose = keyPurpose;
        this.index = keyPurpose.getPathIndex().num();
    }

    public WalletNode(KeyPurpose keyPurpose, int index) {
        this.derivation = List.of(keyPurpose.getPathIndex(), new ChildNumber(index));
        this.derivationPath = KeyDerivation.writePath(derivation);
        this.keyPurpose = keyPurpose;
        this.index = index;
    }

    public String getDerivationPath() {
        return derivationPath;
    }

    public void parseDerivation() {
        this.derivation = KeyDerivation.parsePath(derivationPath);
        this.keyPurpose = new KeyPurpose(derivation.get(0).i()); // Hashat> old: KeyPurpose.fromChildNumber(derivation.get(0));
        this.index = derivation.get(derivation.size() - 1).num();
    }

    public int getIndex() {
        if(index < 0) {
            parseDerivation();
        }

        return index;
    }

    public KeyPurpose getKeyPurpose() {
        if(keyPurpose == null) {
            parseDerivation();
        }

        return keyPurpose;
    }

    public List<ChildNumber> getDerivation() {
        if(derivation == null) {
            parseDerivation();
        }

        return derivation;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getValue() {
        if(transactionOutputs == null) {
            return null;
        }

        return getUnspentTransactionOutputs().stream().mapToLong(BlockTransactionHashIndex::getValue).sum();
    }

    public Set<WalletNode> getChildren() {
        return children == null ? null : Collections.unmodifiableSet(children);
    }

    public void setChildren(TreeSet<WalletNode> children) {
        this.children = children;
    }

    public Set<BlockTransactionHashIndex> getTransactionOutputs() {
        return transactionOutputs == null ? null : Collections.unmodifiableSet(transactionOutputs);
    }

    public void setTransactionOutputs(TreeSet<BlockTransactionHashIndex> transactionOutputs) {
        this.transactionOutputs = transactionOutputs;
    }

    public synchronized void updateTransactionOutputs(Set<BlockTransactionHashIndex> updatedOutputs) {
        for(BlockTransactionHashIndex txo : updatedOutputs) {
            Optional<String> optionalLabel = transactionOutputs.stream().filter(oldTxo -> oldTxo.getHash().equals(txo.getHash()) && oldTxo.getIndex() == txo.getIndex()).map(BlockTransactionHash::getLabel).filter(Objects::nonNull).findFirst();
            optionalLabel.ifPresent(txo::setLabel);
        }

        transactionOutputs.clear();
        transactionOutputs.addAll(updatedOutputs);
    }

    public Set<BlockTransactionHashIndex> getUnspentTransactionOutputs() {
        return getUnspentTransactionOutputs(false);
    }

    public Set<BlockTransactionHashIndex> getUnspentTransactionOutputs(boolean includeMempoolInputs) {
        Set<BlockTransactionHashIndex> unspentTXOs = new TreeSet<>(transactionOutputs);
        return unspentTXOs.stream().filter(txo -> !txo.isSpent() || (includeMempoolInputs && txo.getSpentBy().getHeight() <= 0)).collect(Collectors.toCollection(HashSet::new));
    }

    public long getUnspentValue() {
        long value = 0L;
        for(BlockTransactionHashIndex utxo : getUnspentTransactionOutputs()) {
            value += utxo.getValue();
        }

        return value;
    }

    public synchronized void fillToIndex(int index) {
        for(int i = 0; i <= index; i++) {
            WalletNode node = new WalletNode(getKeyPurpose(), i);
            children.add(node);
        }
    }

    /**
     * @return The highest used index, or null if no addresses are used
     */
    public Integer getHighestUsedIndex() {
        WalletNode highestNode = null;
        for(WalletNode childNode : getChildren()) {
            if(!childNode.getTransactionOutputs().isEmpty()) {
                highestNode = childNode;
            }
        }

        return highestNode == null ? null : highestNode.index;
    }

    @Override
    public String toString() {
        return derivationPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WalletNode node = (WalletNode) o;
        return derivationPath.equals(node.derivationPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(derivationPath);
    }

    @Override
    public int compareTo(WalletNode node) {
        if(getDerivation().size() != node.getDerivation().size()) {
            return getDerivation().size() - node.getDerivation().size();
        }

        for(int i = 0; i < getDerivation().size(); i++) {
            ChildNumber thisChild = getDerivation().get(i);
            ChildNumber nodeChild = node.getDerivation().get(i);
            if(thisChild.num() != nodeChild.num()) {
                return thisChild.num() - nodeChild.num();
            }
        }

        return 0;
    }

    public synchronized void clearHistory() {
        transactionOutputs.clear();
        for(WalletNode childNode : getChildren()) {
            childNode.clearHistory();
        }
    }

    public WalletNode copy() {
        WalletNode copy = new WalletNode(derivationPath);
        copy.setLabel(label);

        for(WalletNode child : getChildren()) {
            copy.children.add(child.copy());
        }

        for(BlockTransactionHashIndex txo : getTransactionOutputs()) {
            copy.transactionOutputs.add(txo.copy());
        }

        return copy;
    }

    public boolean copyLabels(WalletNode pastNode) {
        if(pastNode == null) {
            return false;
        }

        boolean changed = false;

        if(label == null && pastNode.label != null) {
            label = pastNode.label;
            changed = true;
        }

        for(BlockTransactionHashIndex txo : getTransactionOutputs()) {
            Optional<BlockTransactionHashIndex> optPastTxo = pastNode.getTransactionOutputs().stream().filter(pastTxo -> pastTxo.equals(txo)).findFirst();
            if(optPastTxo.isPresent()) {
                BlockTransactionHashIndex pastTxo = optPastTxo.get();
                if(txo.getLabel() == null && pastTxo.getLabel() != null) {
                    txo.setLabel(pastTxo.getLabel());
                    changed = true;
                }
                if(txo.isSpent() && pastTxo.isSpent() && txo.getSpentBy().getLabel() == null && pastTxo.getSpentBy().getLabel() != null) {
                    txo.getSpentBy().setLabel(pastTxo.getSpentBy().getLabel());
                    changed = true;
                }
            }
        }

        for(WalletNode childNode : getChildren()) {
            Optional<WalletNode> optPastChildNode = pastNode.getChildren().stream().filter(node -> node.equals(childNode)).findFirst();
            if(optPastChildNode.isPresent()) {
                changed |= childNode.copyLabels(optPastChildNode.get());
            }
        }

        return changed;
    }
}
