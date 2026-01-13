package com.reltio.workflow.sample.workflow.delete;

import com.reltio.workflow.api.tasks.Task;
import com.reltio.workflow.api.validators.TaskValidator;
import com.reltio.workflow.api.validators.ValidationError;

import java.util.Date;

public class DeleteReviewValidator implements TaskValidator {

	@Override
	public void doValidate(Task task) throws Exception	{
		if (task.getDueDate().compareTo(new Date()) <= 0) {
			throw new ValidationError("Incorrect due date of task " + task.getId());
		}
	}
}
