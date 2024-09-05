package ai.miko.store.verticle;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.miko.store.domain.Application;
import ai.miko.store.domain.ApplicationState;
import ai.miko.store.domain.UpdateAppStateRequest;
import ai.miko.store.domain.UpdateApps;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.mongo.UpdateOptions;

public class StateManagementVerticle extends AbstractVerticle {

	private final MongoClient dbClient;

	private int retryCountForMail = 2;

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

	public StateManagementVerticle(MongoClient dbClient) {
		this.dbClient = dbClient;
	}

	@Override
	public void start(Promise<Void> startPromise) {

		vertx.eventBus().consumer("state.update", message -> {
			UpdateAppStateRequest updateApp = (UpdateAppStateRequest) message
					.body();
			List<UpdateAppStateRequest> updateStateApps = new ArrayList<>();
			updateStateApps.add(updateApp);
			updateState(updateStateApps).compose(res -> updateLogs(res))
					.onComplete(ar -> {
						if (ar.succeeded()) {
							message.reply("State updated");
						} else {
							message.fail(500, ar.cause().getMessage());
						}
					});
			if (updateApp.getRetryCount() >= retryCountForMail
					&& ApplicationState.ERROR == updateApp.getState()) {
				System.out.println("Sending mail");
				vertx.eventBus().send("notification.send", updateApp);
			}
		});

		vertx.eventBus().consumer("state.update.scheduled", message -> {
			UpdateApps updateApps = (UpdateApps) message.body();
			List<UpdateAppStateRequest> updateStateApps = new ArrayList<>();
			for (Application app : updateApps.getApplications()) {
				UpdateAppStateRequest temp = new UpdateAppStateRequest();
				temp.setAppId(app.getAppId());
				temp.setRobotId(updateApps.getRobotId());
				temp.setState(ApplicationState.SCHEDULED);
				temp.setVersion(app.getVersion());
				temp.setRetryCount(0);
				temp.setTimestamp(Instant.now());
				updateStateApps.add(temp);
			}
			updateState(updateStateApps).compose(result -> updateLogs(result))
					.onComplete(ar -> {
						if (ar.succeeded()) {
							System.out.println("State updated and logged");
							message.reply("State updated and logged");
						} else {
							message.fail(500, ar.cause().getMessage());
						}
					});
		});

		startPromise.complete();
	}

	private Future<List<UpdateAppStateRequest>> updateState(
			List<UpdateAppStateRequest> appsStates) {
		Promise<List<UpdateAppStateRequest>> promise = Promise.promise();

		for (UpdateAppStateRequest app : appsStates) {
			JsonObject query = new JsonObject().put("app_id", app.getAppId())
					.put("robot_id", app.getRobotId());

			JsonObject updateFields = new JsonObject()
					.put("state", app.getState())
					.put("version", app.getVersion())
					.put("retries", app.getRetryCount())
					.put("mod_ts", Instant.now())
					.put("op_prfmd_ts", app.getTimestamp());

			// If the state is COMPLETED, update the installedVersion as well
			if (ApplicationState.COMPLETED == app.getState()) {
				updateFields.put("installedVersion", app.getVersion());
			}

			JsonObject update = new JsonObject().put("$set", updateFields);

			// Define the options to return the document after updating
			// Project thestate_id field
			FindOptions findOptions = new FindOptions()
					.setFields(new JsonObject().put("state_id", 1));
			UpdateOptions updateOptions = new UpdateOptions()
					.setReturningNewDocument(false);
			// Return the updated document

			// If the document exists, update it. If not, insert a new one.
			dbClient.findOneAndUpdateWithOptions("app_states", query, update,
					findOptions, updateOptions, res -> {
						if (res.succeeded()) {
							JsonObject updatedDoc = res.result();
							if (updatedDoc != null) {
								// Set the stateId from the existing entry
								app.setStateId(
										updatedDoc.getString("state_id"));
								System.out.println(
										"State updated for existing entry: "
												+ app.getStateId());
							} else {
								// If no document is found, create a new one
								String stateId = UUID.randomUUID().toString();
								app.setStateId(stateId);
								JsonObject newState = new JsonObject()
										.put("state_id", stateId)
										.put("app_id", app.getAppId())
										.put("robot_id", app.getRobotId())
										.put("state", app.getState())
										.put("version", app.getVersion())
										.put("installedVersion",
												app.getVersion())
										.put("retries", app.getRetryCount())
										.put("ent_ts", Instant.now())
										.put("mod_ts", Instant.now())
										.put("op_prfmd_ts", app.getTimestamp());

								dbClient.insert("app_states", newState,
										insertRes -> {
											if (insertRes.succeeded()) {
												System.out.println(
														"New state inserted with stateId: "
																+ stateId);
											} else {
												promise.fail(
														"Failed to insert new state: "
																+ insertRes
																		.cause()
																		.getMessage());
											}
										});
							}
						} else {
							promise.fail("Failed to update state: "
									+ res.cause().getMessage());
						}
					});
		}

		promise.complete(appsStates);
		return promise.future();
	}

	private Future<Void> updateLogs(List<UpdateAppStateRequest> updateApps) {
		Promise<Void> promise = Promise.promise();
		vertx.eventBus().request("log.insert", updateApps, reply -> {
			if (reply.succeeded()) {
				System.out.println("Log inserted.");
				promise.complete();
			} else {
				System.out.println("Failure to insert log.");
				reply.cause().printStackTrace();
				promise.fail("Fail to insert Log.");
			}
		});
		return promise.future();
	}
}
