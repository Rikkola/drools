package org.drools.core;

public class ObjectHandleTuple<T> extends LeftTuple<T> {
    private ObjectHandleImpl<T>  handle;

    public ObjectHandleTuple(ObjectHandleImpl<T> handle, NetworkNode node) {
        super(handle, node);
        this.handle = handle;
    }

    @Override
    public T get() {
        return handle.get();
    }

    @Override
    public T getObject() {
        return handle.get();
    }

    @Override
    public void reAdd() {

    }

    @Override
    public TupleImpl getNextParentWithHandle() {
        return this;
    }

    @Override
    public ObjectTypeNodeId getInputOtnId() {
        return null;
    }

    @Override
    public boolean isLeftTuple() {
        return false;
    }

    @Override
    public ObjectHandleImpl<T> getObjectHandle() {
        return handle;
    }

    @Override
    public ObjectHandleImpl<T> getHandle() {
        return handle;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();

        buffer.append(getClass().getSimpleName() + "[");
        buffer.append("index=").append(getNetworkNode().getPathIndex()).append(", ");
        buffer.append("handle=").append(handle);
        buffer.append("]");

        return buffer.toString();
    }


    @Override
    public boolean hasLeftParent() {
        return true;
    }

    @Override
    public boolean hasRightParent() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}
        if (!super.equals(o)) {return false;}

        ObjectHandleTuple<?> that = (ObjectHandleTuple<?>) o;
        return getHandle().equals(that.getHandle());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getHandle().hashCode();
        return result;
    }
}
