/*
 * Dog  - Z-Wave
 * 
 * Copyright 2013 Davide Aimone  and Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.dog.drivers.zwave.network.info;

import it.polito.elite.dog.core.library.model.ConfigurationConstants;

public class ZWaveInfo extends ConfigurationConstants
{
	// the manufacturer identifier (ZWave)
	public static final String MANUFACTURER = "ZWave";	
	
	// the node ID
	public static final String NODE_ID = "nodeId";
	
	// the instance ID
	public static final String INSTANCE_ID = "instanceId";
	
	// the gateway IPAddress
	public static final String IP_ADDRESS = "IPAddress";
	
	// the gateway port
	public static final  String PORT = "port";
	
	// the gateway username, if any
	public static final String USERNAME = "username";
	
	// the gateway password, if any
	public static final String PASSWORD = "password";
	
	// constant for configuration
	public static final String PROPERTY_UPDATETIME = "updateTimeMillis";
	
	// the persistent store configuration property
	public static final String PROPERTY_PERSISTENT_STORE = "persistentStore";
	
	// the persistent store configuration property
	public static final String PROPERTY_STEP_PERCENT = "stepAsPercentage";

	public static final String DEFAULT_PORT = "8083";

	//the controller node id
	public static final int CONTROLLER_NODE_ID = 1;
	
}
