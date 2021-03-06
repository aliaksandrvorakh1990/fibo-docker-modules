package by.vorakh.dev.fibo.web.service.impl;

import by.vorakh.dev.fibo.jdbc.repository.TaskRepository;
import by.vorakh.dev.fibo.jdbc.repository.entity.TaskEntity;
import by.vorakh.dev.fibo.jdbc.repository.entity.TaskStatus;
import by.vorakh.dev.fibo.web.converter.MillisToTimeFormatConverter;
import by.vorakh.dev.fibo.web.exception.ImpossibleSolvingTaskException;
import by.vorakh.dev.fibo.web.exception.NoCompletedTaskException;
import by.vorakh.dev.fibo.web.exception.NoExistTaskException;
import by.vorakh.dev.fibo.web.model.payload.SequenceSize;
import by.vorakh.dev.fibo.web.model.response.CreationResponse;
import by.vorakh.dev.fibo.web.model.response.ResultResponse;
import by.vorakh.dev.fibo.web.service.TaskService;
import by.vorakh.dev.fibo.redis.entity.ProcessingTime;
import by.vorakh.dev.fibo.redis.repository.ProcessingTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static by.vorakh.dev.fibo.web.converter.TimeInMillisToUtcDateTimeConverter.convertUtcDateTimeFormat;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@Log4j2
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final Executor serviceExecutor;
    private final ProcessingTimeRepository processingTimeRepository;

    @Override
    public CompletableFuture<@NotNull CreationResponse> createTask(@NotNull SequenceSize sequenceSize) {

        long creationTime = System.currentTimeMillis();
        return taskRepository.create(new TaskEntity(sequenceSize.getSize(), creationTime))
            .thenApply(task -> {
                solveTask(task);
                return new CreationResponse(task.getId(), convertUtcDateTimeFormat(creationTime));
            });
    }

    private void solveTask(@NotNull TaskEntity task) {

        long taskId = task.getId();

        supplyAsync(() -> createFibonacciLine(task.getNumber()), serviceExecutor)
            .thenCombine(taskRepository.update(taskId, TaskStatus.PROCESSING), (result, avoid) -> result)
            .handle((result, throwable) -> {
                if ((throwable != null) || (result == null)) {
                    log.error("Task " + taskId + "was failed");
                    taskRepository.update(taskId, TaskStatus.FAILED);
                    throw new ImpossibleSolvingTaskException();
                }
                return result;
            })
            .thenAccept(result -> {
                long endTime = System.currentTimeMillis();
                long time = endTime - task.getCreationTime();

                CompletableFuture.allOf(
                    taskRepository.update(taskId, endTime, TaskStatus.COMPLETED, result),
                    processingTimeRepository.add(new ProcessingTime(taskId, time))
                ).handle((aVoid, throwable) -> {
                    if (throwable != null) {
                        log.error(throwable.getMessage());
                    }
                    log.info("The '{}' task Task is solved at {}", taskId, convertUtcDateTimeFormat(endTime));
                    return null;
                });
            });
    }

    @Override
    public CompletableFuture<@NotNull ResultResponse> getTaskResult(long taskId) {

        return taskRepository.getBy(taskId)
            .thenCombine(
                processingTimeRepository.findProcessingTime(taskId),
                (task, time) -> {

                    if (task == null) {
                        throw new NoExistTaskException();
                    }
                    if ((task.getStatus() == TaskStatus.CREATED) || (task.getStatus() == TaskStatus.PROCESSING)) {
                        throw new NoCompletedTaskException();
                    }

                    String processingTime = Optional.ofNullable(time)
                        .filter(aTime -> (aTime > 0L))
                        .map(MillisToTimeFormatConverter::convert)
                        .orElse("No data");
                    String result = Optional.ofNullable(task.getResult()).orElse("No data");
                    return new ResultResponse(task.getStatus(), result, processingTime);
                });
    }

    private @NotNull String createFibonacciLine(int length) {

        return Stream.iterate(new BigInteger[]{BigInteger.valueOf(0), BigInteger.valueOf(1)},
            fibonaccis -> {
                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
                return new BigInteger[]{fibonaccis[1], fibonaccis[0].add(fibonaccis[1])};
            })
            .limit(length)
            .map(t -> t[0])
            .map(BigInteger::toString)
            .collect(Collectors.joining(", "));
    }
}
