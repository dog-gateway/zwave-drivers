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
package it.polito.elite.dog.drivers.zwave.network.interfaces;

import it.polito.elite.dog.drivers.zwave.network.ZWaveDriverInstance;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;

public interface ZWaveNetwork
{
	/**
	 * Adds a new device-specific driver for the node id
	 * 
	 * @param nodeInfo
	 *            the {@link ZWaveNodeInfo} unique identifier.
	 * @param updateTimeMillis
	 *            milliseconds between two forced get on device sensor. 0 if it
	 *            is not requested
	 * @param driver
	 *            the {@link ZWaveDriverInstance} instance to add.
	 */
	public ZWaveNetworkHandler addDriver(ZWaveNodeInfo nodeInfo,
			int updateTimeMillis, ZWaveDriverInstance driver);

	/**
	 * Adds a new device-specific driver for the node id, used for gateways
	 * where username and password are also needed
	 * 
	 * @param nodeInfo
	 *            the {@link ZWaveNodeInfo} unique identifier.
	 * @param updateTimeMillis
	 *            milliseconds between two forced get on device sensor. 0 if it
	 *            is not requested
	 * @param driver
	 *            the {@link ZWaveDriverInstance} instance to add
	 * @param username
	 *            the username associated to the gateway device
	 * @param password
	 *            The password associated to the gateway device
	 * @return
	 */
	public ZWaveNetworkHandler addDriver(ZWaveNodeInfo nodeInfo,
			int updateTimeMillis, ZWaveDriverInstance driver, String username,
			String password);

	/**
	 * Removes a device-specific driver for the given register
	 * 
	 * @param nodeInfo
	 *            the unique identifier.
	 */
	void removeDriver(ZWaveNodeInfo nodeInfo);

	/**
	 * Removes the driver-register associations for the given driver. To be
	 * called when a specific driver disconnects
	 * 
	 * @param datapoint
	 */
	void removeDriver(ZWaveDriverInstance driver);

	/**
	 * Returns the network-level polling time (base time upon which all
	 * device-specific polling times are based).
	 * 
	 * @return
	 */
	public long getPollingTimeMillis();

	/**
	 * Returns the network handler actually managing communication with the
	 * physical gateway located at the given endpoint, if any.
	 * 
	 * N.B. for future use... TODO evaluate if actually needed or not
	 * 
	 * @param adapterEndpoint
	 * @return The associated ZWaveHandler or null if it does not exists.
	 */
	public ZWaveNetworkHandler getHandler(String adapterEndpoint);

	;
}
