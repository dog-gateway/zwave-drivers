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
package it.polito.elite.dog.drivers.zwave.network;

import it.polito.elite.dog.core.library.model.CNParameters;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.StatefulDevice;
import it.polito.elite.dog.core.library.util.ElementDescription;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Controller;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Instance;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveInfo;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetwork;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetworkHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;

public abstract class ZWaveDriverInstance implements StatefulDevice
{
	// a reference to the network driver interface to allow network-level access
	// for sub-classes
	// TODO: check if at the end of multi-gateway support development is still
	// needed
	protected ZWaveNetwork network;

	// a reference to the actual handler managing the adapter to which the
	// device is connected
	protected ZWaveNetworkHandler handler;

	// the state of the device associated to this driver
	protected DeviceStatus currentState;

	// the device associated to the driver
	protected ControllableDevice device;

	// the endpoint address associated to this device by means of the gateway
	// attribute
	protected int gatewayNodeId;

	// the URL of the endpoint at which the gateway is reachable
	protected String gatewayEndpoint;

	// the unique identifier associated with this device
	protected ZWaveNodeInfo nodeInfo;

	// link to the node representing the device in the json tree from z-way.
	// This is shared between different instances of the same device.
	// If only one instance is present, instanceId = 0
	protected Device deviceNode;

	// zway data associated with this instance
	// protected Instance instanceNode;

	// // the port of the endpoint address
	// protected String gwPort;

	// the datapoints managed by this driver
	// protected Set<ModbusRegisterInfo> managedRegisters;

	// the datapoint to notifications map
	protected Map<ZWaveNodeInfo, Set<CNParameters>> register2Notification;

	// the command2datapoint map
	protected Map<CNParameters, ZWaveNodeInfo> command2Register;

	// milliseconds between two forced update of the device status.
	// Every T, network driver will perform a GET on the device. This will
	// cause a full refresh of the json data for this device. In this way
	// we can avoid the issue caused by some device that doesn't update
	// the right part of the json tree.
	protected int updateTimeMillis;

	// UNIX timestamp of the last communication from the device. Its value comes
	// from Z-Way server
	// and represents the real time of when a value is received. This is due
	// because Z-Way server
	// always answer to a Get with last known value, but it can be old and wrong
	// (for example: device in sleep status, device dead or unavailable).
	// Init as 0. In this way we will probably receive a wrong value on first
	// read,
	// but we can avoid misaligned clock on Z-Way server and current platform.
	// This can be not used by autotrigger device. i.e. door/motion sensor
	protected long lastUpdateTime = 0;

	// counter used to manage warning in log due to no response from device
	// after a Get
	protected int nFailedUpdate = 0;

	/**
	 * The base class constructor, provides common initialization for all the
	 * needed data structures, must be called by sub-class constructors
	 * 
	 * @param network
	 *            the network driver to use (as described by the
	 *            {@link ModbusNetwork} interface.
	 * @param device
	 *            the device to which this driver is attached/associated //TODO
	 */
	public ZWaveDriverInstance(ZWaveNetwork network, ControllableDevice device,
			int deviceId, Set<Integer> instancesId, String gatewayEndpoint,
			int gatewayNodeId, int updateTimeMillis, BundleContext context)
	{
		// store a reference to the network driver
		this.network = network;

		// store a reference to the associate device
		this.device = device;

		// store the nodeid associated to the adapter
		this.gatewayNodeId = gatewayNodeId;

		// store the endpoint address for the gateway to which connects the
		// attached device
		this.gatewayEndpoint = gatewayEndpoint;

		// store update time
		this.updateTimeMillis = updateTimeMillis;

		// store the id of the device
		// nodeInfo = new ZWaveNodeInfo(deviceId, instancesId, isController());

		// Create the nodeInfo object. It defines nodeId, instancesId and Get
		// command class
		nodeInfo = createNodeInfo(deviceId, instancesId, isController());

		// create the map needed to associate datapoints to notifications
		register2Notification = new ConcurrentHashMap<ZWaveNodeInfo, Set<CNParameters>>();

		// create the map to associate commands and datapoints
		command2Register = new ConcurrentHashMap<CNParameters, ZWaveNodeInfo>();

		//
		// // create the set for storing the managed datapoints
		// this.managedRegisters = new HashSet<ModbusRegisterInfo>();

		// fill the data structures depending on the specific device
		// configuration parameters
		fillConfiguration();

		// call the specific configuration method, if needed
		specificConfiguration();

		// associate the device-specific driver to the network driver
		// for (ModbusRegisterInfo register : this.managedRegisters)
		this.handler = addToNetworkDriver(nodeInfo);
	}

	/***
	 * Fills the inner data structures depending on the specific device
	 * configuration parameters, extracted from the device instance associated
	 * to this driver instance
	 */
	private void fillConfiguration()
	{
		// get parameters associated to each device command (if any)
		Set<ElementDescription> commandsSpecificParameters = device
				.getDeviceDescriptor().getCommandSpecificParams();

		// get parameters associated to each device notification (if any)
		Set<ElementDescription> notificationsSpecificParameters = device
				.getDeviceDescriptor().getNotificationSpecificParams();

		// --------------- Handle command specific parameters ----------------
		for (ElementDescription parameter : commandsSpecificParameters)
		{
			// the parameter map
			Map<String, String> params = parameter.getElementParams();

			// get the real command name
			String realCommandName = params.get(ZWaveInfo.COMMAND_NAME);

			CNParameters cmdInfo = new CNParameters(realCommandName,
					parameter.getElementParams());
			// add the command to data point entry
			command2Register.put(cmdInfo, nodeInfo);
		}

		// --------------- Handle notification specific parameters
		// ----------------
		for (ElementDescription parameter : notificationsSpecificParameters)
		{
			// the parameter map
			Map<String, String> params = parameter.getElementParams();

			// get the real command name
			String notificationName = params.get(ZWaveInfo.NOTIFICATION_NAME);

			// fill the data point to notification map, if the data point
			// has
			// never been registered create a new entry in the map.
			Set<CNParameters> notificationNames = register2Notification
					.get(nodeInfo);

			if (notificationNames == null)
			{
				notificationNames = new HashSet<CNParameters>();
				register2Notification.put(nodeInfo, notificationNames);
			}
			// add the notification name to the set associated to the dp
			// datapoint
			CNParameters nInfo = new CNParameters(notificationName,
					parameter.getElementParams());
			notificationNames.add(nInfo);
		}
	}

	/*
	 * /** Update the data structure associated with this device from the
	 * network
	 * 
	 * public void updateDeviceData() { network.read(getNodeInfo()); }
	 */

	/**
	 * @return the gwNodeId
	 */
	public int getGatewayNodeId()
	{
		return gatewayNodeId;
	}

	/**
	 * @return the gatewayEndpoint
	 */
	public String getGatewayEndpoint()
	{
		return gatewayEndpoint;
	}

	@Override
	public DeviceStatus getState()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public ZWaveNodeInfo getNodeInfo()
	{
		return nodeInfo;
	}

	public void setNodeInfo(ZWaveNodeInfo nodeInfo)
	{
		this.nodeInfo = nodeInfo;
	}

	/**
	 * Get the device currently attached to this driver instance
	 * 
	 * @return the device
	 */
	public ControllableDevice getDevice()
	{
		return device;
	}

	/**
	 * Notifies a device-specific driver of new data coming from the underlying
	 * zwave network connection. The updated value is contained in the given
	 * {@link IDevice} instance.
	 * 
	 * @param deviceNode
	 *            the {@link Device} instance representing the driver node.
	 * @param instanceNode
	 *            the {@link Instance} instance representing the instance node.
	 * @param controllerNode
	 *            the {@link Controller} instance representing the controller
	 *            node.
	 * @param string
	 */
	public abstract void newMessageFromHouse(Device deviceNode,
			Instance instanceNode, Controller controllerNode, String sValue);

	/**
	 * Extending classes might implement this method to provide driver-specific
	 * configurations to be done during the driver creation process, before
	 * associating the device-specific driver to the network driver
	 */
	protected abstract void specificConfiguration();

	/**
	 * Adds a device managed by a device-specific driver instance to the
	 * {@link ZWaveNetwork} driver. It must be implemented by extending classes
	 * and it must take care of identifying any additional information needed to
	 * correctly specify the given register and to associate the corresponding
	 * sNodeId with the proper {@link ZWaveDriverImpl} instance.
	 * 
	 * @param sNodeId
	 *            the node to add.
	 * @return 
	 */
	protected abstract ZWaveNetworkHandler addToNetworkDriver(ZWaveNodeInfo nodeInfo);

	/**
	 * Override and return true if it is the controller, false otherwise
	 * 
	 * @return
	 */
	protected abstract boolean isController();

	/**
	 * Extending classes might implement this method to define the
	 * {@link ZWaveNodeInfo} associated with the driver
	 * 
	 * @param deviceId
	 * @param instancesId
	 * @param isController
	 * @return {@link ZWaveNodeInfo}
	 */
	protected abstract ZWaveNodeInfo createNodeInfo(int deviceId,
			Set<Integer> instancesId, boolean isController);
}
