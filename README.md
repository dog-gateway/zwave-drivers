# Dog zwave-drivers
These are the Dog drivers that enable connection and management of Z-Wave devices.
Currently Dog supports Z-Wave by interacting with the REST API offered by the ZWay server bundled with the ZWave.me [RazBerry](http://razberry.z-wave.me/) adapter.

Similarly to all Dog drivers, the ZWave drivers need to be configured properly to effectively provide support for ZWave devices. A sample configuration can be found on the [zwave-configuration](https://github.com/dog-gateway/zwave-configuration) repository.

The main configuration filess required to succesfully operate z-wave drivers are the following:
* The main network driver configuration file
* The single device driver configuration files
* The device discovery db
* The zwave sections in the Dog environment configuration file

### Network driver configuration file
The file ```it.polito.elite.dog.drivers.zwave.network.cfg``` contains the configuration parameters shared by all zwave network drivers. These parameters include the definition of the baseline polling time, the time lapse between subsequent attempts to connect to a given ZWay server and the number of connection attempts that should be carried before "marking" the corresponding server as failed.

It is important to notice that, in the last version of the network driver, details about the ZWay server (gateway) to which connecting are no more inserted in this file, as multiple gateways are supported. Instead, the ZWay IP Address, port and other parameters are defined in the Dog environment configuration file.
```
# NETWORK CONFIGURATION
# ------------------------------

# Time between 2 subsequent polls of the Z-Wave network in milliseconds
pollingTimeMillis = 1000 

# Time between two consecutive tests of the connection to the house (after)
betweenTrialTimeMillis=30000

# the number of connection trials...
numTry=3
```
### Single driver configuration files
Every device driver in Dog can potentially be configured with specific parameters. Such a property is exploited in zwave-drivers to enable developers to configure different polling times for different devices. For example, in zwave, many sensors are battery-powered and pass most of their time sleeping (low power mode). It does not make sense to attempt polling a sleeping device too frequently as, in the better case, there will be no appreciable change in the obtained polling rate, while, in the worst case, once awaken the device will be forced to stay awake upon completion of all pending requests, thus resulting in abnormal battery draining.
A typical single driver configuration file contains one line, only, setting the desired polling time for the "represented" family of devices, i.e., all the dimmer lamps. A sample is reported below.

```
updateTimeMillis=1000
```

The only exception is the zwave-gateway configuration file where additional information for locating the device discovery db and the discovery specific parameters are provided. See an example below.

```
#######################################################################################
#
#   Gateway Configuration
#
#######################################################################################

# Time to wait before first access to new devices to perform auto-detection
waitBeforeDeviceInstall=2500

# Device database file
deviceDB=devicedb/zwave-devices.properties
```
### Device discovery database
Dog zwave-drivers, and in particular, the Dog zwave-gateway exploit a simple, file-based database of devices which maps real devices to corresponding Dog (and thus [DogOnt](http://iot-ontologies.github.io/dogont)) device classes. Real devices are identified by means of the unique triple of ```ManufacturerId(decimal)- Device type(decimal) - Device id (decimal)```. The provided device db can be easily estended by adding new lines with the same format ```<triple = Dog class>``` provided that an existing class is defined which can be mapped to the new device. For example, the full [set](https://github.com/OpenZWave/open-zwave/blob/master/config/manufacturer_specific.xml) of devices supported in [OpenZwave](https://github.com/OpenZWave) can be easily added, paying attention to the fact that in OpenZwave the above ids are represented as hexadecimal numbers.

A sample device db is reported below:

```
#######################################################################################
#
#   Supported Devices Database
#	(Last Update XXXX-XX-XX)
#
#######################################################################################

###########################
#### 2GIG Technologies ####
###########################

# CT30 Thermostat
# 152-7698-348=Thermostat
# CT100 Thermostat
# 152-25601-261=Thermostat


##############################
##### Aeotec / Aeon Labs #####
##############################

# Smart Energy Switch
134-3-6=MeteringPowerOutlet
# Home Energy Meter
134-2-9-4=ThreePhaseElectricityMeter
# Home Energy Meter
134-2-9=SinglePhaseElectricityMeter
# Smart Switch G2
134-3-18=MeteringPowerOutlet
# Micro Motor Controller
134-3-14=EnergyAndPowerMeteringLevelControllableOutput
# MultiSensor
134-2-5=QuadSensor

#####################
##### Everspring ####
#####################

# AN158
96-4-2=MeteringPowerOutlet
# AN145
96-4-1=LampHolder
# ST814
96-6-1=TemperatureAndHumiditySensor
# SP814
96-1-2=MovementSensor
# SM103 Door/Window Magnetic Sensor (To Check)
96-2-1=DoorSensor
96-514-1=DoorSensor
```
### Dog environment configuration file
Dog supports discovery of zwave-devices in the association / disassociation phase, only (support to discovery of already associated devices is planned in a near future). However, to successfully detect association/disassociation a connection towards one ore more ZWay servers shall be configured. This requires to manually add a "gateway" section in the Dog environment configuration files. A sample gateway configuration section is reported below.

```
<dhc:device class="ZWaveGateway" id="zwave-gateway" domoticSystem="ZWave">
            <dhc:description>The ZWave X gateway
			</dhc:description>
            <dhc:isIn>Office</dhc:isIn>
            <dhc:param name="IPAddress" value="http://xxx.xxx.xxx.xxx"/>
            <dhc:param name="port" value="8083"/>
            <dhc:param name="nodeId" value="1" type="network"/>
            <dhc:controlFunctionality class="AssociateFunctionality">
                <dhc:commands>
                    <dhc:command class="AssociateCommand" name="AssociateCommand_zwave-gateway" id="AssociateCommand_zwave-gateway">
                        <dhc:param name="realCommandName" value="associate"/>
                    </dhc:command>
                    <dhc:command class="DisassociateCommand" name="DisassociateCommand_zwave-gateway" id="DisassociateCommand_zwave-gateway">
                        <dhc:param name="realCommandName" value="disassociate"/>
                    </dhc:command>
                </dhc:commands>
            </dhc:controlFunctionality>
            <dhc:notificationFunctionality class="AssociationNotificationFunctionality">
                <dhc:notifications>
                    <dhc:notification class="AssociatingDeviceNotification" id="AssociatingDeviceNotification_zwave-gateway">
                        <dhc:param name="nParams" value="0"/>
                        <dhc:param name="notificationName" value="associating"/>
                    </dhc:notification>
                    <dhc:notification class="DisassociatingDeviceNotification" id="DisassociatingDeviceNotification_zwave-gateway">
                        <dhc:param name="nParams" value="0"/>
                        <dhc:param name="notificationName" value="disassociating"/>
                    </dhc:notification>
                </dhc:notifications>
            </dhc:notificationFunctionality>
        </dhc:device>
```

Once added, the gateway shall be restarted for the changes to become effective. All other zwave-device entries are created through association/disassociation time discovery.
