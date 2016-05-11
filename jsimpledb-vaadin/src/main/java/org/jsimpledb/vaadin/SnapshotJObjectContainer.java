
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.vaadin;

import java.util.Iterator;

import org.jsimpledb.JObject;
import org.jsimpledb.JSimpleDB;
import org.jsimpledb.JTransaction;
import org.jsimpledb.SnapshotJTransaction;
import org.jsimpledb.ValidationMode;
import org.jsimpledb.core.Transaction;
import org.jsimpledb.kv.CloseableKVStore;

/**
 * Specialized {@link JObjectContainer} for use with key/value stores that implement
 * {@link org.jsimpledb.kv.KVTransaction#mutableSnapshot KVTransaction.mutableSnapshot()}.
 *
 * <p>
 * This container is loaded by taking a key/value store snapshot and then iterating the desired backing objects
 * directly via {@link #iterateObjects}; no copying of objects is required.
 *
 * <p>
 * Instances are (re)loaded at any time by invoking {@link #reload}. During reload, the container opens a {@link JTransaction}
 * and then creates a {@link SnapshotJTransaction} using the {@link CloseableKVStore} returned by
 * {@link org.jsimpledb.kv.KVTransaction#mutableSnapshot KVTransaction.mutableSnapshot()}.
 * This {@link SnapshotJTransaction} is set as the current transaction while {@link #iterateObjects}
 * returns the {@link JObject}s to be actually included in the container. The {@link CloseableKVStore} will
 * remain open until the container is {@link #reload}'ed or {@link #disconnect}'ed.
 */
@SuppressWarnings("serial")
public abstract class SnapshotJObjectContainer extends ReloadableJObjectContainer {

    private CloseableKVStore kvstore;

    /**
     * Constructor.
     *
     * @param jdb {@link JSimpleDB} database
     * @param type type restriction, or null for no restriction
     * @throws IllegalArgumentException if {@code jdb} is null
     */
    protected SnapshotJObjectContainer(JSimpleDB jdb, Class<?> type) {
        super(jdb, type);
    }

// Connectable

    @Override
    public void disconnect() {
        super.disconnect();
        if (this.kvstore != null) {
            this.kvstore.close();
            this.kvstore = null;
        }
    }

    /**
     * (Re)load this container.
     *
     * <p>
     * This method opens a {@link JTransaction}, creates a {@link SnapshotJTransaction} using the {@link CloseableKVStore}
     * returned by {@link org.jsimpledb.kv.KVTransaction#mutableSnapshot KVTransaction.mutableSnapshot()}, and loads
     * the container using the objects returned by {@link #iterateObjects}.
     */
    public void reload() {

        // Grab KVStore snapshot
        final CloseableKVStore[] snapshotHolder = new CloseableKVStore[1];
        this.doInTransaction(new Runnable() {
            @Override
            public void run() {
                final JTransaction jtx = JTransaction.getCurrent();
                final CloseableKVStore snapshot = jtx.getTransaction().getKVTransaction().mutableSnapshot();
                jtx.getTransaction().addCallback(new Transaction.CallbackAdapter() {
                    @Override
                    public void afterCompletion(boolean committed) {
                        if (!committed)
                            snapshot.close();
                    }
                });
                snapshotHolder[0] = snapshot;
            }
        });

        // Replace previous KVStore snapshot (if any)
        if (this.kvstore != null) {
            this.kvstore.close();
            this.kvstore = null;
        }
        this.kvstore = snapshotHolder[0];

        // Create snapshot transaction based on the key/value store snapshot and load container
        final SnapshotJTransaction snapshotTx = this.jdb.createSnapshotTransaction(this.kvstore, false, ValidationMode.MANUAL);
        snapshotTx.performAction(new Runnable() {
            @Override
            public void run() {
                SnapshotJObjectContainer.this.load(SnapshotJObjectContainer.this.iterateObjects());
            }
        });
    }

    /**
     * Iterate the database objects that will be used to fill this container.
     * Objects should be returned in the desired order; duplicates and null values will be ignored.
     *
     * <p>
     * A {@link JTransaction} will be open in the current thread when this method is invoked.
     *
     * @return database objects
     */
    protected abstract Iterator<? extends JObject> iterateObjects();
}
