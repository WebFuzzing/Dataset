package script_transform_csv_to_mongodb_and_neo4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;


public class ParallelExecutor implements AutoCloseable {

    private final ExecutorService executor;

    public ParallelExecutor() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static void getFutures(Future[] futures) {
        for (Future future : futures) {
            try {
                if (future != null) future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public <T, R> Future<R> submit(Function<T, R> task, T input) {
        return executor.submit(() -> {

            return task.apply(input);
        });
    }

    public <T> Future<T> submit(Supplier<T> task) {
        return executor.submit(() -> {
            return task.get();
        });

    }

    public <R> Future<R> submit(Function<String, R> task, String input) {
        return executor.submit(() -> {

            return task.apply(input);
        });

    }

    public Future submit(Runnable runnable) {
        return executor.submit(runnable);
    }

    public void shutdown() {
        executor.shutdown();
    }

    //    public static void getFutures(List<Future> futureList){
    //        for (Future future :futureList) {
    //            try {
    //                if (future!=null) future.get();
    //            } catch (InterruptedException e) {
    //                throw new RuntimeException(e);
    //            } catch (ExecutionException e) {
    //                throw new RuntimeException(e);
    //            }
    //        }
    //    }
    public static void getFutures(Iterable<Future> futureIterable) {
        for (Future future : futureIterable) {
            try {
                if (future != null) future.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() {
        shutdown();
    }

    public <T, R, U> Future<U> submit(BiFunction<T, R, U> function, T arg1, R arg2) {
        return executor.submit(() -> {

            return function.apply(arg1, arg2);
        });
    }
}
