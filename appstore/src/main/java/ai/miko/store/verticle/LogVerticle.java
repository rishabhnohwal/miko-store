package ai.miko.store.verticle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.miko.store.domain.UpdateAppStateRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.mongo.MongoClient;

public class LogVerticle extends AbstractVerticle {

	private final MongoClient dbClient;

	public LogVerticle(MongoClient dbClient) {
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
		vertx.eventBus().consumer("log.insert", this::insertLogs);
		startPromise.complete();
	}

	private void insertLogs(Message<List<UpdateAppStateRequest>> message) {
		List<UpdateAppStateRequest> logData = (List<UpdateAppStateRequest>) message
				.body();

		for (UpdateAppStateRequest app : logData) {
			JsonObject logEntry = new JsonObject()
					.put("log_id", UUID.randomUUID().toString())
					.put("state_id", app.getStateId())
					.put("app_id", app.getAppId())
					.put("robot_id", app.getRobotId())
					.put("state", app.getState())
					.put("message", app.getMessage())
					.put("version", app.getVersion())
					.put("retries", app.getRetryCount())
					.put("ent_ts", LocalDateTime.now().toString())
					.put("op_prfmd_ts", app.getTimestamp());

			dbClient.insert("app_logs", logEntry, res -> {
				if (res.succeeded()) {
					message.reply("Log inserted successfully");
				} else {
					message.fail(500, "Failed to insert log: "
							+ res.cause().getMessage());
				}
			});
		}
	}

}
