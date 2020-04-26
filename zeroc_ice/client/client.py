import Ice, sys
from enum import Enum
from pprint import pprint
import SmartHome

host, port = '192.168.100.106', '10000'


class CmdState(Enum):
    INFO = 0
    TEMP = 1
    AC = 2
    HAC = 3


# Wrapper for getting stub instances.
class Provider:

    state_map = {
        CmdState.INFO: ('', SmartHome.InfoPrx),
        CmdState.TEMP: ('temp', SmartHome.Temperature.ThermostatPrx),
        CmdState.AC: ('ac', SmartHome.AirCondition.AirConditionerPrx),
        CmdState.HAC: ('hac', SmartHome.AirCondition.HumidityAirConditionerPrx),
    }

    def __init__(self, communicator):
        self.communicator = communicator

    def get_stub(self, state, name):
        category, proxy = self.state_map[state]
        prx_str = self.proxy_string(name, category)
        obj = self.communicator.stringToProxy(prx_str)
        return proxy.checkedCast(obj)

    @staticmethod
    def proxy_string(name, category):
        string = f"{name}:default -h {host} -p {port}"
        if category:
            string = category + '/' + string
        return string


# Actual execution.
def exec_info(provider):
    stub = provider.get_stub(CmdState.INFO, 'info')
    devices = stub.getDevices()
    print('Devices available: ')
    pprint(devices)


def exec_temp():
    pass


def exec_ac():
    pass


def exec_hac():
    pass


# Parsing related.

def handle(state, provider):
    handler_name = 'exec_' + state.name.lower()
    handler = globals()[handler_name]
    handler(provider)


state_map = {
    'info': CmdState.INFO,
    'temp': CmdState.TEMP,
    'ac': CmdState.AC,
    'hac': CmdState.HAC
}


def parse_cmd(provider):
    cmd = input('> ').strip()
    state = state_map.get(cmd, None)

    if state:
        handle(state, provider)
    else:
        print(f'Incorrect command: {input}')
    return

    obj_info = communicator.stringToProxy("info:default -h 192.168.100.1 -p 10000")
    obj_t1 = communicator.stringToProxy('temp/kitchen:default -p 10000')
    obj_t2 = communicator.stringToProxy('temp/bathroom:default -p 10000')

    obj_ac = communicator.stringToProxy('ac/kitchen:default -p 10000')
    obj_hac = communicator.stringToProxy('hac/basement:default -p 10000')

    info = SmartHome.InfoPrx.checkedCast(obj_info)
    t1 = SmartHome.Temperature.ThermostatPrx.checkedCast(obj_t1)
    t2 = SmartHome.Temperature.ThermostatPrx.checkedCast(obj_t2)
    ac = SmartHome.AirCondition.AirConditionerPrx.checkedCast(obj_ac)
    hac = SmartHome.AirCondition.HumidityAirConditionerPrx.checkedCast(obj_hac)

    print(info.getDevices())
    print(t1.getTemperature())
    t1.turnOn()
    t1.setTemperature(20)
    print(t1.getTemperature())
    print(t2.getTemperature())
    ac.turnOn()
    hac.turnOff()

    print(ac.getConfig())
    props = {
    }
    ac.setConfig(SmartHome.AirCondition.Configuration(props))
    print(ac.getConfig())


if __name__ == '__main__':
    try:
        with Ice.initialize(sys.argv) as communicator:
            provider = Provider(communicator)
            while True:
                parse_cmd(provider)
    except KeyboardInterrupt:
        print('Signing out.')
