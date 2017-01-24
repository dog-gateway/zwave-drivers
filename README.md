# Dog zwave-drivers
These are the Dog drivers that enable connection and management of Z-Wave devices.
Currently Dog supports Z-Wave by interacting with the REST API offered by the ZWay server bundled with the ZWave.me [RazBerry](http://razberry.z-wave.me/) adapter.

Similarly to all Dog drivers, the ZWave drivers need to be configured properly to effectively provide support for ZWave devices. A sample configuration can be found on the [zwave-configuration](https://github.com/dog-gateway/zwave-configuration) repository.

The main configuration filess required to succesfully operate z-wave drivers are the following:
* The main network driver configuration file
* The single device driver configuration files
* The device discovery db files and folder
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
