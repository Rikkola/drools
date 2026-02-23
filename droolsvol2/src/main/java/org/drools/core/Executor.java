package org.drools.core;

import org.drools.api.data.ObjectHandle;
import org.drools.api.data.DataStore;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Executor {
    BlockingQueue<Command> queue = new ArrayBlockingQueue<>(100000);

    private RuleUnitOperation ruleUnitOperation = new RuleUnitOperation();
    private DataStoreOperation dataStoreOperation = new  DataStoreOperation();

    public RuleUnitOperation ruleUnit() {
        return ruleUnitOperation;
    }

    public DataStoreOperation dataStore() {
        return dataStoreOperation;
    }

    public static class Command<I, A1, A2, F> {
        private CommandType type;
        private I instance;
        private A1 argument1;
        private A2 argument2;
        private Future<F> future;

        public Command<I, A1, A2, F> set(CommandType type, I instance, A1 argument1, A2 argument2, Future<F> future) {
            // This is done like this, to eventually use LMAX's (and others) pre-alloocation of events in the RingBuffer.
            this.argument1 = argument1;
            this.argument2 = argument2;
            this.instance = instance;
            this.type = type;
            this.future = future;

            return this;
        }

        public CommandType type() {
            return type;
        }

        public I instance() {
            return instance;
        }

        public A1 argument1() {
            return argument1;
        }

        public A2 argument2() {
            return argument2;
        }

        public Future<F> future() {
            return future;
        }
    }

    public enum CommandType {
        START, SUSPEND, CANCEL, RESUME, ADD, REMOVE, UPDATE;
    }

    public class RuleUnitOperation extends Command {

        public Future<Integer> start(RuleUnit ruleUnit) {
            Command<RuleUnit, Void, Void, Integer> cmd = new Command<>();
            cmd.set(CommandType.START,
                    ruleUnit, null, null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }

        public Future<Void> suspend(RuleUnit ruleUnit) {
            Command<RuleUnit, Void, Void, Void> cmd = new Command<>();
            cmd.set(CommandType.SUSPEND,
                    ruleUnit, null, null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }

        public Future<Void> cancel(RuleUnit ruleUnit) {
            Command<RuleUnit, Void, Void, Void> cmd = new Command<>();
            cmd.set(CommandType.CANCEL,
                    ruleUnit, null, null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }

        public Future<Void> resume(RuleUnit ruleUnit) {
            Command<RuleUnit, Void, Void, Void> cmd = new Command<>();
            cmd.set(CommandType.RESUME,
                    ruleUnit, null, null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }
    }

    public class DataStoreOperation {
        public <T, K extends T> Future<ObjectHandle<K>> add(DataStore<T> ds, K o) {
            Command<DataStore<T>, K, Void, ObjectHandle<K>> cmd = new Command<>();
            cmd.set(CommandType.ADD,
                    ds, o, null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }

        public <T, K extends T> Future<ObjectHandle<K>> update(DataStore<T> ds, ObjectHandle<K> h, K o) {
            Command<DataStore<T>, ObjectHandle<K>, K, ObjectHandle<K>> cmd = new Command<>();
            cmd.set(CommandType.ADD,
                    ds, h, o,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }

        public <T, K extends T> Future<ObjectHandle<K>> remove(DataStore<T> ds, ObjectHandle<K> h) {
            Command<DataStore<T>, ObjectHandle<K>, Void, ObjectHandle<K>> cmd = new Command<>();
            cmd.set(CommandType.REMOVE,
                    ds, h,null,
                    new CompletableFuture<>());
            queue.add(cmd);
            return cmd.future();
        }
    }
}
