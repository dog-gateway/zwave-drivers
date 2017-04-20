/*
 * Dog - Network Driver
 * 
 * Copyright 2013-2014 Davide Aimone and Dario Bonino 
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

import java.util.Dictionary;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetwork;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetworkHandler;

public class ZWaveDriverImpl implements ZWaveNetwork, ManagedService
{
	// -------------- The configuration parameters --------------

	// the update time millis configuration parameter
	public static String POLLING_TIME_MILLIS_CONFIG = "pollingTimeMillis";

	// the discovery configuration parameter
	public static String AUTO_DISCOVERY = "autoDiscovery";

	// ----------------------------------------------------------
	// the log identifier, unique for the class
	public static String LOG_ID = "[ZWaveDriverImpl]: ";

	// the bundle context
	private BundleContext bundleContext;

	// the service registration handle
	private ServiceRegistration<?> regServiceZWaveDriverImpl;

	// the driver logger
	private LogHelper logger;

	// the baseline pollingTime
	private int pollingTimeMillis;

	// the auto-discovery flag, true by default
	private boolean autoDiscovery;

	// the ZWaveNetworkHandler Map indexed by gateway URI (device id)
	private ConcurrentHashMap<String, ZWaveNetworkHandlerImpl> handlers;

	public ZWaveDriverImpl()
	{
		// initialize the zwave handlers map
		this.handlers = new ConcurrentHashMap<String, ZWaveNetworkHandlerImpl>();

		// set the autodiscovery initially at true
		this.autoDiscovery = false;
	}

	/*
	 * TODO: handle dynamic update of configuration parameters
	 */
	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		// get the bundle configuration parameters
		if (properties != null)
		{
			logger.log(LogService.LOG_DEBUG,
					"Received configuration properties");

			// try to get the baseline polling time
			String pollingTimeAsString = (String) properties
					.get(ZWaveDriverImpl.POLLING_TIME_MILLIS_CONFIG);

			// trim leading and trailing spaces
			pollingTimeAsString = pollingTimeAsString.trim();

			// check not null
			if (pollingTimeAsString != null)
			{
				// parse the string
				int newPollingTimeMillis = Integer.valueOf(pollingTimeAsString);

				if (newPollingTimeMillis != this.pollingTimeMillis)
				{
					// store the new value
					this.pollingTimeMillis = newPollingTimeMillis;

					// avoid useless calls
					if ((this.handlers != null) && (!this.handlers.isEmpty()))
					{
						// Prepare an asynchronous task to update the handlers
						// (Such
						// an updated is done in a separate
						// thread as per OSGi specs, this method shall return as
						// quickly as possible)
						Thread updatePollingTimeThread = new Thread(
								new Runnable()
								{

									@Override
									public void run()
									{
										for (ZWaveNetworkHandler handler : handlers
												.values())
										{
											handler.setPollingTime(
													pollingTimeMillis);
										}

									}
								});

						// spawn the worker thread
						updatePollingTimeThread.start();
					}
				}
			}

			// try to get the autodiscovery flag
			String autoDiscoveryAsString = (String) properties
					.get(ZWaveDriverImpl.AUTO_DISCOVERY);

			// check not null
			if (autoDiscoveryAsString != null)
			{
				// parse the string
				this.autoDiscovery = Boolean.valueOf(autoDiscoveryAsString);
			}

			// TODO: handle run-time change of the auto-discovery flag

			// register the driver service if not already registered
			if (regServiceZWaveDriverImpl == null)
				regServiceZWaveDriverImpl = bundleContext.registerService(
						ZWaveNetwork.class.getName(), this, null);

		}
	}

	/**
	 * Handle the bundle activation
	 */
	protected void activate(BundleContext bundleContext)
	{
		// create a logger
		logger = new LogHelper(bundleContext);

		// store the bundle context
		this.bundleContext = bundleContext;

		logger.log(LogService.LOG_DEBUG, "Activated: ZWave NetworkDriver...");
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void deactivate()
	{
		// stop the all pollers?

		// free all devices?

		// unregister service
		if (regServiceZWaveDriverImpl != null)
			regServiceZWaveDriverImpl.unregister();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #addDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveNodeInfo)
	 */
	@Override
	public ZWaveNetworkHandler addDriver(ZWaveNodeInfo nodeInfo,
			int updateTimeMillis, ZWaveDriverInstance driver)
	{
		return this.addDriver(nodeInfo, updateTimeMillis, driver, null, null);
	}

	@Override
	public ZWaveNetworkHandler addDriver(ZWaveNodeInfo nodeInfo,
			int updateTimeMillis, ZWaveDriverInstance driver, String username,
			String password)
	{
		// check if the device is a gateway
		if (nodeInfo.isController())
		{
			// check if not already registered
			if (!this.handlers.containsKey(nodeInfo.getAdapterEndpoint()))
			{
				// insert the gateway
				this.handlers.put(nodeInfo.getAdapterEndpoint(),
						new ZWaveNetworkHandlerImpl(
								nodeInfo.getAdapterEndpoint(), username,
								password, this.pollingTimeMillis,
								this.autoDiscovery, this.logger));
			}
		}
		// get the network handler
		ZWaveNetworkHandler handler = this.handlers
				.get(nodeInfo.getAdapterEndpoint());

		// if the device is not yet registered, registers it
		if (handler.getControllableDeviceURIFromNodeId(
				nodeInfo.getDeviceNodeId()) == null)
		{
			// add the driver
			handler.addDriver(nodeInfo, updateTimeMillis, driver);
		}

		// return the handler
		return handler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #removeDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveNodeInfo)
	 */
	@Override
	public void removeDriver(ZWaveNodeInfo nodeInfo)
	{
		// get the handler
		ZWaveNetworkHandler handler = this.handlers
				.get(nodeInfo.getAdapterEndpoint());

		// remove the driver
		if (handler != null)
			handler.removeDriver(nodeInfo);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.polito.elite.dog.drivers.zwave.networkdriver.interfaces.ZWaveNetwork
	 * #removeDriver(it.polito.elite.dog.drivers.zwave.networkdriver.info.
	 * ZWaveDriver)
	 */
	@Override
	public void removeDriver(ZWaveDriverInstance driver)
	{
		// get the handler
		ZWaveNetworkHandler handler = this.handlers
				.get(driver.getGatewayEndpoint());

		if (handler != null)
			handler.removeDriver(driver);
	}

	/**
	 * Provides a reference to the {@link LogService} instance used by this
	 * class to log messages...
	 * 
	 * @return
	 */
	public LogHelper getLogger()
	{
		return logger;
	}

	/**
	 * Provides the polling time to be used by Poller threads connect to this
	 * driver instance...
	 * 
	 * @return
	 */
	@Override
	public long getPollingTimeMillis()
	{
		return pollingTimeMillis;
	}

	@Override
	public ZWaveNetworkHandler getHandler(String adapterEndpoint)
	{
		return this.handlers.get(adapterEndpoint);
	}

}
