package org.drools.core;

import org.drools.api.data.DataHandle;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toCollection;

public class DataHandleFactory {
    /** The fact id. */
    private IdsGenerator idGen;

    /** The number of facts created - used for recency. */
    private long counter;

    public DataHandleFactory() {
        // starts at 0. So first assigned is 1.
        // 0 is hard coded to Initialfact
        this.idGen = new IdsGenerator(0);
        counter = 0;
    }

    public DataHandle newDataHandle(Object object) {
        return new DataHandleImpl(idGen.getNextId(), object);
    }

    public long getCurrentId() {
        return idGen.getId();
    }

    public long getCurrentRecency() {
        return counter;
    }

    public void reset(long id, long counter) {
        this.idGen = new IdsGenerator( id );
        this.counter = counter;
    }


    private static class IdsGenerator {

        /** The fact id. */
        private AtomicLong id;

        private Queue<Long> usedIds;
        private long        recycledId;

        private IdsGenerator( long startId ) {
            this.id = new AtomicLong( startId );
        }

        public long getNextId() {
            return hasRecycledId() ? recycledId++ : this.id.incrementAndGet();
        }

        private boolean hasRecycledId() {
            if (usedIds != null) {
                while ( !usedIds.isEmpty() ) {
                    long firstUsedId = usedIds.peek();
                    if ( recycledId < firstUsedId ) {
                        return true;
                    } else if ( recycledId == firstUsedId ) {
                        recycledId++;
                    }
                    usedIds.poll();
                }
                usedIds = null;
            }
            return false;
        }

        public long getId() {
            return this.id.get();
        }

        public void doRecycle(Collection<Long> usedIds) {
            this.usedIds = usedIds.stream().sorted().collect( toCollection(ArrayDeque::new));
            this.usedIds.add( id.get()+1 );
            this.recycledId = 1;
        }

        public void stopRecycle() {
            this.usedIds = null;
        }
    }
}
