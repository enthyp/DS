#include <Base.ice>
[["python:pkgdir:SmartHome"]]
module SmartHome {
    
    module Temperature {

        interface Thermometer {
            float getTemperature();
        }
    }
}
