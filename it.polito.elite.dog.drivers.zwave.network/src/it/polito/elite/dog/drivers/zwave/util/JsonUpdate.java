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
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.polito.elite.dog.drivers.zwave.model.zway.json.ZWaveModelTree;


public class JsonUpdate 
{
	@SuppressWarnings("deprecation")
	public static ZWaveModelTree updateModel(ObjectMapper mapper, JsonNode zWaveTree, String json) throws JsonProcessingException, IOException
	{
		JsonNode node = mapper.readTree(json);
		Iterator<Entry<String, JsonNode>> it = node.fields();
		
		while(it.hasNext())
		{
			Entry<String, JsonNode> n = it.next();
			String sNodeKey = n.getKey();
			int nIndex = sNodeKey.indexOf(".");
			int nStart = 0;
			
			JsonNode x = zWaveTree;
			while(nIndex > 0)
			{
				x = x.path(sNodeKey.substring(nStart, nIndex));
				nStart = nIndex + 1;
				nIndex = sNodeKey.indexOf(".",nStart);
			}
			
			String sKey = sNodeKey.substring(nStart);
			((ObjectNode)x).put(sKey, n.getValue());
		}
		
		return mapper.treeToValue(zWaveTree, ZWaveModelTree.class);
	}
}
