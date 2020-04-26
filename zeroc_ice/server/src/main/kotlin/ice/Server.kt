package ice

import com.zeroc.Ice.Util
import ice.device.AirConditionerI
import ice.device.HumidityAirConditionerI
import ice.device.InfoI
import ice.device.ThermometerI

val devices = mutableMapOf(
    "thermometers" to arrayOf("kitchen", "bathroom"),
    "airConditioners" to arrayOf("kitchen"),
    "humidityAirConditioners" to arrayOf("bathroom", "basement")
)

fun main(argv: Array<String>) {
    Util.initialize(argv).use { comm ->
        val adapter = comm.createObjectAdapterWithEndpoints("SmartHomeAdapter", "default -p 10000")

        val info = InfoI(devices)
        adapter.add(info, Util.stringToIdentity("info"))

        val tempLocator = Locator(devices["thermometers"]!!) { ThermometerI() }
        val acLocator = Locator(devices["airConditioners"]!!) { AirConditionerI() }
        val hacLocator = Locator(devices["humidityAirConditioners"]!!) { HumidityAirConditionerI() }

        adapter.addServantLocator(tempLocator, "temp")
        adapter.addServantLocator(acLocator, "ac")
        adapter.addServantLocator(hacLocator, "hac")

        adapter.activate()

        println("Server running...")
        comm.waitForShutdown()
    }
}
