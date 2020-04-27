package ice.device

import SmartHome.AirCondition.AirConditioner
import SmartHome.AirCondition.Configuration
import SmartHome.AirCondition.HumidityAirConditioner
import SmartHome.AirCondition.Property
import SmartHome.BaseError
import SmartHome.ValueError
import com.zeroc.Ice.Current

fun validate(property: Property, value: Float) {
    when (property) {
        Property.TEMPERATURE ->
            if (value !in 15.0f..30.0f) throw ValueError("Value out of range!", 15.0f, 30.0f)
        Property.POWER ->
            if (value !in 0.0f..100.0f) throw ValueError("Value out of range!", 0.0f, 100.0f)
        Property.HUMIDITY ->
            if (value !in 0.0f..100.0f) throw ValueError("Value out of range!", 0.0f, 100.0f)
    }
}

open class AirConditionerI(private val name: String) : AirConditioner, BaseDeviceI(name) {

    protected val props = mutableMapOf(
        Property.TEMPERATURE to 21.0f,
        Property.POWER to 0.0f
    )

    override fun setConfig(config: Configuration, current: Current?) {
        // Validate whole config first.
        for ((prop, value) in config.props) {
            if (prop in props) {
                validate(prop, value)
            } else {
                throw BaseError("Invalid configuration property: $prop")
            }
        }

        // Set props.
        for ((prop, value) in config.props) {
            props[prop] = value
            println("Set $prop = $value for $name")
        }
    }

    override fun getConfig(current: Current?): Configuration {
        println("Get config for $name")
        return Configuration(props)
    }
}

class HumidityAirConditionerI(name: String) : HumidityAirConditioner, AirConditionerI(name) {
    init {
        this.props[Property.HUMIDITY] = 100.0f
    }
}
