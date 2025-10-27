package io.eeaters.langgraph.example.model;

public enum Party {

	TRAILER_COMPANY("拖车公司"), WAREHOUSE("仓库");

	private final String description;

	Party(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

}