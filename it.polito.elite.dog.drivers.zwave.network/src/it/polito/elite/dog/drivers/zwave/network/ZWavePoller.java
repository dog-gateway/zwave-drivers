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

import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.log.LogService;

/**
 * This thread implements a queue.
 * Every deltaT = ZWaveDriverImpl.getPollingTimeMillis(), it wakes-up and asks Z-Way server for system update. 
 * Later it checks TODO
 * successivamente controlla quali device devono essere triggerati in modo da far aggiornare il loro stato.
 *  Questo e' dovuto al fato che alcuni device non comunicano correttamente il loro cambio di stato al server zway, ed e' quindi 
 *  necessario che venga effettuata su di loro una GET da parte di ZWay. Questo thread si occupa di schedulare anche questo tipo di azione.
 *  Per aggiunger un device alla lista dei nodi da triggerare usare la addDeviceToQueue
 *
 */
public class ZWavePoller extends Thread
{
	// reference to the ZWaveDriverImpl currently using this poller
	private ZWaveNetworkHandlerImpl driver;

	// the runnable flag
	private boolean runnable = true;

	// the poller logger
	private LogHelper logger;

	// the log identifier, unique for the class
	public static String logId = "[ZWavePoller]: ";
	
	// set of device to trigger for update
	public Set<TriggerElem> deviceSet = Collections.newSetFromMap(new ConcurrentHashMap<TriggerElem, Boolean>());
	//public ConcurrentHashMap<Integer,TriggerElem> deviceSet = new ConcurrentHashMap<Integer,TriggerElem>();

	public ZWavePoller(ZWaveNetworkHandlerImpl zWaveNetworkHandler)
	{		
		// store a reference to the poller driver
		this.driver = zWaveNetworkHandler;

		// init the logger
		this.logger = this.driver.getLogger();
	}


	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run()
	{
		// run until the thread is runnable
		while (this.runnable)
		{
			// log
			this.logger.log(LogService.LOG_DEBUG, ZWavePoller.logId + "Starting a new polling cycle...");

			Date currentDate = new Date();
			
			//for each driver registered
			for(TriggerElem elem : deviceSet)
			{
				//if its triggerTime is already past
				if(elem.getTriggerTime() <= currentDate.getTime())
				{
					//trigger device
					this.driver.updateSensor(elem.getNodeInfo());
					//update value for next trigger
					elem.setTriggerTime(elem.getTriggerTime() + elem.getUpdateTimeMillis());
					
					//yield to other processes
					Thread.yield();
				}
			}
			
			this.driver.readAll(true);

			// ok now the polling cycle has ended and the poller can sleep for the given polling time
			try
			{
				Thread.sleep(this.driver.getPollingTimeMillis());
			}
			catch (InterruptedException e)
			{
				// log the error
				this.logger.log(LogService.LOG_WARNING, ZWavePoller.logId + "Interrupted exception: " + e);
			}

		}

		// auto-reset the state at runnable...
		this.runnable = true;
	}


	/**
	 * Sets the thread state at runnable (true) or not runnable(false)
	 * 
	 * @param runnable
	 */
	public void setRunnable(boolean runnable)
	{
		this.runnable = runnable;
	}
	

	/**
	 * Call this method to add a device to the queue of the thread.
	 * @param nodeInfo {@link ZWaveNodeInfo} representing the node
	 * @param updateTimeMillis how much the device must be triggered for a full update. 0 means no trigger for this node
	 */
	public void addDeviceToQueue(ZWaveNodeInfo nodeInfo, int updateTimeMillis)
	{
		//if updateTimeMillis = 0, the device doesn't need trigger update
		if(updateTimeMillis <= 0)
			return;
		
		//min value allowed for updateTimeMillis is equals to driver.getPollingTimeMillis()
		if(updateTimeMillis < this.driver.getPollingTimeMillis())
		{
			this.logger.log(LogService.LOG_WARNING, ZWavePoller.logId + "Min value allowed for device.'updateTimeMillis' is equals to network.'pollingTimeMillis'");
			updateTimeMillis = (int) this.driver.getPollingTimeMillis();
		}
		
		Date date = new Date();
		TriggerElem elem = new TriggerElem(nodeInfo, date.getTime()+updateTimeMillis, updateTimeMillis);
		deviceSet.add(elem);
	}
	
	/**
	 * Call this method to remove a device to the queue of the thread.
	 * @param nodeInfo {@link ZWaveNodeInfo} representing the node
	 * @param updateTimeMillis how much the device must be triggered for a full update. 0 means no trigger for this node
	 */
	public void removeDeviceFromQueue(int nodeId)
	{
		//the list of triggers to remove
		ArrayList<TriggerElem> toRemove = new ArrayList<TriggerElem>();
		
		//build the list of triggers to remove
		for(TriggerElem elem : this.deviceSet)
			if(elem.getNodeInfo().getDeviceNodeId() == nodeId)
				toRemove.add(elem);
		//remove
		for(TriggerElem elem : toRemove)
			this.deviceSet.remove(elem);
	}
	
	public class TriggerElem
	{
		//how often trigger the node
		protected int updateTimeMillis;
		
		//timestamp for next trigger
		protected long triggerTime;
		
		//nodeInfo representing the node to trigger
		protected ZWaveNodeInfo nodeInfo;
		
		public TriggerElem(ZWaveNodeInfo nodeInfo, long triggerTime, int updateTimeMillis) 
		{
			this.nodeInfo = nodeInfo;
			this.triggerTime = triggerTime;
			this.updateTimeMillis = updateTimeMillis;
		}
		
		/**
		 * @return the triggerTime
		 */
		public long getTriggerTime() {
			return triggerTime;
		}

		/**
		 * @param triggerTime the triggerTime to set
		 */
		public void setTriggerTime(long triggerTime) {
			this.triggerTime = triggerTime;
		}

		/**
		 * @return the nodeInfo
		 */
		public ZWaveNodeInfo getNodeInfo() {
			return nodeInfo;
		}

		/**
		 * @param nodeInfo the nodeInfo to set
		 */
		public void setNodeInfo(ZWaveNodeInfo nodeInfo) {
			this.nodeInfo = nodeInfo;
		}

		/**
		 * @return the updateTimeMillis
		 */
		public int getUpdateTimeMillis() {
			return updateTimeMillis;
		}

		/**
		 * @param updateTimeMillis the updateTimeMillis to set
		 */
		public void setUpdateTimeMillis(int updateTimeMillis) {
			this.updateTimeMillis = updateTimeMillis;
		}
	}
}
