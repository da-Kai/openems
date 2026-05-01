package io.openems.backend.uiwebsocket.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Ui.Websocket", //
		description = "Configures the websocket server for OpenEMS UI")
@interface Config {

	@AttributeDefinition(name = "Port", description = "The port of the websocket server.")
	int port() default 8082;

	@AttributeDefinition(name = "Number of Threads", description = "Pool-Size: the number of threads dedicated to handle the tasks")
	int poolSize() default 10;

	@AttributeDefinition(name = "Request Limit", description = "Limit of Requests per second, before they get discarded by the Limiter")
	int requestLimit() default 20;

	@AttributeDefinition(name = "Max Connections", description = "Maximum number of concurrent WebSocket connections. 0 disables the limit.")
	int maxConnections() default 1000;

	String webconsole_configurationFactory_nameHint() default "Ui Websocket";
}
