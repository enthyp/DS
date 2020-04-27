package ice.device

import SmartHome.BaseDevice
import SmartHome.DeviceState
import SmartHome.Info
import com.zeroc.Ice.Current

open class BaseDeviceI(private val name: String) : BaseDevice {

    protected var isOn = false
    override fun state(current: Current?): DeviceState {
        println("Get state for $name")
        return when(isOn) {
            true -> DeviceState.ON
            false -> DeviceState.OFF
        }
    }

    override fun name(current: Current?): String {
        println("Get name for $name")
        return this.name
    }

    override fun turnOff(current: Current?) {
        println("Turn off $name")
        isOn = false
    }

    override fun turnOn(current: Current?) {
        println("Turn on $name")
        isOn = true
    }
}

class InfoI(private val devices: MutableMap<String, Array<String>>) : Info {
    override fun getDevices(current: Current?): MutableMap<String, Array<String>> {
        println("Get devices")
        return devices
    }
}
