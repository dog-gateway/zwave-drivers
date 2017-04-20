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
public class NotifyUnknownDeviceTask implements Runnable
{
	// the set of listeners to be notified by this task
	private Set<ZWaveDiscoveryListener> listenersToNotify;

	// the unknown device to be handled by a discovery listener
	private Device unknownDevice;

	// the unknown device node id
	private int nodeId;

	/**
	 * Task constructor, accepts as input the list of
	 * {@link ZWaveDiscoveryListener} to notify about the discovery of a
	 * previously unknown device and the unknown device ({@link Device}) itself.
	 * It subsequently notifies all listeners by calling their
	 * 
	 * <pre>
	 * detectedUnknownZWaveDevice
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
	 * @param unknownDevice
	 *            The newly discovered device
	 * @param nodeId
	 * 			  The node id of the discovered device
	 */
	public NotifyUnknownDeviceTask(
			Set<ZWaveDiscoveryListener> listenersToNotify, Device unknownDevice, int nodeId)
	{
		this.listenersToNotify = listenersToNotify;
		this.unknownDevice = unknownDevice;
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
			listener.detectedUnknownZWaveDevice(this.unknownDevice,this.nodeId);
		}
	}

}
