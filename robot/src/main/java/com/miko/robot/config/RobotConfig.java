package com.miko.robot.config;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RobotConfig {

	public static ConcurrentMap<String, String> installedApps;
	public static Long size;

	static {
		// fetching or initializing the installed Apps.
		installedApps = new ConcurrentHashMap<>();
		installedApps.put("1", "1.0.1");
		size = 100L;
	}
}
