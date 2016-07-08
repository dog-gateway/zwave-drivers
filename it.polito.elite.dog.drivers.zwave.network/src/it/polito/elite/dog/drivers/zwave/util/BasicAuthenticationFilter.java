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
import java.util.Base64;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

/**
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public class BasicAuthenticationFilter implements ClientRequestFilter
{
	// the Base64-encoded username and password to adopt
	private String authToken;

	/**
	 * Builds a Basic authentication filter which adds the given user name and
	 * password to the request headers.
	 * 
	 * @param sUser
	 *            The username
	 * @param sPassword
	 *            The password
	 * @throws UnsupportedEncodingException
	 *             if encoding is not supported
	 */
	public BasicAuthenticationFilter(String sUser, String sPassword)
			throws UnsupportedEncodingException
	{
		// compute once the authorization token and discard plain username and
		// password
		this.authToken = Base64.getEncoder()
				.encodeToString((sUser + ":" + sPassword).getBytes("UTF-8"));
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException
	{
		requestContext.getHeaders().add("Authorization",
				"Basic " + this.authToken);
	}

}
