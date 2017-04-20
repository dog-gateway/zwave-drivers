/**
 * 
 */
package it.polito.elite.dog.drivers.zwave.network.tasks;

import java.util.Set;

import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveDiscoveryListener;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetworkHandler;

/**
 * @author bonino
 *
 */
public class NotifyNotExistingDeviceTask implements Runnable
{
	// the set of listeners to be notified by this task
	private Set<ZWaveDiscoveryListener> listenersToNotify;

	// the not existing device to be handled by a discovery listener
	private Device notExistingDevice;

	// the not existing  device node id
	private int nodeId;

	/**
	 * Task constructor, accepts as input the list of
	 * {@link ZWaveDiscoveryListener} to notify about the detection of a not
	 * existing device and the device ({@link Device}) known in Dog but not
	 * existing at the ZWave network level. It subsequently notifies all
	 * listeners by calling their
	 * 
	 * <pre>
	 * detectedNotExistingZWaveDevice
	 * </pre>
	 * 
	 * method. Calls to listeners are done one after the other and they are
	 * synchronous, therefore a slow handling of such a call by a listener
	 * affects the delivery to all subsequent listeners. Typically there is only
	 * one listener per {@link ZWaveNetworkHandler} therefore this
	 * implementation should not suffer from this implementation choice.
	 * 
	 * @param listenersToNotify
	 *            The {@link Set} of listeners to be notified
	 * @param notExistingDevice
	 *            The device known at the Dog level but not existing at the
	 *            ZWave network level.
	 * @param nodeId
	 *            The id of the device known at the Dog level but not existing
	 *            at the ZWave network level.
	 */
	public NotifyNotExistingDeviceTask(
			Set<ZWaveDiscoveryListener> listenersToNotify, Device unknownDevice,
			int nodeId)
	{
		this.listenersToNotify = listenersToNotify;
		this.notExistingDevice = unknownDevice;
		this.nodeId = nodeId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		// notify listeners
		for (ZWaveDiscoveryListener listener : this.listenersToNotify)
		{
			listener.detectedNotExistingZWaveDevice(this.notExistingDevice,
					nodeId);
		}
	}

}
