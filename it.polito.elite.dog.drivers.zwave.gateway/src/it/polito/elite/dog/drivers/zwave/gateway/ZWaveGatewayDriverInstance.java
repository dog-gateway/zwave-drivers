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
package it.polito.elite.dog.drivers.zwave.gateway;

import it.polito.elite.dog.core.devicefactory.api.DeviceFactory;
import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceDescriptor;
import it.polito.elite.dog.core.library.model.DeviceDescriptorFactory;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.model.devicecategory.ZWaveGateway;
import it.polito.elite.dog.core.library.model.state.DeviceAssociationState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.statevalue.AssociatingStateValue;
import it.polito.elite.dog.core.library.model.statevalue.DisassociatingStateValue;
import it.polito.elite.dog.core.library.model.statevalue.IdleStateValue;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.ZWaveAPI;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Controller;
import it.polito.elite.dog.drivers.zwave.model.zway.json.DataConst;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.model.zway.json.DeviceData;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Instance;
import it.polito.elite.dog.drivers.zwave.network.ZWaveDriverInstance;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveInfo;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetwork;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetworkHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class ZWaveGatewayDriverInstance extends ZWaveDriverInstance
		implements ZWaveGateway
{
	// the driver logger
	LogHelper logger;

	// the log identifier, unique for the class
	public static String LOG_ID = "[ZWaveGatewayDriverInstance]: ";

	// data controller associated with the gateway
	protected Controller controller;

	// the current list of devices for which dynamic creation can be done
	private ConcurrentHashMap<String, String> supportedDevices;

	// the device factory reference
	private DeviceFactory deviceFactory;

	// the device descriptor factory reference
	private DeviceDescriptorFactory descriptorFactory;

	// last included device;
	private int lastIncludedDevice = -1;

	// last excluded device
	private int lastExcludedDevice = -1;

	// enable dynamic device detection
	private boolean detectionEnabled = false;

	// the time to wait before attempting automatic device detection
	private long waitBeforeDeviceInstall = 0;

	// the password to access the gateway, if any
	private String password;

	// the username to access the gateway, if any
	private String username;

	public ZWaveGatewayDriverInstance(ZWaveNetwork network,
			DeviceFactory deviceFactory, ControllableDevice controllableDevice,
			int nodeId, Set<Integer> instancesId, BundleContext context)
	{
		// gateway driver node contains always multiple instanceId, but only the
		// one with Id = 0 contains interesting data
		// also updateTimeMillis = 0 is fixed to zero because we are not
		// interested din this kind of behavior fot the gateway
		super(network, controllableDevice, nodeId, instancesId, null, nodeId, 0,
				context);

		// store the device factory reference
		this.deviceFactory = deviceFactory;

		// create a logger
		logger = new LogHelper(context);

		// create the device descriptor factory
		try
		{
			this.descriptorFactory = new DeviceDescriptorFactory(
					context.getBundle().getEntry("/deviceTemplates"));
		}
		catch (Exception e)
		{

			this.logger.log(LogService.LOG_ERROR,
					"Error while creating DeviceDescriptorFactory ", e);
		}

		// create a new device state (according to the current DogOnt model, no
		// state is actually associated to a Modbus gateway)
		currentState = new DeviceStatus(device.getDeviceId());

		// initialize device states
		this.initializeStates();
	}

	/**
	 * Updates the inner state accordingly to the given State instance and
	 * triggers a device state update
	 * 
	 * @param newState
	 *            The new State instance to update the inner state
	 */
	private boolean changeState(State newState)
	{
		// the state changed flag
		boolean stateChanged = false;

		// get the current state
		String currentStateValue = "";
		State state = currentState
				.getState(DeviceAssociationState.class.getSimpleName());

		if (state != null)
			currentStateValue = (String) state.getCurrentStateValue()[0]
					.getValue();

		// check that the state has changed
		if (!currentStateValue
				.equals(newState.getCurrentStateValue()[0].getValue()))
		{
			// update the current state
			this.currentState.setState(
					DeviceAssociationState.class.getSimpleName(), newState);

			// debug
			logger.log(LogService.LOG_DEBUG,
					ZWaveGatewayDriverInstance.LOG_ID + "Device "
							+ device.getDeviceId() + " is now "
							+ (newState).getCurrentStateValue()[0].getValue());

			// update the status
			this.updateStatus();

			// updated the state changed flag
			stateChanged = true;
		}

		return stateChanged;
	}

	/**
	 * starts inclusion process that lasts for 20 seconds
	 */
	@Override
	public void associate()
	{
		// start inclusion mode
		if (this.handler != null)
			this.handler.controllerWrite(ZWaveGatewayDriver.CMD_INCLUDE, "1");
	}

	/**
	 * starts exclusion process that lasts for 20 seconds
	 */
	@Override
	public void disassociate() // TODO: remove String nodeID
	{
		// start exclusion mode
		if (this.handler != null)
			this.handler.controllerWrite(ZWaveGatewayDriver.CMD_EXCLUDE, "1");
	}

	/**
	 * starts learn process that lasts for 20 seconds. Learn mode is equivalent
	 * to press the button on the device (not only gateway) to start include
	 * process. Due this means that the device is not yet included in the
	 * system, this method is left empty for future purposes
	 */
	// @Override TODO: add notation
	public void learn()
	{
		// Nothing to do...
	}

	/**
	 * reset Z-Wave controller to default. NB: will completely destroy all
	 * stored data about your network!
	 */
	// @Override TODO: add notation
	public void reset()
	{
		if (this.handler != null)
			this.handler.controllerWrite(ZWaveGatewayDriver.CMD_RESET, "");
	}

	@Override
	public synchronized DeviceStatus getState()
	{
		return this.currentState;
	}

	@Override
	protected void specificConfiguration()
	{
		// the ip address
		String ip = null;
		String port = null;
		// get the gateway address
		Set<String> ipAddressAsSet = this.device.getDeviceDescriptor()
				.getSimpleConfigurationParams().get(ZWaveInfo.IP_ADDRESS);
		// if not null, store it
		if ((ipAddressAsSet != null) && (!ipAddressAsSet.isEmpty()))
		{
			// it is a singleton, take the first element
			ip = ipAddressAsSet.iterator().next();
		}

		// get the gateway port
		Set<String> portAsSet = this.device.getDeviceDescriptor()
				.getSimpleConfigurationParams().get(ZWaveInfo.PORT);
		// if not null, store it
		if ((portAsSet != null) && (!portAsSet.isEmpty()))
		{
			// it is a singleton, take the first element
			port = portAsSet.iterator().next();
		}

		// get the gateway port
		Set<String> passwordAsSet = this.device.getDeviceDescriptor()
				.getSimpleConfigurationParams().get(ZWaveInfo.PASSWORD);
		// if not null, store it
		if ((passwordAsSet != null) && (!passwordAsSet.isEmpty()))
		{
			// it is a singleton, take the first element
			this.password = passwordAsSet.iterator().next();
		}

		// get the gateway port
		Set<String> usernameAsSet = this.device.getDeviceDescriptor()
				.getSimpleConfigurationParams().get(ZWaveInfo.USERNAME);
		// if not null, store it
		if ((usernameAsSet != null) && (!usernameAsSet.isEmpty()))
		{
			// it is a singleton, take the first element
			this.username = usernameAsSet.iterator().next();
		}

		if (ip != null)
		{
			if (port != null)
			{
				// store the gateway enpoint (ipaddress:port)
				this.gatewayEndpoint = ip + ":" + port;
			}
			else
			{
				// store the gateway enpoint (ipaddress:port)
				this.gatewayEndpoint = ip + ":" + ZWaveInfo.DEFAULT_PORT;
			}
			
			//update the gateway node info
			this.nodeInfo.setAdapterEndpoint(this.gatewayEndpoint);
		}

	}

	@Override
	protected ZWaveNetworkHandler addToNetworkDriver(ZWaveNodeInfo nodeInfo)
	{
		return network.addDriver(nodeInfo, 0, this, this.username, this.password);
	}

	@Override
	public void newMessageFromHouse(Device deviceNode, Instance instanceNode,
			Controller controllerNode, String sValue)
	{
		this.deviceNode = deviceNode;
		controller = controllerNode;

		/*-------------- HANDLE ASSOCIATION ------------------------*/
		// check if dynamic device detection is enabled
		if (detectionEnabled)
		{
			// check if any new device has been recently associated
			int lastIncludedDeviceAtController = controller.getData()
					.getLastIncludedDevice();
			if ((lastIncludedDeviceAtController != -1)
					// checks that the device is not the last included before
					&& (lastIncludedDeviceAtController != this.lastIncludedDevice)
					// checks that the device is not already included and
					// running
					&& (this.handler.getControllableDeviceURIFromNodeId(
							lastIncludedDeviceAtController) == null)
					// checks that there are supported devices
					&& (this.supportedDevices != null)
					&& (!this.supportedDevices.isEmpty()))
			{

				// get the device data
				Device newDeviceData = this.handler
						.getRawDevice(lastIncludedDeviceAtController);

				// build the device descriptor
				DeviceDescriptor descriptorToAdd = this.buildDeviceDescriptor(
						newDeviceData, lastIncludedDeviceAtController);

				// check not null
				if (descriptorToAdd != null)
				{
					// create the device
					// cross the finger
					this.deviceFactory.addNewDevice(descriptorToAdd);

					// only when the device has been created update the
					// last included device
					this.lastIncludedDevice = lastIncludedDeviceAtController;

					// disable dynamic detection until a new association is
					// detected
					this.detectionEnabled = false;

				}
			}
		}

		/*-------------- HANDLE DISASSOCIATION ------------------------*/

		// check if any new device has been recently disassociated
		int lastExcludedDeviceAtController = controller.getData()
				.getLastExcludedDevice();
		if ((lastExcludedDeviceAtController != -1)
				&& (lastExcludedDeviceAtController != this.lastExcludedDevice))
		{
			// update the last included device
			this.lastExcludedDevice = lastExcludedDeviceAtController;

			// get the device URI
			String deviceId = this.handler.getControllableDeviceURIFromNodeId(
					this.lastExcludedDevice);

			// remove the device (if not null)
			if ((deviceId != null) && (!deviceId.isEmpty()))
			{
				this.deviceFactory.removeDevice(deviceId);
			}

			// remove the device association
			// TODO: this should be done by the device driver, check how to
			this.handler.removeDriver(this.lastExcludedDevice);
		}

		/*-------------- HANDLE STATE ----------------------------*/
		if (this.currentState != null)
		{
			int controllerState = this.controller.getData()
					.getControllerState();

			// handle controller states
			switch (controllerState)
			{
				case 0: // idle
				{
					State currentAssociationState = this.currentState.getState(
							DeviceAssociationState.class.getSimpleName());
					if ((currentAssociationState != null)
							&& (currentAssociationState
									.getCurrentStateValue()[0].getClass()
											.getName()
											.equals(AssociatingStateValue.class
													.getName())))
					{
						// enable dynamic device detection
						this.detectionEnabled = true;
					}

					if (this.changeState(
							new DeviceAssociationState(new IdleStateValue())))
						// notify the current idle state
						this.notifyIdle();

					break;
				}
				case 1: // associating
				{
					if (this.changeState(new DeviceAssociationState(
							new AssociatingStateValue())))
						// notify the current associating state
						this.notifyAssociating();

					break;
				}
				case 5: // disassociating
				{
					if (this.changeState(new DeviceAssociationState(
							new DisassociatingStateValue())))
						// notify the current disassociating state
						this.notifyDisassociating();

					break;
				}
				default:
				{
					break;
				}
			}
		}
	}

	@Override
	protected boolean isController()
	{
		return true;
	}

	@Override
	protected ZWaveNodeInfo createNodeInfo(int deviceId,
			Set<Integer> instancesId, boolean isController)
	{
		HashMap<Integer, Set<Integer>> instanceCommand = new HashMap<Integer, Set<Integer>>();

		// this is the gateway so we are not really interested in command class
		// for sensor data update
		HashSet<Integer> ccSet = new HashSet<Integer>();
		ccSet.add(ZWaveAPI.COMMAND_CLASS_BASIC);

		for (Integer instanceId : instancesId)
		{
			instanceCommand.put(instanceId, ccSet);
		}
		ZWaveNodeInfo nodeInfo = new ZWaveNodeInfo(this.gatewayEndpoint,
				deviceId, instanceCommand, isController);

		return nodeInfo;
	}

	/**
	 * @return the supportedDevices
	 */
	public ConcurrentHashMap<String, String> getSupportedDevices()
	{
		return supportedDevices;
	}

	/**
	 * @param supportedDevices
	 *            the supportedDevices to set
	 */
	public void setSupportedDevices(
			ConcurrentHashMap<String, String> supportedDevices)
	{
		// simplest updated policy : replacement
		this.supportedDevices = supportedDevices;

		// debug
		this.logger.log(LogService.LOG_DEBUG,
				"Updated dynamic device creation db");
	}

	/**
	 * Gets the time to wait before automatic device detection, in milliseconds
	 * 
	 * @return
	 */
	public long getWaitBeforeDeviceInstall()
	{
		return waitBeforeDeviceInstall;
	}

	/**
	 * Sets the time to wait before automatic device detection, in milliseconds
	 * 
	 * @param waitBeforeDeviceInstall
	 */
	public void setWaitBeforeDeviceInstall(long waitBeforeDeviceInstall)
	{
		this.waitBeforeDeviceInstall = waitBeforeDeviceInstall;
	}

	// TODO: implement better.... just a trial
	private DeviceDescriptor buildDeviceDescriptor(Device device, int nodeId)
	{
		// the device descriptor to return
		DeviceDescriptor descriptor = null;

		if (this.descriptorFactory != null)
		{

			// get the new device data
			DeviceData deviceData = device.getData();

			// get the manufacturer id
			String manufacturerId = deviceData.getAllData()
					.get(DataConst.MANUFACTURER_ID).getValue().toString();
			String manufacturerProductType = deviceData.getAllData()
					.get(DataConst.MANUFACTURER_PRODUCT_TYPE).getValue()
					.toString();
			String manufacturerProductId = deviceData.getAllData()
					.get(DataConst.MANUFACTURER_PRODUCT_ID).getValue()
					.toString();

			// wait for instances to be read.... (may be read with a certain
			// variable delay)
			try
			{
				Thread.sleep(this.waitBeforeDeviceInstall);
			}
			catch (InterruptedException e1)
			{
				this.logger.log(LogService.LOG_WARNING,
						"Instance wait time was less than necessary due to interrupted thread, device instantiation might not be accurate.",
						e1);
			}

			// build the 4th id (number of instances)
			int numberOfInstances = this.handler.getRawDevice(nodeId).getInstances()
					.size();

			// build the device unique id
			String extendedDeviceUniqueId = manufacturerId + "-"
					+ manufacturerProductType + "-" + manufacturerProductId
					+ "-" + numberOfInstances;

			// build the device unique id
			String deviceUniqueId = manufacturerId + "-"
					+ manufacturerProductType + "-" + manufacturerProductId;

			// get the device class
			String deviceClass = this.supportedDevices
					.get(extendedDeviceUniqueId);

			// check if not extended
			if (deviceClass == null)
				deviceClass = this.supportedDevices.get(deviceUniqueId);

			// normal workflow...
			if ((deviceClass != null) && (!deviceClass.isEmpty()))
			{
				// create a descriptor definition map
				HashMap<String, Object> descriptorDefinitionData = new HashMap<String, Object>();

				// store the device name
				descriptorDefinitionData.put(DeviceDescriptorFactory.NAME,
						UUID.nameUUIDFromBytes(((this.gatewayEndpoint+"_"+deviceClass + "_" + nodeId)).getBytes()).toString());

				// store the device description
				descriptorDefinitionData.put(
						DeviceDescriptorFactory.DESCRIPTION,
						"New Device of type " + deviceClass);

				// store the device gateway
				descriptorDefinitionData.put(DeviceDescriptorFactory.GATEWAY,
						this.device.getDeviceId());

				// store the device location
				descriptorDefinitionData.put(DeviceDescriptorFactory.LOCATION,
						"");

				// store the node id
				descriptorDefinitionData.put("nodeId", "" + nodeId);

				// handle multiple instances
				String instanceIds[] = new String[device.getInstances().size()];

				// mine instances
				int i = 0;
				for (Integer instanceId : device.getInstances().keySet())
				{
					instanceIds[i] = instanceId.toString();
					i++;
				}

				// store mined instances
				descriptorDefinitionData.put("instanceIds", instanceIds);

				// get the device descriptor
				try
				{
					descriptor = this.descriptorFactory.getDescriptor(
							descriptorDefinitionData, deviceClass);
				}
				catch (Exception e)
				{
					this.logger.log(LogService.LOG_ERROR,
							"Error while creating DeviceDescriptor for the just added device ",
							e);
				}

				// debug dump
				this.logger.log(LogService.LOG_INFO,
						"Detected new device: \n\tdeviceUniqueId: "
								+ deviceUniqueId + "\n\tnodeId: " + nodeId
								+ "\n\tdeviceClass: " + deviceClass);
			}
		}

		// return
		return descriptor;
	}

	/**
	 * Initializes the state asynchronously as required by OSGi
	 */
	private void initializeStates()
	{
		// initialize the state
		this.currentState.setState(DeviceAssociationState.class.getSimpleName(),
				new DeviceAssociationState(new IdleStateValue()));

		// get the initial state of the device
		Runnable worker = new Runnable()
		{
			public void run()
			{
				if (handler != null)
					handler.read(nodeInfo, true);
			}
		};

		Thread workerThread = new Thread(worker);
		workerThread.start();

	}

	@Override
	public void notifyAssociating()
	{
		((ZWaveGateway) this.device).notifyAssociating();
	}

	@Override
	public void notifyDisassociating()
	{
		((ZWaveGateway) this.device).notifyDisassociating();
	}

	@Override
	public void notifyIdle()
	{
		((ZWaveGateway) this.device).notifyIdle();
	}

	@Override
	public void updateStatus()
	{
		// update the monitor admin status snapshot
		((Controllable) this.device).updateStatus();
	}

	public String getGatewayEndpoint()
	{
		return this.gatewayEndpoint;
	}

	/**
	 * @return the password
	 */
	public String getPassword()
	{
		return password;
	}

	/**
	 * @return the username
	 */
	public String getUsername()
	{
		return username;
	}

}
