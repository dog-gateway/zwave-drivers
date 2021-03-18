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
package it.polito.elite.dog.drivers.zwave.model.zway.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;



public class CommandClasses
{
	@JsonProperty("userSet")
	private Map<String, Object> userSet;
	@JsonProperty("userGet")
	private Map<String, Object> userGet;
	@JsonProperty("configGet")
	private Map<String, List<GetSetEntry>> configGet;
	@JsonProperty("configSet")
	private Map<String, List<GetSetEntry>> configSet;
	@JsonProperty("name")
	private String name;
	@JsonProperty("data")
	private CommandClassesData data;

	// @JsonProperty("configGet") private ConfigGet configGet;
	// @JsonProperty("configSet") private ConfigSet configSet;

	public Map<String, Object> getUserSet()
	{
		return userSet;
	}

	public void setUserSet(Map<String, Object> userSet)
	{
		this.userSet = userSet;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public CommandClassesData getCommandClassesData()
	{
		return data;
	}

	public void setCommandClassesData(CommandClassesData data)
	{
		this.data = data;
	}

	/**
	 * 
	 * @param propertyName
	 * @return
	 */
	public DataElemObject get(String propertyName)
	{
		return data.getDataElem(propertyName);
	}

	public Map<String, Object> getUserGet()
	{
		return userGet;
	}

	public void setUserGet(Map<String, Object> userGet)
	{
		this.userGet = userGet;
	}

	/**
	 * @return UpdateTime of LEVEL field from ZWay server
	 */
	public long getLevelUpdateTime()
	{
		DataElemObject ii = getCommandClassesData()
				.getDataElem(CommandClassesData.FIELD_LEVEL);

		if (ii != null)
			return ii.getUpdateTime();
		else
			return DataConst.INVALID;
	}

	/**
	 * @return LEVEL field from ZWay server. Field Level is commonly used by
	 *         onOff or dimmable switch. 0 = Off 1-99 = used by dimmable switch
	 *         for % set 255 = On
	 */
	public int getLevelAsInt()
	{
		int value = DataConst.INVALID;

		Object iiObj = getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_LEVEL);

		// patch for versions higher than 1.3.1
		if (iiObj instanceof Integer)
		{
			value = ((Integer) iiObj).intValue();
		}
		else if (iiObj instanceof Boolean)
		{
			Boolean ii = (Boolean) iiObj;

			if (ii)
				value = 255;
			else
				value = 0;
		}

		return value;
	}

	public boolean getLevelAsBoolean()
	{
		Boolean ii = (Boolean) getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_LEVEL);

		if (ii != null)
			return ii.booleanValue();
		else
			return false;
	}

	/**
	 * @return Value associated. This is often used for read sensor value
	 */
	public double getVal()
	{
		Double ii = (Double) getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_VAL);

		if (ii != null)
			return ii.doubleValue();
		else
			return DataConst.INVALID;
	}

	/**
	 * @return scale associated with getVal() method. This can be an empty
	 *         String because device manufacturer not always populates this
	 *         field
	 */
	public String getScale()
	{
		Object ii = getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_SCALE);

		if (ii != null)
			return ii.toString();
		else
			return "";
	}

	public long getValUpdateTime()
	{
		DataElemObject ii = getCommandClassesData()
				.getDataElem(CommandClassesData.FIELD_VAL);

		if (ii != null)
			return ii.getUpdateTime();
		else
			return DataConst.INVALID;
	}

	public String getSensorType()
	{
		Object ii = getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_SENSORTYPE);

		if (ii != null)
			return ii.toString();
		else
			return "";
	}

	/**
	 * @return UNIX Timestamp representing the updateTime of this element
	 */
	public long getUpdateTime()
	{
		// invalid by default
		long updateTime = DataConst.INVALID;

		// get the update time
		Long ii = (Long) getCommandClassesData().getUpdateTime();

		//if an update time has been specified, return it
		if (ii != null)
			updateTime = ii.longValue();

		// return the update time
		return updateTime;
	}

	public boolean isInterviewDone()
	{
		Boolean ii = (Boolean) getCommandClassesData()
				.getDataElemValue(CommandClassesData.FIELD_ISINTERVIEWDONE);

		if (ii != null)
			return ii.booleanValue();
		else
			return false;
	}
}
