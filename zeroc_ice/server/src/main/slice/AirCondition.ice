#include <Base.ice>
[["python:pkgdir:SmartHome"]]
module SmartHome {

    module AirCondition {
        enum Property {
            TEMPERATURE,
            POWER,
            HUMIDITY,
            DIRECTION
        }

        dictionary<Property, float> Properties;

        class Configuration extends DeviceConfiguration {
            Properties props;
        }

        // TODO: errors for incorrect values + operation when deactivated
        // Ordinary air conditioner
        interface AirConditioner {
            idempotent void on() throws RuntimeError;
            idempotent void off() throws RuntimeError;
            idempotent Configuration getConfig();
            idempotent void setConfig(Configuration config) throws RuntimeError;
        }

        // Air conditioner with humidity regulation
        interface HumidityAirConditioner extends AirConditioner {}

        // Air conditioner with adjustable airflow direction
        interface DirectableAirConditioner extends AirConditioner {}
    }
}
