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
package it.polito.elite.dog.drivers.zwave.threephaseelectricitymeter;

import it.polito.elite.dog.core.library.model.ControllableDevice;
import it.polito.elite.dog.core.library.model.DeviceStatus;
import it.polito.elite.dog.core.library.model.devicecategory.ThreePhaseElectricityMeter;
import it.polito.elite.dog.core.library.model.state.FrequencyMeasurementState;
import it.polito.elite.dog.core.library.model.state.PowerFactorMeasurementState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseActiveEnergyState;
import it.polito.elite.dog.core.library.model.state.SinglePhaseReactiveEnergyState;
import it.polito.elite.dog.core.library.model.state.State;
import it.polito.elite.dog.core.library.model.state.ThreePhaseActiveEnergyState;
import it.polito.elite.dog.core.library.model.state.ThreePhaseActivePowerMeasurementState;
import it.polito.elite.dog.core.library.model.state.ThreePhaseApparentPowerMeasurementState;
import it.polito.elite.dog.core.library.model.state.ThreePhaseCurrentState;
import it.polito.elite.dog.core.library.model.state.ThreePhaseReactivePowerMeasurementState;
import it.polito.elite.dog.core.library.model.state.ThreePhaseVoltageState;
import it.polito.elite.dog.core.library.model.statevalue.ActiveEnergyStateValue;
import it.polito.elite.dog.core.library.model.statevalue.ActivePowerStateValue;
import it.polito.elite.dog.core.library.model.statevalue.StateValue;
//import it.polito.elite.dog.core.library.util.LogHelper;
import it.polito.elite.dog.drivers.zwave.ZWaveAPI;
import it.polito.elite.dog.drivers.zwave.model.zway.json.CommandClasses;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Controller;
import it.polito.elite.dog.drivers.zwave.model.zway.json.DataElemObject;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Device;
import it.polito.elite.dog.drivers.zwave.model.zway.json.Instance;
import it.polito.elite.dog.drivers.zwave.network.ZWaveDriver;
import it.polito.elite.dog.drivers.zwave.network.info.ZWaveNodeInfo;
import it.polito.elite.dog.drivers.zwave.network.interfaces.ZWaveNetwork;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.measure.DecimalMeasure;
import javax.measure.Measure;
import javax.measure.quantity.Power;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.osgi.framework.BundleContext;

public class ZWaveThreePhaseElectricityMeterInstance extends ZWaveDriver
		implements ThreePhaseElectricityMeter
{

	public ZWaveThreePhaseElectricityMeterInstance(ZWaveNetwork network,
			ControllableDevice device, int deviceId, Set<Integer> instancesId,
			int gatewayNodeId, int updateTimeMillis, BundleContext context)
	{
		super(network, device, deviceId, instancesId, gatewayNodeId,
				updateTimeMillis, context);

		// initialize states
		this.initializeStates();
	}

	/**
	 * Initializes the state asynchronously as required by OSGi
	 */
	private void initializeStates()
	{
		// create the var and va units
		Unit<Power> VAR = SI.WATT.alternate("var");
		VAR.alternate("Var");
		Unit<Power> VA = SI.WATT.alternate("VA");

		// add unit of measure aliases (to fix notation problems...)
		UnitFormat uf = UnitFormat.getInstance();
		uf.alias(SI.WATT.times(NonSI.HOUR), "Wh");
		uf.label(SI.KILO(SI.WATT.times(NonSI.HOUR)), "kWh");
		uf.alias(VAR.times(NonSI.HOUR), "Varh");
		uf.label(SI.KILO(VAR.times(NonSI.HOUR)), "kVarh");
		uf.label(SI.KILO(VA), "kVA");

		String activeEnergyUOM = SI.WATT.times(NonSI.HOUR).toString();
		// String reactiveEnergyUOM = VAR.times(NonSI.HOUR).toString();
		String activePowerUOM = SI.WATT.toString();
		// String reactivePowerUOM = VAR.toString();
		// String frequencyUOM = SI.HERTZ.toString();
		// String currentUOM = SI.AMPERE.toString();
		// String apparentPowerUOM = VA.toString();
		// String voltageUOM = SI.VOLT.toString();

		// ------------ Three Phase Active Power
		// --------------------------------
		ActivePowerStateValue activePowerStateL1 = new ActivePowerStateValue();
		activePowerStateL1.setFeature("phaseID", "L1");
		activePowerStateL1.setValue(DecimalMeasure.valueOf("0 "
				+ activePowerUOM));

		ActivePowerStateValue activePowerStateL2 = new ActivePowerStateValue();
		activePowerStateL2.setFeature("phaseID", "L2");
		activePowerStateL2.setValue(DecimalMeasure.valueOf("0 "
				+ activePowerUOM));

		ActivePowerStateValue activePowerStateL3 = new ActivePowerStateValue();
		activePowerStateL3.setFeature("phaseID", "L3");
		activePowerStateL3.setValue(DecimalMeasure.valueOf("0 "
				+ activePowerUOM));

		this.currentState.setState(ThreePhaseActivePowerMeasurementState.class
				.getSimpleName(), new ThreePhaseActivePowerMeasurementState(
				activePowerStateL1, activePowerStateL2, activePowerStateL3));
		// --------------------------------------------------------------------------

		// ------------- Frequency -----------------------------------------
		/*
		 * FrequencyStateValue frequencyStateValue = new FrequencyStateValue();
		 * frequencyStateValue.setValue(DecimalMeasure .valueOf("0 " +
		 * frequencyUOM)); this.currentState.setState(
		 * FrequencyMeasurementState.class.getSimpleName(), new
		 * FrequencyMeasurementState(frequencyStateValue));
		 */
		// -----------------------------------------------------------------

		// -------------- SinglePhaseActiveEnergy ---------------------------
		/*
		 * ActiveEnergyStateValue activeEnergyStateValue = new
		 * ActiveEnergyStateValue();
		 * activeEnergyStateValue.setValue(DecimalMeasure.valueOf("0 " +
		 * activeEnergyUOM)); this.currentState.setState(
		 * SinglePhaseActiveEnergyState.class.getSimpleName(), new
		 * SinglePhaseActiveEnergyState(activeEnergyStateValue));
		 */
		// ------------------------------------------------------------------

		// -------------- Three Phase Active Energy -------------------------
		// ------------ Three Phase Active Power
		// --------------------------------
		ActiveEnergyStateValue activeEnergyStateL1 = new ActiveEnergyStateValue();
		activeEnergyStateL1.setFeature("phaseID", "L1");
		activeEnergyStateL1.setValue(DecimalMeasure.valueOf("0 "
				+ activeEnergyUOM));

		ActiveEnergyStateValue activeEnergyStateL2 = new ActiveEnergyStateValue();
		activeEnergyStateL2.setFeature("phaseID", "L2");
		activeEnergyStateL2.setValue(DecimalMeasure.valueOf("0 "
				+ activeEnergyUOM));

		ActiveEnergyStateValue activeEnergyStateL3 = new ActiveEnergyStateValue();
		activeEnergyStateL3.setFeature("phaseID", "L3");
		activeEnergyStateL3.setValue(DecimalMeasure.valueOf("0 "
				+ activeEnergyUOM));

		this.currentState.setState(ThreePhaseActiveEnergyState.class
				.getSimpleName(), new ThreePhaseActiveEnergyState(
				activeEnergyStateL1, activeEnergyStateL2, activeEnergyStateL3));

		// -------------- ThreePhaseCurrent ---------------------------------
		/*
		 * CurrentStateValue currentStateL1 = new CurrentStateValue();
		 * currentStateL1.setFeature("phaseID", "L1");
		 * currentStateL1.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		 * 
		 * CurrentStateValue currentStateL2 = new CurrentStateValue();
		 * currentStateL2.setFeature("phaseID", "L2");
		 * currentStateL2.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		 * 
		 * CurrentStateValue currentStateL3 = new CurrentStateValue();
		 * currentStateL3.setFeature("phaseID", "L3");
		 * currentStateL3.setValue(DecimalMeasure.valueOf("0 " + currentUOM));
		 * 
		 * this.currentState.setState(
		 * ThreePhaseCurrentState.class.getSimpleName(), new
		 * ThreePhaseCurrentState(currentStateL1, currentStateL2,
		 * currentStateL3));
		 */
		// -------------------------------------------------------------------

		// ------------- ThreePhaseReactivePower -----------------------------
		/*
		 * ReactivePowerStateValue reactivePowerStateL1 = new
		 * ReactivePowerStateValue(); reactivePowerStateL1.setFeature("phaseID",
		 * "L1"); reactivePowerStateL1.setValue(DecimalMeasure.valueOf("0 " +
		 * reactivePowerUOM));
		 * 
		 * ReactivePowerStateValue reactivePowerStateL2 = new
		 * ReactivePowerStateValue(); reactivePowerStateL2.setFeature("phaseID",
		 * "L2"); reactivePowerStateL2.setValue(DecimalMeasure.valueOf("0 " +
		 * reactivePowerUOM));
		 * 
		 * ReactivePowerStateValue reactivePowerStateL3 = new
		 * ReactivePowerStateValue(); reactivePowerStateL3.setFeature("phaseID",
		 * "L3"); reactivePowerStateL3.setValue(DecimalMeasure.valueOf("0 " +
		 * reactivePowerUOM));
		 * 
		 * this.currentState.setState(
		 * ThreePhaseReactivePowerMeasurementState.class.getSimpleName(), new
		 * ThreePhaseReactivePowerMeasurementState( reactivePowerStateL1,
		 * reactivePowerStateL2, reactivePowerStateL3));
		 */
		// --------------------------------------------------------------------

		// ---------------------- Power Factor --------------------------------
		/*
		 * PowerFactorStateValue powerFactorStateValue = new
		 * PowerFactorStateValue();
		 * powerFactorStateValue.setValue(DecimalMeasure.valueOf("0 " +
		 * Unit.ONE));
		 * 
		 * this.currentState.setState(
		 * PowerFactorMeasurementState.class.getCanonicalName(), new
		 * PowerFactorMeasurementState(powerFactorStateValue));
		 */
		// --------------------------------------------------------------------

		// ---------------------- ThreePhaseApparentPower ---------------------
		/*
		 * ApparentPowerStateValue apparentPowerStateL1 = new
		 * ApparentPowerStateValue(); apparentPowerStateL1.setFeature("phaseID",
		 * "L1"); apparentPowerStateL1.setValue(DecimalMeasure.valueOf("0 " +
		 * apparentPowerUOM));
		 * 
		 * ApparentPowerStateValue apparentPowerStateL2 = new
		 * ApparentPowerStateValue(); apparentPowerStateL2.setFeature("phaseID",
		 * "L2"); apparentPowerStateL2.setValue(DecimalMeasure.valueOf("0 " +
		 * apparentPowerUOM));
		 * 
		 * ApparentPowerStateValue apparentPowerStateL3 = new
		 * ApparentPowerStateValue(); apparentPowerStateL3.setFeature("phaseID",
		 * "L3"); apparentPowerStateL3.setValue(DecimalMeasure.valueOf("0 " +
		 * apparentPowerUOM));
		 * 
		 * this.currentState.setState(
		 * ThreePhaseApparentPowerMeasurementState.class.getSimpleName(), new
		 * ThreePhaseApparentPowerMeasurementState( apparentPowerStateL1,
		 * apparentPowerStateL2, apparentPowerStateL3));
		 */
		// ----------------------------------------------------------------------

		// ------------------------ Single Phase Reactive Energy
		/*
		 * ReactiveEnergyStateValue reactiveEnergyStateValue = new
		 * ReactiveEnergyStateValue();
		 * reactiveEnergyStateValue.setValue(DecimalMeasure.valueOf("0 " +
		 * reactiveEnergyUOM)); this.currentState.setState(
		 * SinglePhaseReactiveEnergyState.class.getSimpleName(), new
		 * SinglePhaseReactiveEnergyState(reactiveEnergyStateValue));
		 */
		// -----------------------------------------------------------------------

		// ------------------------ ThreePhaseLNVoltage
		/*
		 * VoltageStateValue voltageStateL1N = new VoltageStateValue();
		 * voltageStateL1N.setFeature("phaseID", "L1");
		 * voltageStateL1N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * VoltageStateValue voltageStateL2N = new VoltageStateValue();
		 * voltageStateL2N.setFeature("phaseID", "L2");
		 * voltageStateL2N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * VoltageStateValue voltageStateL3N = new VoltageStateValue();
		 * voltageStateL3N.setFeature("phaseID", "L3");
		 * voltageStateL3N.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * this.currentState.setState(
		 * ThreePhaseVoltageState.class.getSimpleName(), new
		 * ThreePhaseVoltageState(voltageStateL1N, voltageStateL2N,
		 * voltageStateL3N));
		 */
		// ----------------------------------------------------------

		// ------------------------ ThreePhaseLLVoltage
		/*
		 * VoltageStateValue voltageStateL12 = new VoltageStateValue();
		 * voltageStateL12.setFeature("phaseID", "L12");
		 * voltageStateL12.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * VoltageStateValue voltageStateL23 = new VoltageStateValue();
		 * voltageStateL23.setFeature("phaseID", "L23");
		 * voltageStateL23.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * VoltageStateValue voltageStateL31 = new VoltageStateValue();
		 * voltageStateL31.setFeature("phaseID", "L31");
		 * voltageStateL31.setValue(DecimalMeasure.valueOf("0 " + voltageUOM));
		 * 
		 * this.currentState.setState(ThreePhaseVoltageState.class.getSimpleName(
		 * ) + "LL", new ThreePhaseVoltageState(voltageStateL12,
		 * voltageStateL23, voltageStateL31));
		 */

		// get the initial state of the device
		Runnable worker = new Runnable()
		{
			public void run()
			{
				network.read(nodeInfo, true);
			}
		};

		Thread workerThread = new Thread(worker);
		workerThread.start();

	}

	@Override
	public DeviceStatus getState()
	{
		return currentState;
	}

	@Override
	public void newMessageFromHouse(Device deviceNode, Instance instanceNode,
			Controller controllerNode, String sValue)
	{
		this.deviceNode = deviceNode;

		// Read the value associated with the right CommandClass
		// instance 0 doesn't contains sensor data
		if (instanceNode.getInstanceId() != 0)
		{
			// meter values are in
			// devices.X.instances.1.commandClasses.50.data.0 (KwH) and
			// devices.X.instances.1.commandClasses.50.data.2 (W)
			CommandClasses ccElectricityEntry = instanceNode
					.getCommandClasses().get(ZWaveAPI.COMMAND_CLASS_METER);

			// tin pants...
			if (ccElectricityEntry != null)
			{
				// prevent errors on first inclusion
				DataElemObject data0 = ccElectricityEntry.get("0");
				if (data0 != null)
				{
					Object data0Value = data0.getDataElemValue("val");
					if (data0Value != null)
					{
						double activeEnergy = Double.valueOf(data0Value
								.toString());
						notifyNewActiveEnergyValue(
								"L" + instanceNode.getInstanceId(),
								DecimalMeasure.valueOf(activeEnergy + " "
										+ SI.KILO(SI.WATT.times(NonSI.HOUR))));
					}
				}

				// prevent errors on first inclusion
				DataElemObject data2 = ccElectricityEntry.get("2");
				if (data2 != null)
				{
					Object data2Value = data2.getDataElemValue("val");
					if (data2Value != null)
					{
						double activePower = Double.valueOf(data2Value
								.toString());
						notifyNewActivePowerValue(
								"L" + instanceNode.getInstanceId(),
								DecimalMeasure.valueOf(activePower + " "
										+ SI.WATT));
					}
				}
			}
		}
	}

	@Override
	protected void specificConfiguration()
	{
		// prepare the device state map
		currentState = new DeviceStatus(device.getDeviceId());
	}

	@Override
	protected void addToNetworkDriver(ZWaveNodeInfo nodeInfo)
	{
		network.addDriver(nodeInfo, updateTimeMillis, this);
	}

	@Override
	protected boolean isController()
	{
		return false;
	}

	@Override
	protected ZWaveNodeInfo createNodeInfo(int deviceId,
			Set<Integer> instancesId, boolean isController)
	{
		HashMap<Integer, Set<Integer>> instanceCommand = new HashMap<Integer, Set<Integer>>();

		// meter information are in instance 1 with commandclasses =
		// COMMAND_CLASS_METER (kwH) and COMMAND_CLASS_SENSOR_MULTILEVEL (W)

		for (Integer instanceId : instancesId)
		{
			HashSet<Integer> ccSet = new HashSet<Integer>();
			ccSet.add(ZWaveAPI.COMMAND_CLASS_METER);
			ccSet.add(ZWaveAPI.COMMAND_CLASS_SENSOR_MULTILEVEL);
			instanceCommand.put(instanceId, ccSet);
		}
		ZWaveNodeInfo nodeInfo = new ZWaveNodeInfo(deviceId, instanceCommand,
				isController);

		return nodeInfo;
	}

	@Override
	public Measure<?, ?> getReactiveEnergyValue()
	{
		return (Measure<?, ?>) this.currentState.getState(
				SinglePhaseReactiveEnergyState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getReactivePower(String phaseID)
	{
		return this.getThreePhaseStateValue(
				ThreePhaseReactivePowerMeasurementState.class.getSimpleName(),
				phaseID);
	}

	@Override
	public Measure<?, ?> getFrequency()
	{
		return (Measure<?, ?>) this.currentState.getState(
				FrequencyMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getPowerFactor()
	{
		return (Measure<?, ?>) this.currentState.getState(
				PowerFactorMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getActiveEnergyValue()
	{
		return (Measure<?, ?>) this.currentState.getState(
				SinglePhaseActiveEnergyState.class.getSimpleName())
				.getCurrentStateValue()[0].getValue();
	}

	@Override
	public Measure<?, ?> getLNVoltageValue(String phaseID)
	{
		return this.getThreePhaseStateValue(
				ThreePhaseVoltageState.class.getSimpleName(), phaseID);
	}

	@Override
	public Measure<?, ?> getLLVoltageValue(String phaseID1, String phaseID2)
	{
		// TODO: fix this....
		String phaseID = phaseID1 + phaseID2.substring(1);

		return this.getThreePhaseStateValue(
				ThreePhaseVoltageState.class.getSimpleName() + "LL", phaseID);
	}

	@Override
	public Measure<?, ?> getElectricCurrentValue(String phaseID)
	{
		return this.getThreePhaseStateValue(
				ThreePhaseCurrentState.class.getSimpleName(), phaseID);
	}

	@Override
	public Measure<?, ?> getApparentPower(String phaseID)
	{
		return this.getThreePhaseStateValue(
				ThreePhaseApparentPowerMeasurementState.class.getSimpleName(),
				phaseID);
	}

	@Override
	public Measure<?, ?> getActivePower(String phaseID)
	{
		return this.getThreePhaseStateValue(
				ThreePhaseActivePowerMeasurementState.class.getSimpleName(),
				phaseID);
	}

	@Override
	public void notifyNewFrequencyValue(Measure<?, ?> frequency)
	{
		// update the state
		((FrequencyMeasurementState) this.currentState
				.getState(FrequencyMeasurementState.class.getSimpleName()))
				.getCurrentStateValue()[0].setValue(frequency);

		// notify the new measure
		((ThreePhaseElectricityMeter) this.device)
				.notifyNewFrequencyValue(frequency);

	}

	@Override
	public void notifyStateChanged(State newState)
	{
		// probably unused...
		((ThreePhaseElectricityMeter) this.device).notifyStateChanged(newState);

	}

	@Override
	public void notifyNewReactivePowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(
				ThreePhaseReactivePowerMeasurementState.class.getSimpleName(),
				phaseID, value);

		((ThreePhaseElectricityMeter) this.device).notifyNewReactivePowerValue(
				phaseID, value);

	}

	@Override
	public void notifyNewReactiveEnergyValue(Measure<?, ?> value)
	{
		// update the state
		this.currentState.getState(
				SinglePhaseReactiveEnergyState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(value);

		// notify the new measure
		((ThreePhaseElectricityMeter) this.device)
				.notifyNewReactiveEnergyValue(value);

	}

	@Override
	public void notifyNewActiveEnergyValue(Measure<?, ?> value)
	{
		// update the state
		((SinglePhaseActiveEnergyState) this.currentState
				.getState(SinglePhaseActiveEnergyState.class.getSimpleName()))
				.getCurrentStateValue()[0].setValue(value);

		// notify the new measure
		((ThreePhaseElectricityMeter) this.device)
				.notifyNewActiveEnergyValue(value);
	}

	public void notifyNewActiveEnergyValue(String phaseID,
			DecimalMeasure<Quantity> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(
				ThreePhaseActiveEnergyState.class.getSimpleName(), phaseID,
				value);

		// TODO: to be implemented

		/*
		 * ((ThreePhaseElectricityMeter) this.device).notifyNewActivePowerValue(
		 * phaseID, value);
		 */

	}

	@Override
	public void notifyNewPhaseNeutralVoltageValue(String phaseID,
			Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(
				ThreePhaseVoltageState.class.getSimpleName(), phaseID, value);

		((ThreePhaseElectricityMeter) this.device)
				.notifyNewPhaseNeutralVoltageValue(phaseID, value);

	}

	@Override
	public void notifyNewPhasePhaseVoltageValue(String phaseID,
			Measure<?, ?> value)
	{

		this.updateThreePhaseStateValue(
				ThreePhaseVoltageState.class.getSimpleName() + "LL", phaseID,
				value);

		((ThreePhaseElectricityMeter) this.device)
				.notifyNewPhasePhaseVoltageValue(phaseID, value);

	}

	@Override
	public void notifyNewApparentPowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....

		this.updateThreePhaseStateValue(
				ThreePhaseApparentPowerMeasurementState.class.getSimpleName(),
				phaseID, value);

		((ThreePhaseElectricityMeter) this.device).notifyNewApparentPowerValue(
				phaseID, value);

	}

	@Override
	public void notifyNewPowerFactorValue(Measure<?, ?> powerFactor)
	{
		// update the state
		this.currentState.getState(
				PowerFactorMeasurementState.class.getSimpleName())
				.getCurrentStateValue()[0].setValue(powerFactor);

		// notify the new measure
		((ThreePhaseElectricityMeter) this.device)
				.notifyNewPowerFactorValue(powerFactor);

	}

	@Override
	public void notifyNewActivePowerValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(
				ThreePhaseActivePowerMeasurementState.class.getSimpleName(),
				phaseID, value);

		((ThreePhaseElectricityMeter) this.device).notifyNewActivePowerValue(
				phaseID, value);
	}

	@Override
	public void notifyNewCurrentValue(String phaseID, Measure<?, ?> value)
	{
		// update the state....
		this.updateThreePhaseStateValue(
				ThreePhaseCurrentState.class.getSimpleName(), phaseID, value);

		((ThreePhaseElectricityMeter) this.device).notifyNewCurrentValue(
				phaseID, value);

	}

	private void updateThreePhaseStateValue(String stateClass, String phaseID,
			Measure<?, ?> value)
	{
		// update the state....
		StateValue currentStateValue[] = ((State) this.currentState
				.getState(stateClass)).getCurrentStateValue();
		for (int i = 0; i < currentStateValue.length; i++)
		{
			// find the matching state value
			HashMap<String, Object> features = currentStateValue[i]
					.getFeatures();

			if ((features.containsKey("phaseID"))
					&& (((String) features.get("phaseID"))
							.equalsIgnoreCase(phaseID)))
			{
				// set the new value
				currentStateValue[i].setValue(DecimalMeasure.valueOf(value
						.toString()));
			}
		}
	}

	private Measure<?, ?> getThreePhaseStateValue(String stateClass,
			String phaseID)
	{
		// the measure
		Measure<?, ?> value = null;

		// get the current state value
		StateValue currentStateValue[] = ((State) this.currentState
				.getState(stateClass)).getCurrentStateValue();

		// check which state value matches the given phase ID
		for (int i = 0; i < currentStateValue.length; i++)
		{
			// find the matching state value
			HashMap<String, Object> features = currentStateValue[i]
					.getFeatures();

			// if matches, extract the power value and break the cycle
			if ((features.containsKey("phaseID"))
					&& (((String) features.get("phaseID"))
							.equalsIgnoreCase(phaseID)))
			{
				// extract the power value
				value = (Measure<?, ?>) currentStateValue[i].getValue();

				// break
				i = currentStateValue.length;
			}
		}
		return value;
	}

}