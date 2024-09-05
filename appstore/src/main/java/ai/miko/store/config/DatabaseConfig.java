package ai.miko.store.config;

import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

// not required now.

public class DatabaseConfig {

	private static volatile SqlClient mySQLClient;

	public static SqlClient getMySQLPool(Vertx vertx) {
		if (mySQLClient == null) {
			synchronized (DatabaseConfig.class) {
				if (mySQLClient == null) {
					MySQLConnectOptions connectOptions = new MySQLConnectOptions()
							.setHost("localhost").setPort(3306)
							.setDatabase("appstore").setUser("root")
							.setPassword("root");

					PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

					mySQLClient = MySQLBuilder.client().with(poolOptions)
							.connectingTo(connectOptions).using(vertx).build();
				}
			}

		}
		return mySQLClient;
	}
}
