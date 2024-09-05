package ai.miko.store.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FetchAppsRequest implements Serializable {

	private static final long serialVersionUID = -58371648380693434L;

	private String robotId;
	private List<InstalledApp> installedApps;

	public FetchAppsRequest() {
	}

	public FetchAppsRequest(String robotId, List<InstalledApp> installedApps) {
		this.robotId = robotId;
		this.installedApps = installedApps;
	}

	public String getRobotId() {
		return robotId;
	}

	public void setRobotId(String robotId) {
		this.robotId = robotId;
	}

	public List<InstalledApp> getInstalledApps() {
		return installedApps;
	}

	public void setInstalledApps(List<InstalledApp> installedApps) {
		this.installedApps = installedApps;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	public static class InstalledApp implements Serializable {

		private static final long serialVersionUID = -5865728850854654353L;

		private String appId;
		private String version;

		public InstalledApp() {
		}

		public InstalledApp(String appId, String version) {
			this.appId = appId;
			this.version = version;
		}

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}
}
