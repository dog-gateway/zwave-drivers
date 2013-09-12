/*
 * Dog  - Z-Wave
 * 
 * Copyright [2013] 
 * [Davide Aimone (aimone.dav@gmail.com)]
 * [Dario Bonino (dario.bonino@polito.it), Politecnico di Torino] 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed 
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and limitations under the License. 
 */
package it.polito.elite.dog.drivers.zwave.model;

import org.codehaus.jackson.annotate.JsonProperty;

public class Range {
	@JsonProperty("max") private long max;
	@JsonProperty("min") private long min;
	@JsonProperty("shift") private Integer shift;
}