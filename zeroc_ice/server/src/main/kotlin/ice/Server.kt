package ice

import com.zeroc.Ice.Util
import ice.device.AirConditionerI
import ice.device.HumidityAirConditionerI
import ice.device.ThermometerI

val thermometers = setOf("kitchen", "bathroom")
val airConditioners = setOf("kitchen")
val humidityAirConditioners = setOf("bathroom", "basement")


fun main(argv: Array<String>) {
    Util.initialize(argv).use { comm ->
        val adapter = comm.createObjectAdapterWithEndpoints("SmartHomeAdapter", "default -p 10000")

        val tempLocator = Locator(thermometers) { ThermometerI() }
        val acLocator = Locator(airConditioners) { AirConditionerI() }
        val hacLocator = Locator(humidityAirConditioners) { HumidityAirConditionerI() }

        adapter.addServantLocator(tempLocator, "temp")
        adapter.addServantLocator(acLocator, "ac")
        adapter.addServantLocator(hacLocator, "hac")

        adapter.activate()

        println("Server running...")
        comm.waitForShutdown()
    }
}
