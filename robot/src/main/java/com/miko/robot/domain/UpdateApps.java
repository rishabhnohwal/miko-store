package com.miko.robot.domain;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateApps implements Serializable {

	private static final long serialVersionUID = -462499061032340968L;

	private String robotId;
	private List<Application> applications;

	public UpdateApps() {
		super();
	}

	public UpdateApps(String robotId, List<Application> applications) {
		super();
		this.robotId = robotId;
		this.applications = applications;
	}

	public List<Application> getApplications() {
		return applications;
	}

	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}

	public String getRobotId() {
		return robotId;
	}

	public void setRobotId(String robotId) {
		this.robotId = robotId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(applications, robotId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UpdateApps other = (UpdateApps) obj;
		return Objects.equals(applications, other.applications)
				&& Objects.equals(robotId, other.robotId);
	}

	@Override
	public String toString() {
		return "UpdateApps [robotId=" + robotId + ", applications="
				+ applications + "]";
	}

}
