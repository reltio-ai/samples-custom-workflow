package com.reltio.workflow.sample.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskTO (String taskId, String assignee) {}
