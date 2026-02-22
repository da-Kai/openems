package io.openems.common.logger;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

/**
 * A Logger implementation, that prefixes all log messages with a given context.
 */
public abstract class PrefixedLogger implements Logger {
	
	protected final Logger logger;
	
	protected PrefixedLogger(Class<?> clazz) {
		this.logger = LoggerFactory.getLogger(Objects.requireNonNull(clazz));
	}
	
	protected PrefixedLogger(String name) {
		this.logger = LoggerFactory.getLogger(Objects.requireNonNull(name));
	}
	
	protected abstract String prefix(String format);
	
	@Override
	public String getName() {
		return this.logger.getName();
	}

	@Override
	public void debug(String msg) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(prefix(msg));
		}
	}

	@Override
	public void debug(String format, Object arg) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(prefix(format), arg);
		}
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(prefix(format), arguments);
		}
	}
	
	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(prefix(format), arg1, arg2);
		}
	}

	@Override
	public void debug(String msg, Throwable t) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(prefix(msg), t);
		}
	}

	@Override
	public void debug(Marker marker, String msg) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(marker, prefix(msg));
		}
	}	

	@Override
	public void debug(Marker marker, String format, Object arg) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(marker, prefix(format), arg);
		}
	}

	@Override
	public void debug(Marker marker, String format, Object... arguments) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(marker, prefix(format), arguments);
		}
	}

	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(marker, prefix(format), arg1, arg2);
		}
	}
	
	@Override
	public void debug(Marker marker, String msg, Throwable t) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug(marker, prefix(msg), t);
		}
	}

	@Override
	public void error(String msg) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(prefix(msg));
		}
	}

	@Override
	public void error(String format, Object arg) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(prefix(format), arg);
		}
	}

	@Override
	public void error(String format, Object... arguments) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(prefix(format), arguments);
		}
	}
	
	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(prefix(format), arg1, arg2);
		}
	}

	@Override
	public void error(String msg, Throwable t) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(prefix(msg), t);
		}
	}

	@Override
	public void error(Marker marker, String msg) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(marker, prefix(msg));
		}
	}	

	@Override
	public void error(Marker marker, String format, Object arg) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(marker, prefix(format), arg);
		}
	}

	@Override
	public void error(Marker marker, String format, Object... arguments) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(marker, prefix(format), arguments);
		}
	}

	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(marker, prefix(format), arg1, arg2);
		}
	}
	
	@Override
	public void error(Marker marker, String msg, Throwable t) {
		if (this.logger.isErrorEnabled()) {
			this.logger.error(marker, prefix(msg), t);
		}
	}
	
	@Override
	public void info(String msg) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(prefix(msg));
		}
	}

	@Override
	public void info(String format, Object arg) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(prefix(format), arg);
		}
	}

	@Override
	public void info(String format, Object... arguments) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(prefix(format), arguments);
		}
	}
	
	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(prefix(format), arg1, arg2);
		}
	}

	@Override
	public void info(String msg, Throwable t) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(prefix(msg), t);
		}
	}

	@Override
	public void info(Marker marker, String msg) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(marker, prefix(msg));
		}
	}	

	@Override
	public void info(Marker marker, String format, Object arg) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(marker, prefix(format), arg);
		}
	}

	@Override
	public void info(Marker marker, String format, Object... arguments) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(marker, prefix(format), arguments);
		}
	}

	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(marker, prefix(format), arg1, arg2);
		}
	}
	
	@Override
	public void info(Marker marker, String msg, Throwable t) {
		if (this.logger.isInfoEnabled()) {
			this.logger.info(marker, prefix(msg), t);
		}
	}
	
	@Override
	public void trace(String msg) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(prefix(msg));
		}
	}

	@Override
	public void trace(String format, Object arg) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(prefix(format), arg);
		}
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(prefix(format), arguments);
		}
	}
	
	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(prefix(format), arg1, arg2);
		}
	}

	@Override
	public void trace(String msg, Throwable t) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(prefix(msg), t);
		}
	}

	@Override
	public void trace(Marker marker, String msg) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(marker, prefix(msg));
		}
	}	

	@Override
	public void trace(Marker marker, String format, Object arg) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(marker, prefix(format), arg);
		}
	}

	@Override
	public void trace(Marker marker, String format, Object... arguments) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(marker, prefix(format), arguments);
		}
	}

	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(marker, prefix(format), arg1, arg2);
		}
	}
	
	@Override
	public void trace(Marker marker, String msg, Throwable t) {
		if (this.logger.isTraceEnabled()) {
			this.logger.trace(marker, prefix(msg), t);
		}
	}
	
	@Override
	public void warn(String msg) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(prefix(msg));
		}
	}

	@Override
	public void warn(String format, Object arg) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(prefix(format), arg);
		}
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(prefix(format), arguments);
		}
	}
	
	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(prefix(format), arg1, arg2);
		}
	}

	@Override
	public void warn(String msg, Throwable t) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(prefix(msg), t);
		}
	}

	@Override
	public void warn(Marker marker, String msg) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(marker, prefix(msg));
		}
	}	

	@Override
	public void warn(Marker marker, String format, Object arg) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(marker, prefix(format), arg);
		}
	}

	@Override
	public void warn(Marker marker, String format, Object... arguments) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(marker, prefix(format), arguments);
		}
	}

	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(marker, prefix(format), arg1, arg2);
		}
	}
	
	@Override
	public void warn(Marker marker, String msg, Throwable t) {
		if (this.logger.isWarnEnabled()) {
			this.logger.warn(marker, prefix(msg), t);
		}
	}
	
	@Override
	public boolean isDebugEnabled() {
		return this.logger.isDebugEnabled();
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return this.logger.isDebugEnabled(marker);
	}

	@Override
	public boolean isErrorEnabled() {
		return this.logger.isErrorEnabled();
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return this.logger.isErrorEnabled(marker);
	}

	@Override
	public boolean isInfoEnabled() {
		return this.logger.isInfoEnabled();
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return this.logger.isInfoEnabled(marker);
	}

	@Override
	public boolean isTraceEnabled() {
		return this.logger.isTraceEnabled();
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return this.logger.isTraceEnabled(marker);
	}

	@Override
	public boolean isWarnEnabled() {
		return this.logger.isWarnEnabled();
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return this.logger.isWarnEnabled(marker);
	}
	
}
