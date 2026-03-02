package org.drools.core.rete.builder;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayDeque;
import java.util.Queue;

public class IdGenerator implements Externalizable {

    private static final long serialVersionUID = 510l;

    private Queue<Integer> recycledIds;
    private int            nextId;

    public IdGenerator() {
        this(1);
    }

    public IdGenerator(final int firstId) {
        this.nextId      = firstId;
        this.recycledIds = new ArrayDeque<>();
    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        recycledIds = (Queue<Integer>) in.readObject();
        nextId      = in.readInt();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(recycledIds);
        out.writeInt(nextId);
    }

    public synchronized int getNextId() {
        Integer id = this.recycledIds.poll();
        return (id == null) ? this.nextId++ : id;
    }

    public synchronized void releaseId(int id) {
        this.recycledIds.add(id);
    }

    public int getLastId() {
        return this.nextId - 1;
    }
}
