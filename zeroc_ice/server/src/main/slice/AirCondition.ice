#include <Base.ice>
[["python:pkgdir:SmartHome"]]
module SmartHome {

    module AirCondition {

        // Air conditioner configuration
        enum Property {
            TEMPERATURE,
            POWER,
            HUMIDITY
        }

        dictionary<Property, float> Properties;

        class Configuration extends DeviceConfiguration {
            Properties props;
        }

        // Ordinary air conditioner
        interface AirConditioner extends BaseDevice {
            idempotent Configuration getConfig();
            idempotent void setConfig(Configuration config) throws BaseError;
        }

        // Air conditioner with humidity regulation
        interface HumidityAirConditioner extends AirConditioner {}
    }
}
