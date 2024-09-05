package com.miko.robot.domain;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Application implements Serializable {

	private static final long serialVersionUID = -3470857518232284045L;

	private String appId;
	private String name;
	private String version;
	private String url;

	private String stateId;
	private ApplicationState state;
	private int retryCount;

	public Application() {
		super();
	}

	public Application(String appId, String name, String version, String url) {
		super();
		this.appId = appId;
		this.name = name;
		this.version = version;
		this.url = url;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getStateId() {
		return stateId;
	}

	public void setStateId(String stateId) {
		this.stateId = stateId;
	}

	public ApplicationState getState() {
		return state;
	}

	public void setState(ApplicationState state) {
		this.state = state;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(appId, name, url, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Application other = (Application) obj;
		return Objects.equals(appId, other.appId)
				&& Objects.equals(name, other.name)
				&& Objects.equals(url, other.url)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return "Application [appId=" + appId + ", name=" + name + ", version="
				+ version + ", url=" + url + "]";
	}

}
