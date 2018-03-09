package org.processmining.plugins.kafka;

public class LogEntry {
	private String eventId;
	private String caseId;
	private Long timestamp;
	
	public LogEntry(String serializedEntry) {
		super();
		String[] split = serializedEntry.split(",", 3);
        this.caseId = split[0];
        this.timestamp = Long.parseLong(split[1]);
		this.eventId = split[2];
	}
	
	public LogEntry(String eventId, String caseId, Long timestamp) {
		super();
		this.eventId = eventId;
		this.caseId = caseId;
		this.timestamp = timestamp;
	}

	public String getEventId() {
		return eventId;
	}
	
	public String getCaseId() {
		return caseId;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((caseId == null) ? 0 : caseId.hashCode());
		result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
		result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogEntry other = (LogEntry) obj;
		if (caseId == null) {
			if (other.caseId != null)
				return false;
		} else if (!caseId.equals(other.caseId))
			return false;
		if (eventId == null) {
			if (other.eventId != null)
				return false;
		} else if (!eventId.equals(other.eventId))
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}
}
