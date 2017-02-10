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
package it.polito.elite.dog.drivers.zwave.device;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceCostants;
import it.polito.elite.dog.core.library.model.devicecategory.Controllable;
import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.gateway.ZWaveGatewayDriver;
import it.polito.elite.dog.drivers.zwave.network.ZWaveDriverInstance;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetwork;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;

public abstract class ZWaveDeviceDriver implements Driver, ManagedService
{
	// The OSGi framework context
	protected BundleContext context;

	// System logger
	protected LogHelper logger;

	// a reference to the network driver
	private AtomicReference<ZWaveNetwork> network;

	// a reference to the gateway driver
	private AtomicReference<ZWaveGatewayDriver> gateway;

	// milliseconds between two update of the device status, from configuration
	// file
	protected int updateTimeMillis;

	// the list of instances controlled / spawned by this driver
	protected Hashtable<String, ZWaveDriverInstance> managedInstances;

	// the registration object needed to handle the life span of this bundle in
	// the OSGi framework (it is a ServiceRegistration object for use by the
	// bundle registering the service to update the service's properties or to
	// unregister the service).
	private ServiceRegistration<?> regDriver;

	// the filter query for listening to framework events relative to the
	// to the ZWave gateway driver
	String filterQuery = String.format("(%s=%s)", Constants.OBJECTCLASS,
			ZWaveGatewayDriver.class.getName());

	// what are the on/off device categories that can match with this driver?
	protected Set<String> deviceCategories;

	// the driver instance class from which extracting the supported device
	// categories
	protected Class<?> driverInstanceClass;

	public ZWaveDeviceDriver()
	{
		// intialize atomic references
		this.gateway = new AtomicReference<ZWaveGatewayDriver>();
		this.network = new AtomicReference<ZWaveNetwork>();

		// initialize the connected drivers list
		this.managedInstances = new Hashtable<String, ZWaveDriverInstance>();

		// initialize the set of implemented device categories
		this.deviceCategories = new HashSet<String>();
	}

	/**
	 * Handle the bundle activation
	 */
	public void activate(BundleContext bundleContext)
	{
		// init the logger
		this.logger = new LogHelper(bundleContext);

		// store the context
		this.context = bundleContext;

		// fill the device categories
		this.properFillDeviceCategories(this.driverInstanceClass);

	}

	public void deactivate()
	{
		// remove the service from the OSGi framework
		this.unRegisterZWaveDeviceDriver();
	}

	public void addingService(ZWaveGatewayDriver gatewayDriver)
	{
		gateway.set(gatewayDriver);

		// TODO: remove and use declarative services!!!
		network.set(gateway.get().getNetwork());
	}

	public void removedService(ZWaveGatewayDriver gatewayDriver)
	{
		if (gateway.compareAndSet(gatewayDriver, null))
			// unregisters this driver from the OSGi framework
			unRegisterZWaveDeviceDriver();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public synchronized int match(ServiceReference reference) throws Exception
	{
		int matchValue = Device.MATCH_NONE;

		// get the given device category
		String deviceCategory = (String) reference
				.getProperty(DeviceCostants.DEVICE_CATEGORY);

		// get the given device manufacturer
		String manifacturer = (String) reference
				.getProperty(DeviceCostants.MANUFACTURER);

		// get the gateway to which the device is connected
		String gateway = (String) reference.getProperty(DeviceCostants.GATEWAY);

		// compute the matching score between the given device and
		// this driver
		if (deviceCategory != null)
		{
			if (manifacturer != null && (gateway != null)
					&& (manifacturer.equals(ZWaveInfo.MANUFACTURER))
					&& (this.deviceCategories.contains(deviceCategory))
					&& (this.gateway.get().isGatewayAvailable(gateway)))
			{
				matchValue = Controllable.MATCH_MANUFACTURER
						+ Controllable.MATCH_TYPE;
			}

		}

		return matchValue;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public synchronized String attach(ServiceReference reference)
			throws Exception
	{
		// get the referenced device
		ControllableDevice device = ((ControllableDevice) context
				.getService(reference));

		// check if not already attached
		if (!this.managedInstances.containsKey(device.getDeviceId()))
		{
			// get the gateway to which the device is connected
			String gateway = (String) device.getDeviceDescriptor().getGateway();

			// get the corresponding end point set
			Set<String> nodeIdSet = device.getDeviceDescriptor()
					.getSimpleConfigurationParams().get(ZWaveInfo.NODE_ID);

			// get the corresponding end point set
			Set<String> instanceIdSet = device.getDeviceDescriptor()
					.getSimpleConfigurationParams().get(ZWaveInfo.INSTANCE_ID);

			// get the nodeId
			String sNodeID = nodeIdSet.iterator().next();

			// get the list of instances available
			HashSet<Integer> instancesId = new HashSet<Integer>();

			for (String sInstanceId : instanceIdSet)
				instancesId.add(Integer.parseInt(sInstanceId));

			// create a new driver instance
			ZWaveDriverInstance driverInstance = this
					.createZWaveDriverInstance(network.get(), device,
							Integer.parseInt(sNodeID), instancesId,
							this.gateway.get().getSpecificGateway(gateway).getGatewayEndpoint(),
							this.gateway.get().getSpecificGateway(gateway)
									.getNodeInfo().getDeviceNodeId(),
							updateTimeMillis, context);

			// connect this driver instance with the device
			device.setDriver(driverInstance);

			// store a reference to the connected driver
			synchronized (this.managedInstances)
			{
				this.managedInstances.put(device.getDeviceId(), driverInstance);
			}
		}
		else
		{
			this.context.ungetService(reference);
		}

		return null;
	}

	public abstract ZWaveDriverInstance createZWaveDriverInstance(
			ZWaveNetwork zWaveNetwork, ControllableDevice device, int nodeId,
			HashSet<Integer> instancesId, String string, int gatewayNodeId,
			int updateTimeMillis, BundleContext context);

	/**
	 * Registers this driver in the OSGi framework, making its services
	 * available to all the other bundles living in the same or in connected
	 * frameworks.
	 */
	private void registerZWaveDeviceDriver()
	{
		if ((gateway.get() != null) && (network.get() != null)
				&& (this.context != null) && (this.regDriver == null))
		{
			// create a new property object describing this driver
			Hashtable<String, Object> propDriver = new Hashtable<String, Object>();
			// add the id of this driver to the properties
			propDriver.put(DeviceCostants.DRIVER_ID, this.getClass().getName());
			// register this driver in the OSGi framework
			regDriver = context.registerService(Driver.class.getName(), this,
					propDriver);
		}
	}

	/**
	 * Handle the bundle de-activation
	 */
	protected void unRegisterZWaveDeviceDriver()
	{
		// TODO DETACH allocated Drivers
		if (regDriver != null)
		{
			regDriver.unregister();
			regDriver = null;
		}
	}

	/**
	 * Fill a set with all the device categories whose devices can match with
	 * this driver. Automatically retrieve the device categories list by reading
	 * the implemented interfaces of its DeviceDriverInstance class bundle.
	 */
	public void properFillDeviceCategories(Class<?> cls)
	{
		if (cls != null)
		{
			for (Class<?> devCat : cls.getInterfaces())
			{
				this.deviceCategories.add(devCat.getName());
			}
		}

	}

	@Override
	public void updated(Dictionary<String, ?> properties)
			throws ConfigurationException
	{
		if (properties != null)
		{
			// try to get the baseline polling time
			String updateTimeAsString = (String) properties
					.get(ZWaveInfo.PROPERTY_UPDATETIME);

			// trim leading and trailing spaces
			updateTimeAsString = updateTimeAsString.trim();

			// check not null
			if (updateTimeAsString != null)
			{
				// parse the string
				updateTimeMillis = Integer.valueOf(updateTimeAsString);
			}
			else
			{
				throw new ConfigurationException(ZWaveInfo.PROPERTY_UPDATETIME,
						ZWaveInfo.PROPERTY_UPDATETIME
								+ " not defined in configuraton file for "
								+ ZWaveDeviceDriver.class.getName());
			}

			// register driver
			registerZWaveDeviceDriver();
		}
	}
}
