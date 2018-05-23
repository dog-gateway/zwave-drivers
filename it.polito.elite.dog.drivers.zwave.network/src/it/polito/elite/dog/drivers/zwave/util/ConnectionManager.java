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
package it.polito.elite.dog.drivers.zwave.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.osgi.service.log.LogService;

import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.model.zway.json.ZWaveModelTree;

public class ConnectionManager
{
	// the log identifier, unique for the class
	public static String LOG_ID = "[ZWaveConnessionManager]: ";

	public static final String DATA_PATH = "/ZWaveAPI/Data";
	public static final String RUN_PATH = "/ZWaveAPI/Run";
	private static ConnectionManager connectionManager = null;

	protected String sURL;

	protected Client client;
	protected WebTarget service;
	private String sLastError;

	// Tree representing system status
	ZWaveModelTree zWaveModelTree = null;

	// Json tree representig the system status, used for partial update
	private JsonNode zWaveTree;

	// Convert JSON to Java object
	ObjectMapper mapper = new ObjectMapper();

	// last update
	long lastUpdate = 0;

	// the logger
	LogHelper logger;

	public ConnectionManager(String sURL, String sUser, String sPassword,
			LogHelper logger)
	{
		this.sURL = sURL;
		this.logger = logger;

		// if credentials are specified, create an authenticated client,
		// otherwise a simple one.
		if ((sUser != null) && (!sUser.isEmpty()) && (sPassword != null)
				&& (!sPassword.isEmpty()))
		{
			// create authenticated client
			try
			{
				client = ClientBuilder.newBuilder()
						.register(JacksonFeature.class)
						.register(
								new BasicAuthenticationFilter(sUser, sPassword))
						.build();
			}
			catch (UnsupportedEncodingException e)
			{
				logger.log(LogService.LOG_ERROR,
						"Unable to correctly set Basic authentication parameters",
						e);
			}
		}
		else
		{
			// create simple client
			client = ClientBuilder.newBuilder().register(JacksonFeature.class)
					.build();
		}
		service = client.target(sURL);
	}
	
	/*------ SINGLETON -------*/
	
	//inherited from the previous single-gateway implementation
	//needs to be revised and possibly deleted if not needed.

	/**
	 * Obtain the current ConnessionManager
	 * 
	 * @return ConnessionManager, may be null
	 */
	public static ConnectionManager get()
	{
		return connectionManager;
	}

	/**
	 * Obtain an instance of ConnessionManager, with the given params. If one of
	 * them changed or if no ConnessionManager already exists, a new one is
	 * created
	 * 
	 * @param sURL
	 * @param sUser
	 * @param sPassword
	 * @return ConnessionManager
	 */
	public static ConnectionManager get(String sURL, String sUser,
			String sPassword, LogHelper logger)
	{
		// create a new instance if needed, or returns the current one
		if (connectionManager == null
				|| !sURL.equals(connectionManager.getURL()))

			connectionManager = new ConnectionManager(sURL, sUser, sPassword,
					logger);

		return connectionManager;
	}
	
	/*----- END SINGLETON ------*/

	/**
	 * Query zway-server for an update of the system status since the lSince
	 * param. Implementation of partial update is 2-3 times faster than a full
	 * update.
	 * 
	 * @param lSince
	 *            Unix-timestamp representing the last update, 0 for a full
	 *            query
	 * @return ZWaveModelTree representing the full system
	 * @throws Exception
	 * 
	 */
	public ZWaveModelTree updateDevices(long lSince) throws Exception
	{
		Response response = service.path(DATA_PATH).path(String.valueOf(lSince))// after ZWay2.3.x lsince no longer works
				.request(MediaType.APPLICATION_JSON).get(); // was post, after ZWay2.3.xpost no longer works from Jersey

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			String json = response.readEntity(String.class);

			// Convert JSON to Java object
			try
			{
				// in this case we process the whole data
				if (zWaveModelTree == null || lSince == 0)
				{
					// System.out.println(json);//use
					// http://jsoneditoronline.org/ for a friendly UI
					zWaveModelTree = mapper.readValue(json,
							ZWaveModelTree.class);
					zWaveTree = mapper.readTree(json);
				}
				else
				// otherwise we proceed to update the tree
				{
					zWaveModelTree = JsonUpdate.updateModel(mapper, zWaveTree,
							json);// devices.26.instances.0.commandClasses.49.data.1.val
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
				throw e;
			}
		}
		else
		{
			throw new Exception("Can't read json from Z-Way server: "
					+ response.toString());
		}

		return zWaveModelTree;
	}

	/**
	 * Query zway-server for an update of the system status. It's equivalent to
	 * updateDevices(ZWaveModelTree.getLastUpdate())
	 * 
	 * @return ZWaveModelTree representing the full system
	 * @throws Exception
	 */
	public ZWaveModelTree updateDevices() throws Exception
	{
		updateDevices(lastUpdate);
		// Update time of the last update
		lastUpdate = zWaveModelTree.getUpdateTime();
		return zWaveModelTree;
	}

	/**
	 * Send a command and returns a boolean
	 * 
	 * @param sCommand
	 * @return if client response status is ok returns true, otherwise false.
	 *         Call getLastError() to discover causes of fail
	 */
	protected boolean sendCommandBoolean(String sCommand)
	{
		boolean bSuccess = true;

		try
		{
			sendCommand(sCommand);
		}
		catch (Exception e)
		{
			bSuccess = false;
			sLastError = e.getMessage();
		}

		return bSuccess;
	}

	public String sendCommand(String sCommand) throws Exception
	{
		String jsonResponse = null;

		Response response = service.path(RUN_PATH).path(sCommand)
				.request(MediaType.APPLICATION_JSON_TYPE).get(); // was post, after razberry 2.3.x post no longer works from Jersey

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			jsonResponse = response.readEntity(String.class);
		}
		else
		{
			throw new Exception("Can't read json from Z-Way server: "
					+ response.toString());
		}

		return jsonResponse;
	}

	public String pingDevice(String sNodeId) throws Exception
	{
		return sendCommand("devices[" + sNodeId + "].SendNoOperation()");
	}

	public String getURL()
	{
		return sURL;
	}

	public String getLastError()
	{
		return sLastError;
	}
}
