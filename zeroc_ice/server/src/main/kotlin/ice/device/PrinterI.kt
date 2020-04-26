package ice.device

import com.zeroc.Ice.Current

class PrinterI : SmartHome.Printer {
    override fun printString(s: String?, current: Current?) {
        println(s)
    }
}
