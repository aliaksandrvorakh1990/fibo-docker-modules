package by.vorakh.dev.fibo.web.model.response;

import by.vorakh.dev.fibo.jdbc.repository.entity.TaskStatus;
import lombok.Data;

@Data
public class ResultResponse {

    private final TaskStatus status;
    private final String result;
    private final String processingTime;
}
