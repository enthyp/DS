[["python:pkgdir:SmartHome"]]
module SmartHome {

    exception Error {
        string reason;
    }

    exception LogicError extends Error {}

    exception RuntimeError extends Error {}

    class DeviceConfiguration {}

    sequence<string> DeviceList;
    dictionary<string, DeviceList> Devices;

    interface Info {
        Devices getDevices();
    }
}
