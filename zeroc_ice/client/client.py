import Ice, sys
import SmartHome

with Ice.initialize(sys.argv) as communicator:
    obj_info = communicator.stringToProxy("info:default -p 10000")
    obj_t1 = communicator.stringToProxy('temp/kitchen:default -p 10000')
    obj_t2 = communicator.stringToProxy('temp/bathroom:default -p 10000')

    obj_ac = communicator.stringToProxy('ac/kitchen:default -p 10000')
    obj_hac = communicator.stringToProxy('hac/basement:default -p 10000')

    info = SmartHome.InfoPrx.checkedCast(obj_info)
    t1 = SmartHome.Temperature.ThermometerPrx.checkedCast(obj_t1)
    t2 = SmartHome.Temperature.ThermometerPrx.checkedCast(obj_t2)
    ac = SmartHome.AirCondition.AirConditionerPrx.checkedCast(obj_ac)
    hac = SmartHome.AirCondition.HumidityAirConditionerPrx.checkedCast(obj_hac)

    print(info.getDevices())
    print(t1.getTemperature())
    print(t2.getTemperature())
    ac.on()
    hac.off()



    print(ac.getConfig())
    props = {
        SmartHome.AirCondition.Property.TEMPERATURE: 20
    }
    ac.setConfig(SmartHome.AirCondition.Configuration(props))
    print(ac.getConfig())
