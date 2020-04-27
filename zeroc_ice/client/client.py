import Ice, sys
import SmartHome
from execute import *

host, port = '192.168.100.106', '10000'


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
        try:
            return proxy.checkedCast(obj)
        except Ice.ObjectNotExistException:
            return None

    @staticmethod
    def proxy_string(name, category):
        string = f"{name}:default -h {host} -p {port}"
        if category:
            string = category + '/' + string
        return string


# Parsing.
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
        try:
            if cmd == 'info':
                exec_info(provider)
            else:
                execute(state, provider)
        except Ice.Exception as e:
            print('ICE ERROR: ', e)
    else:
        print(f'Incorrect command: {cmd}')


if __name__ == '__main__':
    try:
        with Ice.initialize(sys.argv) as communicator:
            provider = Provider(communicator)
            while True:
                parse_cmd(provider)
    except KeyboardInterrupt:
        print('Signing out.')
