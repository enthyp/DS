package ice.device

import SmartHome.BaseDevice
import SmartHome.DeviceState
import SmartHome.Info
import com.zeroc.Ice.Current

open class BaseDeviceI(private val name: String) : BaseDevice {

    protected var isOn = false
    override fun state(current: Current?): DeviceState {
        return when(isOn) {
            true -> DeviceState.ON
            false -> DeviceState.OFF
        }
    }

    override fun name(current: Current?): String {
        return this.name
    }

    override fun turnOff(current: Current?) {
        isOn = false
    }

    override fun turnOn(current: Current?) {
        isOn = true
    }
}

class InfoI(private val devices: MutableMap<String, Array<String>>) : Info {
    override fun getDevices(current: Current?): MutableMap<String, Array<String>> = devices
}
