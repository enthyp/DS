package ice.device

import SmartHome.Temperature.Thermometer
import com.zeroc.Ice.Current
import kotlin.random.Random

class ThermometerI : Thermometer {
    override fun getTemperature(current: Current?): Float {
        val mean = Random.nextInt(23, 27)
        return mean + 2 * Random.nextFloat()
    }
}
