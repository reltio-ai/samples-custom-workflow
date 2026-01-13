package com.reltio.workflow.external.services.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeRequest {

	@JsonProperty("uri")
	private String uri;
	@JsonProperty("createdBy")
	private String createdBy;
	@JsonProperty("createdTime")
	private Long createdTime;
	@JsonProperty("updatedBy")
	private String updatedBy;
	@JsonProperty("updatedTime")
	private Long updatedTime;
	@JsonProperty("changes")
	private Map<String, List<Object>> changes;
	@JsonProperty("state")
	private String state;
	@JsonIgnore
	private Map<String, Map<String, Object>> objectsInfo;
	@JsonIgnore
	private Map<String, Object> additionalProperties;

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Map<String, List<Object>> getChanges() {
		return changes;
	}

	public void setChanges(Map<String, List<Object>> changes) {
		this.changes = changes;
	}
	public void setState(String state) {
		this.state = state;
	}
}