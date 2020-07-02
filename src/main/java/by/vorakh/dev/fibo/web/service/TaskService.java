package by.vorakh.dev.fibo.web.service;

import by.vorakh.dev.fibo.web.model.payload.SequenceSize;
import by.vorakh.dev.fibo.web.model.response.CreationResponse;
import by.vorakh.dev.fibo.web.model.response.ResultResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface TaskService {

    CompletableFuture<@NotNull CreationResponse> createTask(@NotNull SequenceSize sequenceSize);

    CompletableFuture<@NotNull ResultResponse> getTaskResult(long taskId);
}
