[["python:pkgdir:SmartHome"]]
module SmartHome {

    // Generic data and operations
    enum DeviceState { ON, OFF }
    class DeviceConfiguration {}

    interface BaseDevice {
        idempotent string name();
        idempotent void turnOn();
        idempotent void turnOff();
        idempotent DeviceState state();
    }

    exception BaseError {
        string reason;
    }

    exception ValueError extends BaseError {
        float from;
        float to;
    }

    // Info about all available devices
    sequence<string> DeviceList;
    dictionary<string, DeviceList> Devices;

    interface Info {
        Devices getDevices();
    }
}
