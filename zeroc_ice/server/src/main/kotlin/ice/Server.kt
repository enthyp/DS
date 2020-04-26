package ice

import com.zeroc.Ice.Util
import ice.device.PrinterI

fun main(argv: Array<String>) {
    Util.initialize(argv).use { comm ->
        val adapter = comm.createObjectAdapterWithEndpoints("SimpleSmartHomeAdapter", "default -p 10000")
        val printer = PrinterI()
        adapter.add(printer, Util.stringToIdentity("SimplePrinter"))
        adapter.activate()
        comm.waitForShutdown()
    }
}
