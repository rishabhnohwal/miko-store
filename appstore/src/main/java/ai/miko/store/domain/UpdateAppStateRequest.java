package ai.miko.store.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UpdateAppStateRequest implements Serializable {

	private static final long serialVersionUID = -4577295285110218293L;

	private String appId;
	private String robotId;
	private String version;
	private int retryCount;
	private ApplicationState state;
	private String message;

	private Instant timestamp;

	private String stateId;

	public UpdateAppStateRequest() {
		super();
	}
	public UpdateAppStateRequest(String appId, String robotId, String version,
			int retryCount, ApplicationState state, String message,
			Instant timestamp) {
		super();
		this.appId = appId;
		this.robotId = robotId;
		this.version = version;
		this.retryCount = retryCount;
		this.state = state;
		this.message = message;
		this.timestamp = timestamp;
	}
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	public String getRobotId() {
		return robotId;
	}
	public void setRobotId(String robotId) {
		this.robotId = robotId;
	}
	public int getRetryCount() {
		return retryCount;
	}
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}
	public ApplicationState getState() {
		return state;
	}
	public void setState(ApplicationState state) {
		this.state = state;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Instant getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getStateId() {
		return stateId;
	}
	public void setStateId(String stateId) {
		this.stateId = stateId;
	}
	@Override
	public int hashCode() {
		return Objects.hash(appId, message, retryCount, robotId, state,
				timestamp);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateAppStateRequest other = (UpdateAppStateRequest) obj;
		return Objects.equals(appId, other.appId)
				&& Objects.equals(message, other.message)
				&& retryCount == other.retryCount
				&& Objects.equals(robotId, other.robotId)
				&& Objects.equals(state, other.state)
				&& Objects.equals(timestamp, other.timestamp);
	}
	@Override
	public String toString() {
		return "UpdateAppStateRequest [appId=" + appId + ", robotId=" + robotId
				+ ", retryCount=" + retryCount + ", state=" + state
				+ ", message=" + message + ", timestamp=" + timestamp + "]";
	}

}
