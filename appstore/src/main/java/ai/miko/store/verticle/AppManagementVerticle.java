package ai.miko.store.verticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.miko.store.domain.Application;
import ai.miko.store.domain.FetchAppsRequest;
import ai.miko.store.domain.FetchAppsRequest.InstalledApp;
import ai.miko.store.domain.UpdateApps;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.mongo.MongoClient;

public class AppManagementVerticle extends AbstractVerticle {

	private final MongoClient dbClient;

	public AppManagementVerticle(MongoClient dbClient) {
		this.dbClient = dbClient;
	}

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

		// Consumer for fetching scheduled apps
		vertx.eventBus().consumer("app.fetch", message -> {
			FetchAppsRequest request = (FetchAppsRequest) message.body();
			fetchScheduledApps(request).onComplete(ar -> {
				if (ar.succeeded()) {
					UpdateApps updateApps = new UpdateApps();
					updateApps.setApplications(ar.result());
					message.reply(updateApps);

					updateState(ar.result(), request.getRobotId());

				} else {
					message.fail(500, ar.cause().getMessage());
				}
			});
		});

		startPromise.complete();
	}

	private Future<List<Application>> fetchScheduledApps(
			FetchAppsRequest fetchApps) {
		Promise<List<Application>> promise = Promise.promise();
		JsonObject query = buildMongoQuery(fetchApps.getInstalledApps());

		dbClient.find("apps", query, ar -> {
			if (ar.succeeded()) {
				List<Application> apps = new ArrayList<>();
				for (JsonObject json : ar.result()) {
					Application app = new Application(json.getString("app_id"),
							json.getString("name"), json.getString("version"),
							json.getString("url"));
					apps.add(app);
				}
				promise.complete(apps);
			} else {
				promise.fail(ar.cause());
			}
		});

		return promise.future();
	}

	private JsonObject buildMongoQuery(List<InstalledApp> installedApps) {
		JsonObject query = new JsonObject();
		if (installedApps != null && !installedApps.isEmpty()) {
			List<JsonObject> conditions = new ArrayList<>();
			for (InstalledApp app : installedApps) {
				// Each condition will exclude the app with a matching app_id
				// and version
				JsonObject excludeCondition = new JsonObject()
						.put("app_id", app.getAppId())
						.put("version", app.getVersion());

				// We use $nor to exclude the apps that match both app_id and
				// version
				conditions.add(new JsonObject().put("$nor",
						List.of(excludeCondition)));
			}
			query.put("$and", conditions);
		}
		return query;
	}

	private Future<Void> updateState(List<Application> appsToBeUpdated,
			String robotId) {
		Promise<Void> promise = Promise.promise();
		if (Objects.nonNull(appsToBeUpdated) && !appsToBeUpdated.isEmpty()) {
			UpdateApps updateApps = new UpdateApps();
			updateApps.setRobotId(robotId);
			updateApps.setApplications(appsToBeUpdated);
			vertx.eventBus().request("state.update.scheduled", updateApps,
					reply -> {
						if (reply.succeeded()) {
							System.out.println("state changed");
						} else {
							reply.cause().printStackTrace();
							System.out.println("failed to update state.");
						}
					});
		}
		return promise.future();
	}
}
