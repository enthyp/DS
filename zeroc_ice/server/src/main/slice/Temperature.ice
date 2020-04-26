#include <Base.ice>
[["python:pkgdir:SmartHome"]]
module SmartHome {
    
    module Temperature {

        interface Thermostat extends BaseDevice {
            void setTemperature(float temperature) throws BaseError;
            float getTemperature();
        }
    }
}
