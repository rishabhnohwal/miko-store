package ai.miko.store;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.miko.store.domain.FetchAppsRequest;
import ai.miko.store.domain.UpdateAppStateRequest;
import ai.miko.store.domain.UpdateApps;
import ai.miko.store.verticle.AppManagementVerticle;
import ai.miko.store.verticle.LogVerticle;
import ai.miko.store.verticle.NotificationVerticle;
import ai.miko.store.verticle.StateManagementVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.ThreadingModel;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;

public class MainVerticle extends AbstractVerticle {

	private MongoClient mongoClient;

	static {
		// Intermediate solution for LocalDateTime/Instant Json
		// encoding/decoding exception
		System.out.println("Customizing the built-in jackson ObjectMapper...");
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
		String connectionString = "mongodb+srv://<username>:<password>@store.dsvyk.mongodb.net/?retryWrites=true&w=majority&appName=store";
		JsonObject config = new JsonObject()
				.put("connection_string", connectionString)
				.put("db_name", "app_store");

		mongoClient = MongoClient.create(vertx, config);

		// Deploy the verticles sequentially and then start the HTTP server
		deployVerticles().compose(v -> createHttpServer()).onComplete(ar -> {
			if (ar.succeeded()) {
				startPromise.complete();
				System.out.println(
						"All verticles deployed successfully and HTTP server started!");
			} else {
				startPromise.fail(ar.cause());
			}
		});
	}

	private Future<Void> deployVerticles() {
		Promise<Void> promise = Promise.promise();

		vertx.deployVerticle(new AppManagementVerticle(mongoClient))
				.compose(v -> vertx.deployVerticle(
						new StateManagementVerticle(mongoClient)))
				.compose(v -> vertx.deployVerticle(new NotificationVerticle(),
						new DeploymentOptions()
								.setThreadingModel(ThreadingModel.WORKER)))
				.compose(
						v -> vertx.deployVerticle(new LogVerticle(mongoClient)))
				.onComplete(ar -> {
					if (ar.succeeded()) {
						promise.complete();
					} else {
						promise.fail(ar.cause());
					}
				});

		return promise.future();
	}

	private Future<Void> createHttpServer() {
		Promise<Void> promise = Promise.promise();
		Router router = Router.router(vertx);

		router.route().handler(LoggerHandler.create());
		router.route().handler(BodyHandler.create());

		router.route().failureHandler(ctx -> {
			Throwable failure = ctx.failure();
			int statusCode = ctx.statusCode();

			if (failure instanceof RuntimeException) {
				statusCode = 400;
				failure.printStackTrace();
				ctx.response().setStatusCode(statusCode)
						.putHeader("content-type", "application/json")
						.end(new JsonObject()
								.put("error", "Custom error occurred")
								.toBuffer());
			} else {
				ctx.response()
						.setStatusCode(statusCode != -1 ? statusCode : 500)
						.putHeader("content-type", "application/json")
						.end(new JsonObject().put("error", failure.getMessage())
								.toBuffer());
			}
		});

		router.post("/api/apps").handler(ctx -> {
			FetchAppsRequest requestBody = Json
					.decodeValue(ctx.body().asString(), FetchAppsRequest.class);
			vertx.eventBus().request("app.fetch", requestBody, reply -> {
				if (reply.succeeded()) {
					UpdateApps updateApps = (UpdateApps) reply.result().body();
					ctx.response().setStatusCode(200)
							.putHeader("content-type", "application/json")
							.end(Json.encode(updateApps));
				} else {
					ctx.fail(reply.cause());
				}
			});
		});

		router.post("/api/state-update").handler(ctx -> {
			UpdateAppStateRequest updateRequest = Json.decodeValue(
					ctx.body().asString(), UpdateAppStateRequest.class);
			vertx.eventBus().request("state.update", updateRequest, reply -> {
				if (reply.succeeded()) {
					ctx.response().putHeader("content-type", "text/plain")
							.end("State and logs updated successfully");
				} else {
					ctx.fail(reply.cause());
				}
			});
		});

		// Feature to be developed.
		router.post("/api/analytics").handler(ctx -> {
			// AnalyticsRequest analyticsReq =
			// Json.decodeValue(ctx.body().asString(), AnalyticsRequest.class);
			vertx.eventBus().request("logs.analytics", "analyticsReq",
					reply -> {
						if (reply.succeeded()) {
							// To be developed.
						} else {
							ctx.fail(reply.cause());
						}
					});
		});

		vertx.createHttpServer().requestHandler(router).listen(8080, http -> {
			if (http.succeeded()) {
				System.out.println("HTTP server started on port 8080");
				promise.complete();
			} else {
				promise.fail(http.cause());
			}
		});

		return promise.future();
	}

	@Override
	public void stop(Promise<Void> stopPromise) {
		if (mongoClient != null) {
			mongoClient.close();
		}
		stopPromise.complete();
	}
}
