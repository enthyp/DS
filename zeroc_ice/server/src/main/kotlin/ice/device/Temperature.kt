package ice.device

import SmartHome.BaseError
import SmartHome.Temperature.Thermostat
import SmartHome.ValueError
import com.zeroc.Ice.Current
import kotlin.random.Random

val TEMPERATURE_RANGE = 15.0f..25.0f

class ThermostatI(name: String) : Thermostat, BaseDeviceI(name) {

    private var temperature: Float? = null

    override fun setTemperature(temperature: Float, current: Current?) {
        if (isOn) {
            if (temperature in TEMPERATURE_RANGE) {
                this.temperature = temperature
            } else {
                throw ValueError("Value out of range!", 15.0f, 25.0f)
            }
        } else {
            throw BaseError("Thermostat is off.")
        }
    }

    override fun getTemperature(current: Current?): Float {
        if (temperature == null) {
            temperature = Random.nextInt(23, 27).toFloat()
        }
        return this.temperature!! + 2 * Random.nextFloat()
    }
}
