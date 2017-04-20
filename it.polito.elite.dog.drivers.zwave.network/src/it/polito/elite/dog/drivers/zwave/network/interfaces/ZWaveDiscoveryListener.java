/**
 * 
 */
package it.polito.elite.dog.drivers.zwave.network.interfaces;

import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;

/**
 * @author bonino
 *
 *TODO: find a better naming...
 */
public interface ZWaveDiscoveryListener
{
	//TODO: check if other parameters are needed
	public void detectedUnknownZWaveDevice(Device discoveredDevice, int nodeId);
	
	//TODO: check the right parameters to use
	public void detectedNotExistingZWaveDevice(Device notExistingDevice, int nodeId);
}
