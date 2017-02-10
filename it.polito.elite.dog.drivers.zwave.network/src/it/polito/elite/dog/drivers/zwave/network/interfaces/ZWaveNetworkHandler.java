/**
 * 
 */
package it.polito.elite.dog.drivers.zwave.network.interfaces;

import java.util.Map;

import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.network.ZWaveDriverInstance;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;

/**
 * @author bonino
 *
 */
public interface ZWaveNetworkHandler
{
	/**
	 * Read the current value associated to the given nodeInfo. This value can
	 * be different from the real status of the device, since some devices
	 * doesn't update Z-Way server in real-time
	 * 
	 * @param nodeInfo
	 *            the {@link ZWaveNodeInfo} to read.
	 * @param bRequery
	 *            if true requery Z-Way server, otherwise use cached data
	 */
	public void read(ZWaveNodeInfo nodeInfo, boolean bRequery);

	/**
	 * Read the current value of all devices. Values can be different from the
	 * real status of the devices, since some devices doesn't update Z-Way
	 * server in real-time
	 * 
	 * @param bRequery
	 *            if true requery Z-Way server, otherwise use cached data
	 */
	public void readAll(boolean bRequery);

	// /**
	// * Register polling time for devices
	// * TODO
	// *
	// * @param nodeInfo
	// * @param pollingTimeMillis
	// */
	// void registerPoolingTime(int pollingTimeMillis);

	/**
	 * Ask the device its real status and trigger an update of the data tree.
	 * Note that the device will update Z-Way data only when it is waked up:
	 * while a device is in sleep state it will no answer to any request.
	 * 
	 * @param nodeInfo
	 *            the {@link ZWaveNodeInfo} to update.
	 */
	public void updateSensor(ZWaveNodeInfo nodeInfo);

	/**
	 * Writes a given command to a given ZWave nodeInfo
	 * 
	 * @param deviceId
	 *            the unique identifier of the device.
	 * @param instanceId
	 *            the unique identifier of the instance.
	 * @param nCommandClass
	 *            command class. See
	 *            it.polito.elite.dog.drivers.zwave.ZWaveConst
	 * @param commandValue
	 *            the command value to send.
	 */
	public void write(int deviceId, int instanceId, int nCommandClass,
			String commandValue);

	/**
	 * Sends a given command to the controller node
	 * 
	 * @param sCommand
	 * @param commandValue
	 */
	public void controllerWrite(String sCommand, String commandValue);

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
	public void addDriver(ZWaveNodeInfo nodeInfo, int updateTimeMillis,
			ZWaveDriverInstance driver);

	/**
	 * Removes a device-specific driver for the given register
	 * 
	 * @param nodeInfo
	 *            the unique identifier.
	 */
	void removeDriver(ZWaveNodeInfo nodeInfo);

	/**
	 * removes a specific device/driver association
	 * 
	 * @param nodeId
	 */
	public void removeDriver(int nodeId);

	/**
	 * Removes the driver-register associations for the given driver. To be
	 * called when a specific driver disconnects
	 * 
	 * @param datapoint
	 */
	void removeDriver(ZWaveDriverInstance driver);

	/**
	 * Get all raw device data for devices currently available on the network,
	 * including those that still have to be configured in Dog
	 * 
	 * @return
	 */
	public Map<Integer, Device> getRawDevices();

	/**
	 * Get raw device data on the basis of the given nodeId
	 * 
	 * @return
	 */
	public Device getRawDevice(int nodeId);

	/**
	 * Get the URI of the device associated to the given nodeId, on if the
	 * device is active and currently attached to one ZWaveDriver, otherwise
	 * returns a null pointer
	 * 
	 * @param nodeId
	 *            the nodeId of the device
	 * @return the URI of the device as {@link String} or null if the device is
	 *         neither active nor attached.
	 */
	public String getControllableDeviceURIFromNodeId(int nodeId);

	/**
	 * Returns the network-level polling time (base time upon which all
	 * device-specific polling times are based).
	 * 
	 * @return
	 */
	public long getPollingTimeMillis();
}
