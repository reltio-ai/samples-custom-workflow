package com.company.workflow.custom.dcr;

import com.reltio.workflow.api.WorkflowService;
import com.reltio.workflow.api.listeners.WorkflowTaskListener;
import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.tasks.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DueDateAdjustListener implements WorkflowTaskListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DueDateAdjustListener.class);

    @WorkflowService
    TaskService taskService;

    @Override
    public void notify(Task task) {
        LOGGER.info("Extending dueDate with weekends");
        LocalDateTime dueDate = LocalDateTime.ofInstant(task.getDueDate().toInstant(), ZoneId.systemDefault());
        LocalDate cursor = LocalDate.now();

        //extend due period if weekend day presented in it
        while (!cursor.isAfter(dueDate.toLocalDate())) {
            if (isWeekend(cursor)) {
                dueDate = dueDate.plusDays(1);
            }
            cursor = cursor.plusDays(1);
        }

        taskService.setDueDate(task.getId(), Date.from(dueDate.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private boolean isWeekend(LocalDate localDate) {
        return localDate.getDayOfWeek() == DayOfWeek.SATURDAY || localDate.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
