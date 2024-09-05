package com.miko.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miko.robot.config.RobotConfig;
import com.miko.robot.domain.Application;
import com.miko.robot.domain.FetchAppsRequest;
import com.miko.robot.domain.FetchAppsRequest.InstalledApp;
import com.miko.robot.domain.UpdateApps;
import com.miko.robot.verticle.AppInstallWorkerVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;

public class MainVerticle extends AbstractVerticle {

	// 6 hours in milliseconds
	private static final long FETCH_INTERVAL = 6 * 60 * 60 * 1000;

	// 20 seconds in milliseconds
	private static final long FETCH_INTERVAL2 = 20 * 1000;

	private static final String ROBOT_ID = "123";

	static {
		var objectMapper = DatabindCodec.mapper();
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.disable(
				SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
		objectMapper.disable(
				DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);

		JavaTimeModule module = new JavaTimeModule();
		objectMapper.registerModule(module);
	}

	@Override
	public void start(Promise<Void> startPromise) {
		// EventBus eventBus = vertx.eventBus();

		// Deploy worker verticle to handle app installation sequentially
		vertx.deployVerticle(new AppInstallWorkerVerticle(),
				new DeploymentOptions()
						.setThreadingModel(ThreadingModel.WORKER));

		// Initial fetch on startup
		fetchAppsAndProcess();

		// Schedule periodic fetching
		vertx.setPeriodic(FETCH_INTERVAL2, id -> fetchAppsAndProcess());

		startPromise.complete();
	}

	private void fetchAppsAndProcess() {
		FetchAppsRequest fetchAppsRequest = new FetchAppsRequest();
		fetchAppsRequest.setRobotId(ROBOT_ID);

		List<InstalledApp> apps = new ArrayList<>();
		for (Entry<String, String> entry : RobotConfig.installedApps
				.entrySet()) {
			InstalledApp app = new InstalledApp();
			app.setAppId(entry.getKey());
			app.setVersion(entry.getValue());
			apps.add(app);
		}

		fetchAppsRequest.setInstalledApps(apps);

		HttpClient client = vertx.createHttpClient();
		String fetchUrl = "http://localhost:8080/api/apps";

		RequestOptions options = new RequestOptions().setMethod(HttpMethod.POST)
				.setAbsoluteURI(fetchUrl);

		client.request(options).compose(request -> request
				.send(Json.encode(fetchAppsRequest)).compose(response -> {
					if (response.statusCode() == 200) {
						return response.body();
					} else {
						System.err.println("Failed to fetch apps: "
								+ response.statusCode());
						return null;
					}
				}).onSuccess(body -> {
					if (body != null) {
						UpdateApps updateApps = Json.decodeValue(body,
								UpdateApps.class);
						System.out.println(updateApps);
						try {
							scheduleAppsForInstallation(updateApps);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}).onFailure(err -> {
					System.err.println("Request failed: " + err.getMessage());
				}));
	}

	private void scheduleAppsForInstallation(UpdateApps apps)
			throws InterruptedException {
		EventBus eventBus = vertx.eventBus();
		for (Application app : apps.getApplications()) {
			// Send the app to the worker verticle for installation
			eventBus.send("app.install", Json.encode(app));
		}
	}
}