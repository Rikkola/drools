package org.drools.api.data;


public interface DataStream<T>  extends DataSource<T> {

    /**
     * Append an object to this stream of data.
     */
    void append(T value);

}