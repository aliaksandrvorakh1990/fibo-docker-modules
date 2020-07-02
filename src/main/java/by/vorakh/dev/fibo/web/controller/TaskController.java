package by.vorakh.dev.fibo.web.controller;

import by.vorakh.dev.fibo.web.exception.IncorrectFibonacciSequenceSizeException;
import by.vorakh.dev.fibo.web.model.payload.SequenceSize;
import by.vorakh.dev.fibo.web.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

import static by.vorakh.dev.fibo.web.validation.SequenceSizeValidator.isCorrectSize;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @PostMapping("/task")
    public @NotNull CompletableFuture<ResponseEntity<?>> createTask(@RequestBody @NotNull SequenceSize sequenceSize) {

        if (!isCorrectSize(sequenceSize)) {
            throw new IncorrectFibonacciSequenceSizeException();
        }

        return service.createTask(sequenceSize)
            .thenApply(createdTask -> new ResponseEntity<>(createdTask, HttpStatus.OK));
    }
}
