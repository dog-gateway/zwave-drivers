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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;



public class AreasData 
{
	//static name properties
	private Integer updateTime;
	private String name;
	private Object value;
	private String type;
	private Integer invalidateTime;
	
	//dynamic name prop are managed through this map
	//and through @JsonAnyGetter and @JsonAnySetter annotation
	private Map<String, AreasDataElem> data = new HashMap<String, AreasDataElem>(); 
	
	@JsonCreator
    public AreasData(@JsonProperty("updateTime") Integer updateTime,
    		@JsonProperty("name") String name,
    		@JsonProperty("value") Object value,
    		@JsonProperty("type") String type,
    		@JsonProperty("invalidateTime") Integer invalidateTime
    		)
    {
		this.updateTime = updateTime;
		this.name = name;
		this.value = value;
		this.type = type;
		this.invalidateTime = invalidateTime;
    }
	
	// "any getter" needed for serialization    
    @JsonAnyGetter
    public Map<String, AreasDataElem> getAllData() {
        return data;
    }

    @JsonAnySetter
    public void set(String name, AreasDataElem value) {
    	data.put(name, value);
    }
    
    /**
     * returns the property defined by name or null
     */
    public AreasDataElem get(String name)
    {
    	return data.get(name);
    }
    
	/**
	 * @return the updateTime
	 */
	public Integer getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(Integer updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(Object value) {
		this.value = value;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return the invalidateTime
	 */
	public Integer getInvalidateTime() {
		return invalidateTime;
	}

	/**
	 * @param invalidateTime the invalidateTime to set
	 */
	public void setInvalidateTime(Integer invalidateTime) {
		this.invalidateTime = invalidateTime;
	}
}
