package ice.device

import SmartHome.AirCondition.AirConditioner
import SmartHome.AirCondition.Configuration
import SmartHome.AirCondition.HumidityAirConditioner
import SmartHome.AirCondition.Property
import SmartHome.RuntimeError
import com.zeroc.Ice.Current

open class AirConditionerI : AirConditioner {

    companion object {
        const val TEMP_MIN = 15.0
        const val TEMP_MAX = 30.0
        const val POWER_MIN = 0.0
        const val POWER_MAX = 1.0
    }

    protected var temperature: Float? = null
    protected var power: Float? = null
    protected var isOn = false

    override fun on(current: Current?) {
        isOn = true
    }

    override fun off(current: Current?) {
        isOn = false
    }

    override fun setConfig(config: Configuration, current: Current?) {
        // Validate temperature
        val temperature = config.props[Property.TEMPERATURE]
        temperature?.let { t ->
            if (t in TEMP_MIN..TEMP_MAX) {
                this.temperature = t
            } else {
                throw RuntimeError()
            }
        }

        // Validate power
        val power = config.props[Property.POWER]
        power?.let { p ->
            if (p in POWER_MIN..POWER_MAX) {
                this.power = p
            } else {
                throw RuntimeError()
            }
        }
    }

    override fun getConfig(current: Current?): Configuration {
        val props = mutableMapOf<Property, Float?>()
        temperature?.let { props[Property.TEMPERATURE] = temperature }
        power?.let { props[Property.POWER] = power }

        return Configuration(props)
    }
}

class HumidityAirConditionerI : HumidityAirConditioner, AirConditionerI() {
    companion object {
        const val HUMIDITY_MIN = 0.0
        const val HUMIDITY_MAX = 1.0
    }

    protected var humidity: Float? = null

    override fun setConfig(config: Configuration, current: Current?) {
        super.setConfig(config, current)

        // Validate humidity
        val humidity = config.props[Property.HUMIDITY]
        humidity?.let { h ->
            if (h in HUMIDITY_MIN..HUMIDITY_MAX) {
                this.humidity = h
            } else {
                throw RuntimeError()
            }
        }
    }

    override fun getConfig(current: Current?): Configuration {
        val config = super.getConfig(current)
        humidity?.let { config.props[Property.HUMIDITY] = humidity }

        return config
    }
}
