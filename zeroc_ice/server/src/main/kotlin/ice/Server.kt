package ice

import com.zeroc.Ice.Util
import ice.device.ThermometerI

fun main(argv: Array<String>) {
    Util.initialize(argv).use { comm ->
        val adapter = comm.createObjectAdapterWithEndpoints("SmartHomeAdapter", "default -p 10000")
        // TODO: ServiceLocators!
        val thermometer1 = ThermometerI()
        val thermometer2 = ThermometerI()

        adapter.add(thermometer1, Util.stringToIdentity("Thermometer1"))
        adapter.add(thermometer2, Util.stringToIdentity("Thermometer2"))

        adapter.activate()
        println("Server running...")
        comm.waitForShutdown()
    }
}
