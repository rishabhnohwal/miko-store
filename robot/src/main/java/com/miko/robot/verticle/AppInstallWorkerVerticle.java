package com.miko.robot.verticle;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.miko.robot.config.RobotConfig;
import com.miko.robot.domain.Application;
import com.miko.robot.domain.ApplicationState;
import com.miko.robot.domain.UpdateAppStateRequest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;

public class AppInstallWorkerVerticle extends AbstractVerticle {

	private static final String ROBOT_ID = "123";
	private final ConcurrentLinkedQueue<Application> installQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<Application> errorQueue = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean isProcessing = new AtomicBoolean(false);
	private int retryCountForInstall = 3;

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

		EventBus eventBus = vertx.eventBus();

		eventBus.consumer("app.install", this::enqueueAppForInstallation);

		startPromise.complete();
	}

	private void enqueueAppForInstallation(Message<String> message) {
		Application app = Json.decodeValue(message.body(), Application.class);
		installQueue.add(app);
		processNextApp();
	}

	private void processNextApp() {
		if (isProcessing.get()) {
			// Another app is being processed
			return;
		}

		Application app = installQueue.poll();
		if (app == null) {
			// No app to process
			processErroredApps();
			return;
		}

		isProcessing.set(true);

		System.out.println("Received app for installation: " + app.getAppId());

		updateAppState(app.getAppId(), ROBOT_ID, app.getVersion(),
				ApplicationState.PICKEDUP,
				"App Picked Up for Downloading and Installation.",
				app.getRetryCount()).onComplete(sc -> {
					downloadAndInstallApp(app).onSuccess(v -> {
						System.out.println("App installation completed: "
								+ app.getAppId());
						RobotConfig.installedApps.put(app.getAppId(),
								app.getVersion());
						updateAppState(app.getAppId(), ROBOT_ID,
								app.getVersion(), ApplicationState.COMPLETED,
								"App installed successfully.",
								app.getRetryCount()).onComplete(result -> {
									isProcessing.set(false);
									// Process the next app
									processNextApp();
								});
					}).onFailure(err -> {
						System.err.println("App installation failed: "
								+ app.getAppId() + " - " + err.getMessage());
						updateAppState(app.getAppId(), ROBOT_ID,
								app.getVersion(), ApplicationState.ERROR,
								"Installation failed: " + err.getMessage(),
								app.getRetryCount()).onComplete(result -> {
									app.setRetryCount(app.getRetryCount() + 1);
									if (app.getRetryCount() < retryCountForInstall) {
										// Add to errorQueue if retries left
										errorQueue.add(app);
									}
									isProcessing.set(false);
									// Process the next app
									processNextApp();
								});
					});
				});
	}

	private void processErroredApps() {
		if (installQueue.isEmpty() && !errorQueue.isEmpty()) {
			Application app;
			while ((app = errorQueue.poll()) != null) {
				if (app.getRetryCount() < retryCountForInstall) {
					installQueue.add(app); // Add back to installQueue for retry
				} else {
					System.err.println(
							"Max retries reached for app: " + app.getAppId());
				}
			}
			processNextApp();
		}
	}

	private Future<Void> downloadAndInstallApp(Application app) {
		Promise<Void> promise = Promise.promise();
		System.out.println("Started Downloading app: " + app.getAppId());
		// Simulate the download process (replace with actual logic)
		vertx.setTimer(5000, id -> {
			System.out
					.println("Downloading in progress app: " + app.getAppId());
			boolean downloadSuccess = false; // Simulate success

			if (downloadSuccess) {
				System.out.println("Downloaded app: " + app.getAppId());
				installApp(app, promise); // Pass the promise to installApp
			} else {
				System.err.println("Downloaded failed: " + app.getAppId());
				promise.fail("Download failed");
			}
		});

		return promise.future();
	}

	private void installApp(Application app, Promise<Void> promise) {
		System.out.println("Started Installing app: " + app.getAppId());

		// Simulate installation
		vertx.setTimer(3000, id -> {
			System.out.println("Installing in progress app: " + app.getAppId());
			boolean installSuccess = true; // Simulate success

			if (installSuccess) {
				System.out.println("Installed app: " + app.getAppId());
				promise.complete();
			} else {
				System.err.println("Installing failed: " + app.getAppId());
				promise.fail("Installation failed");
			}
		});
	}

	private Future<Void> updateAppState(String appId, String robotId,
			String version, ApplicationState state, String message,
			int retryCount) {
		Promise<Void> promise = Promise.promise();
		HttpClient client = vertx.createHttpClient();

		UpdateAppStateRequest updateAppStateReq = new UpdateAppStateRequest(
				appId, robotId, version, retryCount, state, message,
				Instant.now());

		String updateUrl = "http://localhost:8080/api/state-update";

		RequestOptions options = new RequestOptions().setMethod(HttpMethod.POST)
				.setAbsoluteURI(updateUrl);

		client.request(options)
				.compose(req -> req.send(Json.encode(updateAppStateReq)))
				.compose(resp -> {
					if (resp.statusCode() == 200) {
						System.out.println(
								"State updated successfully for app: " + appId);
						promise.complete();
					} else {
						System.err.println(
								"Failed to update state for app: " + appId);
						promise.fail("Failed to update state");
					}
					return promise.future();
				}).onFailure(pr -> {
					System.err.println(
							"Failed to update state for app: " + appId);
					pr.printStackTrace();
				});

		return promise.future();
	}
}
