import Ice, sys
import SmartHome

with Ice.initialize(sys.argv) as communicator:
    obj_t1 = communicator.stringToProxy('Thermometer1:default -p 10000')
    obj_t2 = communicator.stringToProxy('Thermometer2:default -p 10000')

    t1 = SmartHome.Temperature.ThermometerPrx.checkedCast(obj_t1)
    t2 = SmartHome.Temperature.ThermometerPrx.checkedCast(obj_t2)

    print(t1.getTemperature())
    print(t2.getTemperature())
