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
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.ws.rs.core.Response.Status;

import org.osgi.service.log.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.polito.elite.dog.drivers.zwave.model.zway.json.ZWaveModelTree;

public class ConnectionManager
{
	// the log identifier, unique for the class
	public static String LOG_ID = "[ZWaveConnessionManager]: ";

	public static final String DATA_PATH = "/ZWaveAPI/Data/";
	public static final String RUN_PATH = "/ZWaveAPI/Run/";
	private static final Duration API_REQUEST_TIMEOUT = Duration.of(30, ChronoUnit.SECONDS);

	protected URI baseURL;

	protected HttpClient hClient;
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
	private Logger logger;

	public ConnectionManager(String sURL, String sUser, String sPassword, Logger logger)
	{
		// handle possible ending slash on the URL
		this.baseURL = URI.create(sURL);
		this.logger = logger;

		// if credentials are specified, create an authenticated client,
		// otherwise a simple one.
		if ((sUser != null) && (!sUser.isEmpty()) && (sPassword != null) && (!sPassword.isEmpty()))
		{
			// create authenticated client
			this.hClient = HttpClient.newBuilder().authenticator(new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(sUser, sPassword.toCharArray());
				}
			}).connectTimeout(Duration.of(10, ChronoUnit.SECONDS)).build();

		}
		else
		{
			// create simple client
			this.hClient = HttpClient.newHttpClient();
		}
	}

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
		HttpRequest request = HttpRequest.newBuilder(this.baseURL.resolve(DATA_PATH)).timeout(API_REQUEST_TIMEOUT)
				.header("Content-type", "application/json").GET().build();

		HttpResponse<String> response = null;
		try
		{
			response = this.hClient.send(request, BodyHandlers.ofString());
		}
		catch (IOException | InterruptedException e)
		{
			this.logger.warn(String.format("Unable to update device: %s", e));
			throw e;
		}

		// if it comes here, response is not null, check the response status
		if (response.statusCode() == Status.OK.getStatusCode())
		{

			String json = response.body();// response.readEntity(String.class);

			// Convert JSON to Java object
			try
			{
				// in this case we process the whole data
				if (zWaveModelTree == null || lSince == 0)
				{
					// System.out.println(json);//use
					// http://jsoneditoronline.org/ for a friendly UI
					zWaveModelTree = mapper.readValue(json, ZWaveModelTree.class);
					zWaveTree = mapper.readTree(json);
				}
				else
				// otherwise we proceed to update the tree
				{
					zWaveModelTree = JsonUpdate.updateModel(mapper, zWaveTree, json);// devices.26.instances.0.commandClasses.49.data.1.val
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
			this.logger.error(String.format("unable to read from Z-Way server: %s", response.body()));
			throw new Exception("Can't read json from Z-Way server: " + response.body());
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
		HttpRequest request = HttpRequest
				.newBuilder(this.baseURL.resolve( RUN_PATH).resolve( URLEncoder.encode(sCommand, StandardCharsets.UTF_8)))
				.timeout(API_REQUEST_TIMEOUT).GET().build();
		HttpResponse<String> response = this.hClient.send(request, BodyHandlers.ofString());

		if (response.statusCode() != Status.OK.getStatusCode())
		{
			throw new Exception("Can't read json from Z-Way server: " + response.toString());
		}

		return response.body();
	}

	public String pingDevice(String sNodeId) throws Exception
	{
		return sendCommand("devices[" + sNodeId + "].SendNoOperation()");
	}

	public String getURL()
	{
		return this.baseURL.toString();
	}

	public String getLastError()
	{
		return sLastError;
	}
}
