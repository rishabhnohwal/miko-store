package ai.miko.store.verticle;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ai.miko.store.domain.UpdateAppStateRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;

public class NotificationVerticle extends AbstractVerticle {

	private MailClient mailClient;

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
		MailConfig mailConfig = new MailConfig().setHostname("smtp.gmail.com")
				.setPort(587).setStarttls(StartTLSOptions.REQUIRED)
				.setUsername("")
				.setPassword("");

		mailClient = MailClient.create(vertx, mailConfig);

		vertx.eventBus().consumer("notification.send", message -> {
			System.out.println("message recieved");
			UpdateAppStateRequest updateApp = (UpdateAppStateRequest) message
					.body();
			System.out.println(updateApp.toString());
			sendNotificationEmail(updateApp).onComplete(ar -> {
				if (ar.succeeded()) {
					message.reply("Notification sent");
					System.out.println("Mail Sent.");
				} else {
					System.err.println("Notification not sent.");
					ar.cause().printStackTrace();
					message.fail(500, ar.cause().getMessage());
				}
			});
		});

		startPromise.complete();
	}

	private Future<Void> sendNotificationEmail(
			UpdateAppStateRequest updateApp) {
		Promise<Void> promise = Promise.promise();
		System.out.println("Begining to send mail.");
		MailMessage message = new MailMessage().setFrom("")
				.setTo("")
				.setSubject("Installation Failed")
				.setText("The installation of " + updateApp.getAppId()
						+ " version: " + updateApp.getVersion()
						+ " has failed after multiple attempts for Miko: "
						+ updateApp.getRobotId());

		mailClient.sendMail(message, ar -> {
			if (ar.succeeded()) {
				System.out.println("Mail Sent Successfully");
				promise.complete();
			} else {
				System.err.println("Could Not Send mail.");
				ar.cause().printStackTrace();
				promise.fail(ar.cause());
			}
		});

		return promise.future();
	}
}
