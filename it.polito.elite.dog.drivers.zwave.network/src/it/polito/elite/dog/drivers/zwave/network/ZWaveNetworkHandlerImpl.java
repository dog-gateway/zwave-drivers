/**
 * 
 */
package it.polito.elite.dog.drivers.zwave.network;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.Version;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.ZWaveAPI;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Controller;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Instance;
import it.polito.elite.dog.drivers.zwave.model.zway.json.ZWaveModelTree;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveInfo;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetworkHandler;
import it.polito.elite.dog.drivers.zwave.util.ConnectionManager;

/**
 * This shall handle a single ZWave network connected to a well defined gateway
 * there are several ways to actually implement this handler, but probably the
 * quickest/more efficient one would be to move here the core logic of the
 * former driver implementation. With required adjustments.
 * 
 * @author bonino
 *
 */
public class ZWaveNetworkHandlerImpl implements ZWaveNetworkHandler
{
	// the default polling time
	public static final int DEFAULT_POLLING_TIME_MILLIS = 5000;
	
	// the URL of the pseudo-REST end point of the Z-Way server
	private String gatewayEndpointURL;

	// the username with which connecting
	private String username;

	// the password with which connecting
	private String password;

	// the LogHelper needed to log events
	private LogHelper logger;

	// connection manager used to deal with a specific z-way server, associated
	// with the given url.
	private ConnectionManager conManager;

	// model tree representing the system
	private ZWaveModelTree modelTree;

	// the register to driver map
	private ConcurrentHashMap<ZWaveNodeInfo, ZWaveDriverInstance> nodeInfo2Driver;

	// the inverse map
	// Please notice that on the basis of the driver instance definition a
	// single ZWaveDriverInstance will never handle more than one z-wave node.
	private ConcurrentHashMap<ZWaveDriverInstance, ZWaveNodeInfo> driver2NodeInfo;

	// the zwave poller
	private ZWavePoller poller;

	// the baseline pollingTime adopted if no server-specific setting is given
	private int pollingTimeMillis = ZWaveNetworkHandlerImpl.DEFAULT_POLLING_TIME_MILLIS; 

	private Version version;

	/**
	 * 
	 */
	public ZWaveNetworkHandlerImpl(String gatewayEndpointURL, String username,
			String password, int pollingTimeMillis, LogHelper logger)
	{
		// TODO add checks for needed values
		// store the instance variables
		this.gatewayEndpointURL = gatewayEndpointURL; // gateway url
		this.username = username; // the username to access the gateway
		this.password = password; // the password to access the gateway
		this.pollingTimeMillis = pollingTimeMillis; // the polling time to use
		this.logger = logger; // the logger to provide information when needed

		/*--- Bi-directional hash map, naive implementation ----*/

		// create the node info to driver map
		nodeInfo2Driver = new ConcurrentHashMap<ZWaveNodeInfo, ZWaveDriverInstance>();

		// create the driver to node info map
		driver2NodeInfo = new ConcurrentHashMap<ZWaveDriverInstance, ZWaveNodeInfo>();

		/*---- Physical connection handler ----*/

		// Create a connection manager and try to connect to the server
		conManager = new ConnectionManager(this.gatewayEndpointURL,
				this.username, this.password, this.logger);

		// do not start the Poller if already existing
		if (this.poller == null)
		{
			// in any case, as the polling time has a default, init the
			// poller
			// thread and start it
			poller = new ZWavePoller(this);

			// start the poller
			poller.start();

			// log the driver start
			logger.log(LogService.LOG_INFO, ZWaveDriverImpl.LOG_ID
					+ "Started the driver poller thread...");
		}

	}

	/**
	 * Provides a reference to the {@link LogService} instance used by this
	 * class to log messages...
	 * 
	 * @return
	 */
	public LogHelper getLogger()
	{
		return this.logger;
	}

	/**
	 * Provides the polling time to be used by Poller threads connect to this
	 * driver instance...
	 * 
	 * @return
	 */
	public long getPollingTimeMillis()
	{
		return this.pollingTimeMillis;
	}

	/**
	 * Read a single node info
	 * 
	 * @param nodeInfo
	 * @param bRequery
	 */
	public void read(ZWaveNodeInfo nodeInfo, boolean bRequery)
	{
		if (conManager != null)
		{
			try
			{
				// if needed update tree model
				if (bRequery || modelTree == null)
					modelTree = conManager.updateDevices();

				Device deviceNode = null;
				Instance instanceNode = null;
				Controller controllerNode = null;

				// if node is the controller (gateway) we have to put also
				// controller data.
				if (nodeInfo.isController())
					controllerNode = modelTree.getController();

				deviceNode = modelTree.getDevices()
						.get(nodeInfo.getDeviceNodeId());
				Device device = modelTree.getDevices()
						.get(nodeInfo.getDeviceNodeId());
				// device can be null if house xml configuration is wrong
				if (device != null)
				{
					for (Integer instanceId : nodeInfo.getInstanceSet())
					{
						instanceNode = device.getInstances().get(instanceId);

						// instance can be null if house xml configuration is
						// wrong
						if (instanceNode != null)
						{
							ZWaveDriverInstance driver = nodeInfo2Driver
									.get(nodeInfo);
							driver.newMessageFromHouse(deviceNode, instanceNode,
									controllerNode, null);
						}
						else
						{
							logger.log(LogService.LOG_ERROR,
									"Device id: " + nodeInfo.getDeviceNodeId()
											+ " instance id: " + instanceId
											+ " does not exists!");
							
							// here auto discovery takes place:
							if(controllerNode==null)
							{
								//get the controller device and trigger auto-discovery
							}
						}
					}
				}
				else
				{
					logger.log(LogService.LOG_ERROR, "Device id: "
							+ nodeInfo.getDeviceNodeId() + " does not exists!");
				}
			}
			catch (Exception e)
			{
				logger.log(LogService.LOG_ERROR, "Error: ", e);

			}
		}
	}

	public void readAll(boolean bRequery)
	{
		if (conManager != null)
		{
			try
			{
				// if needed update tree model
				if (bRequery || modelTree == null)
					modelTree = conManager.updateDevices();

				// if version still not available
				if (this.version == null)
				{
					String versionAsString = (String) this.modelTree
							.getController().getData().getAllData()
							.get("softwareRevisionVersion").getValue();
					versionAsString = versionAsString.trim().substring(1);
					this.version = new Version(versionAsString);
					logger.log(LogService.LOG_INFO,
							"ZWay version:" + this.version);
				}

				for (ZWaveNodeInfo nodeInfo : driver2NodeInfo.values())
				{
					read(nodeInfo, false);
					// yield to other processes
					Thread.yield();
				}
			}
			catch (Exception e)
			{
				logger.log(LogService.LOG_ERROR,
						"Unable to update devices due to exception:", e);

			}
		}
	}

	public void updateSensor(ZWaveNodeInfo nodeInfo)
	{
		// check if the node info is registered...
		if (this.nodeInfo2Driver.containsKey(nodeInfo))
		{
			// send one update command per each command class
			for (Entry<Integer, Set<Integer>> instanceCC : nodeInfo
					.getInstanceSensorCC().entrySet())
			{
				for (Integer ccToTrigger : instanceCC.getValue())
				{
					try
					{
						// check if the model still contains the device: in some
						// cases device removal at the z-wave network-level may
						// happen before than the time in which the gateway
						// driver detects it.
						if (modelTree.getDevices()
								.containsKey(nodeInfo.getDeviceNodeId()))
						{
							conManager.sendCommand("devices["
									+ nodeInfo.getDeviceNodeId()
									+ "].instances[" + instanceCC.getKey()
									+ "].commandClasses[" + ccToTrigger
									+ "].Get()");
						}
					}
					catch (Exception e)
					{
						logger.log(LogService.LOG_ERROR, "Can't send command",
								e);

					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #addDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveNodeInfo)
	 */
	public void addDriver(ZWaveNodeInfo nodeInfo, int updateTimeMillis,
			ZWaveDriverInstance driver)
	{
		// get the register gateway address
		int deviceNodeId = nodeInfo.getDeviceNodeId();
		Set<Integer> lstInstanceNodeId = nodeInfo.getInstanceSet();

		// info on port usage
		logger.log(LogService.LOG_INFO,"Using deviceId: " + deviceNodeId + " instancesId: "
						+ lstInstanceNodeId.toString());

		// adds a given register-driver association
		nodeInfo2Driver.put(nodeInfo, driver);

		// fills the reverse map
		ZWaveNodeInfo registeredNodeInfo = driver2NodeInfo.get(driver);
		if (registeredNodeInfo == null)
		{
			// create the new entry associated to the driver
			driver2NodeInfo.put(driver, nodeInfo);
		}

		// add a new device to the thread that is the responsible for device
		// update
		poller.addDeviceToQueue(nodeInfo, updateTimeMillis);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #removeDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveNodeInfo)
	 */

	public void removeDriver(ZWaveNodeInfo nodeInfo)
	{
		// removes a given register-driver association
		ZWaveDriverInstance drv = nodeInfo2Driver.remove(nodeInfo);

		// if actually a driver was registered for the given nodeInfo (local to
		// the handled gateway), remove it
		if (drv != null)
		{
			driver2NodeInfo.remove(drv);
		}
	}

	public void removeDriver(int nodeId)
	{
		ZWaveNodeInfo infoToRemove = null;
		// look for nodeinfos
		for (ZWaveNodeInfo info : this.nodeInfo2Driver.keySet())
		{
			// compare the node id, if they are equal, get the driver and ask
			// for the device uri
			if (info.getDeviceNodeId() == nodeId)
			{
				infoToRemove = info;
				break;
			}
		}

		if (infoToRemove != null)
		{
			this.poller.removeDeviceFromQueue(nodeId);
			this.removeDriver(infoToRemove);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #removeDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveDriver)
	 */

	public void removeDriver(ZWaveDriverInstance driver)
	{
		// removes a given driver-register association
		ZWaveNodeInfo driverNodeInfo = driver2NodeInfo.remove(driver);

		// remove the node info to driver associations
		if (driverNodeInfo != null)
		{
			// remove the datapoint-to-driver associations
			nodeInfo2Driver.remove(driverNodeInfo);
		}
	}

	public void write(int deviceId, int instanceId, int nCommandClass,
			String commandValue)
	{
		try
		{
			// version > 1.3.1 patch
			if (this.version.compareTo(new Version("1.3.1")) > 0)
			{
				if (nCommandClass == ZWaveAPI.GENERIC_TYPE_SWITCH_BINARY)
				{
					if (commandValue.equals("255"))
						commandValue = "true";
					else
						commandValue = "false";
				}
			}

			conManager.sendCommand("devices[" + deviceId + "].instances["
					+ instanceId + "].commandClasses[" + nCommandClass
					+ "].Set(" + commandValue + ")");
		}
		catch (Exception e)
		{
			logger.log(LogService.LOG_ERROR, "Can't send command", e);

		}

	}

	public void controllerWrite(String sCommand, String commandValue)
	{
		try
		{
			conManager.sendCommand(sCommand + "(" + commandValue + ")");
		}
		catch (Exception e)
		{
			logger.log(LogService.LOG_ERROR, "Can't send command", e);

		}
	}
	
	/**
	 * Get all devices currently available on the network, including devices
	 * that still have to be configured in Dog
	 */
	public Map<Integer, Device> getRawDevices()
	{
		return this.modelTree.getDevices();
	}

	/**
	 * Get raw device data on the basis of the given nodeId
	 * 
	 * @return
	 */
	public Device getRawDevice(int nodeId)
	{
		return this.modelTree.getDevices().get(new Integer(nodeId));
	}

	/**
	 * checks among currently handled device whether the given node id exists,
	 * and in the case, extracts the corresponding device URI
	 * 
	 * @param nodeId
	 * @return The URI of the corresponding {@link ControllableDevice}
	 */
	public String getControllableDeviceURIFromNodeId(int nodeId)
	{
		String deviceId = null;

		// look for nodeinfos
		for (ZWaveNodeInfo info : this.nodeInfo2Driver.keySet())
		{
			// compare the node id, if they are equal, get the driver and ask
			// for the device uri
			if (info.getDeviceNodeId() == nodeId)
			{
				// the driver currently connected to the device
				ZWaveDriverInstance driver = this.nodeInfo2Driver.get(info);

				// get the device uri
				deviceId = driver.getDevice().getDeviceId();

				// break the cycle if the needed information has been found
				break;
			}
		}

		return deviceId;
	}


}
