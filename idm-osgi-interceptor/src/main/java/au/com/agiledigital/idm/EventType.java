package au.com.agiledigital.idm;

public enum EventType {
	CREATE("create"),
	UPDATE("update"),
	DELETE("delete"),
	READ("read"),
	ACTION("action"),
	PATCH("patch"),
	QUERY("query");

	private String name;
	EventType(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
}
