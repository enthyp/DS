[["python:pkgdir:SmartHome"]]
module SmartHome {
    exception Error {
        string reason;
    }

    exception LogicError extends Error {}

    exception RuntimeError extends Error {}

    class DeviceConfiguration {}

    module Scheduling {
        struct Time {
            byte hour;
            byte minute;
        }
        
        struct TimeRange {
            Time from;
            Time to;
        }
        
        // TODO: ValueErrors for Time and TimeRange
        struct Activity {
            TimeRange workPeriod;
            DeviceConfiguration config;
        }

        sequence<Activity> Schedule;
        
        interface Schedulable {
            string schedule(Activity activity) throws RuntimeError;
            void unschedule(string activityId) throws RuntimeError;
            Schedule getSchedule();
        }
    }
}
