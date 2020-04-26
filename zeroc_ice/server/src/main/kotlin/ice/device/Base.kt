package ice.device

import SmartHome.Info
import com.zeroc.Ice.Current

class InfoI(private val devices: MutableMap<String, Array<String>>) : Info {
    override fun getDevices(current: Current?): MutableMap<String, Array<String>> = devices
}
