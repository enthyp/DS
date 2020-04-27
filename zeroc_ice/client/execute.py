from enum import Enum
from pprint import pprint
import SmartHome

__all__ = ['CmdState', 'exec_info', 'execute']


class CmdState(Enum):
    INFO = 0
    TEMP = 1
    AC = 2
    HAC = 3


def exec_info(provider):
    stub = provider.get_stub(CmdState.INFO, 'info')
    devices = stub.getDevices()
    print('Devices available: ')
    pprint(devices)


def execute(state, provider):
    name = input('> name: ').strip()
    stub = provider.get_stub(state, name)
    if not stub:
        print('No such device!')
        return

    while True:
        try:
            cmd = input('> cmd: ').strip()
            if cmd == 'exit':
                break
            if state == CmdState.TEMP:
                exec_temp(cmd, stub)
            elif state == CmdState.AC:
                exec_ac(cmd, stub)
            else:
                exec_hac(cmd, stub)
        except SmartHome.ValueError as e:
            print(e.reason)
            print(f'Valid range: {e._from} to {e.to}')
        except SmartHome.BaseError as e:
            print(e.reason)


def exec_temp(cmd, stub):
    if cmd == 'get':
        pprint(stub.getTemperature())
    elif cmd == 'set':
        try:
            val = float(input('> value: ').strip())
            stub.setTemperature(val)
        except ValueError:
            print('A number must be provided.')
            return
    elif cmd == 'on':
        stub.turnOn()
    elif cmd == 'off':
        stub.turnOff()
    elif cmd == 'state':
        pprint(stub.state())
    elif cmd == 'name':
        pprint(stub.name())
    else:
        print('Unknown command.')


def parse_ac_prop(name):
    inp = input(f'> {name}: ').strip()
    if inp != '':
        return float(inp)


def parse_ac_props():
    props = {}
    t = parse_ac_prop('temperature')
    if t is not None:
        props[SmartHome.AirCondition.Property.TEMPERATURE] = t
    p = parse_ac_prop('power')
    if p is not None:
        props[SmartHome.AirCondition.Property.POWER] = p
    return props


def exec_ac(cmd, stub):
    if cmd == 'getConfig':
        pprint(stub.getConfig())
    elif cmd == 'setConfig':
        props = parse_ac_props()
        stub.setConfig(SmartHome.AirCondition.Configuration(props))
    elif cmd == 'state':
        pprint(stub.state())
    elif cmd == 'on':
        stub.turnOn()
    elif cmd == 'off':
        stub.turnOff()
    elif cmd == 'name':
        pprint(stub.name())
    else:
        print('Unknown command.')


def exec_hac(cmd, stub):
    if cmd == 'getConfig':
        pprint(stub.getConfig())
    elif cmd == 'setConfig':
        props = parse_ac_props()
        h = parse_ac_prop('humidity')
        if h is not None:
            props[SmartHome.AirCondition.Property.HUMIDITY] = h
        stub.setConfig(SmartHome.AirCondition.Configuration(props))
    elif cmd == 'state':
        pprint(stub.state())
    elif cmd == 'on':
        stub.turnOn()
    elif cmd == 'off':
        stub.turnOff()
    elif cmd == 'name':
        pprint(stub.name())
    else:
        print('Unknown command.')
