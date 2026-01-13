package com.reltio.workflow.sample.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessInstanceTO(String processInstanceId, List<TaskTO> currentTasks) {
}
