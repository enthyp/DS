package ice.device

import SmartHome.BaseError
import SmartHome.Temperature.Thermostat
import SmartHome.ValueError
import com.zeroc.Ice.Current

val TEMPERATURE_RANGE = 15.0f..25.0f

class ThermostatI(private val name: String) : Thermostat, BaseDeviceI(name) {

    private var temperature: Float = 21.0f

    override fun setTemperature(temperature: Float, current: Current?) {
        if (isOn) {
            if (temperature in TEMPERATURE_RANGE) {
                this.temperature = temperature
                println("Set temperature = $temperature for $name")
            } else {
                throw ValueError("Value out of range!", 15.0f, 25.0f)
            }
        } else {
            throw BaseError("Thermostat is off.")
        }
    }

    override fun getTemperature(current: Current?): Float {
        println("Get temperature for $name")
        return this.temperature
    }
}
