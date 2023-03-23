package qwertzite.extraexplosions.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qwertzite.extraexplosions.ExtraExplosionCore;

public class ModLog {
	private static final Logger LOGGER = logInit();
	
	private static final Logger logInit() {
		String name = ExtraExplosionCore.LOG_BASE_NAME;
		return LogManager.getFormatterLogger(name);
	}
	
	/**
	 * Returns a {@link Logger} for current side.
	 *
	 * @return a logger instance
	 */
	private static Logger getLogger() {
		return LOGGER;
	}
	
	public static void log(Level level, String message, Throwable throwable) {
		Logger logger = getLogger();
		logger.log(level, message, throwable);
	}
	
	public static void log(Level level, String message, Object...params) {
		Logger logger = getLogger();
		logger.log(level, message, params);
	}
	
	public static void log(Level level, String message) {
		Logger logger = getLogger();
		logger.log(level, message);
	}
	
	public static void error(String message, Throwable throwable) {
		ModLog.log(Level.ERROR, message, throwable);
	}
	
	public static void warn(String message, Throwable throwable) {
		ModLog.log(Level.WARN, message, throwable);
	}
	
	public static void warn(String message, Object...params) {
		ModLog.log(Level.WARN, message, params);
	}
	
	public static void warn(String message) {
		ModLog.log(Level.WARN, message);
	}
	
	public static void info(String message, Object...params) {
		ModLog.log(Level.INFO, message, params);
	}
	
	public static void info(String message) {
		ModLog.log(Level.INFO, message);
	}
	
	public static void debug(String message, Object...objects) {
		ModLog.log(Level.DEBUG, message, objects);
	}
	
	public static void debug(String message) {
		ModLog.log(Level.DEBUG, message);
	}
	
	public static void trace(String message) {
		ModLog.log(Level.TRACE, message);
	}
}
