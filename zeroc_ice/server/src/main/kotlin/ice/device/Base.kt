package ice.device

import SmartHome.RuntimeError
import SmartHome.Scheduling.*
import com.zeroc.Ice.Current
import java.util.*

// TODO: check (time) errors
open class SchedulableI : Schedulable {

    private val scheduled = mutableMapOf<String, Activity>()

    override fun schedule(activity: Activity, current: Current?): String {
        val activityId = UUID.randomUUID().toString()
        scheduled[activityId] = activity
        return activityId
    }

    override fun unschedule(activityId: String?, current: Current?) {
        if (scheduled.remove(activityId) == null) {
            throw RuntimeError("No such activity scheduled.")
        }
    }

    override fun getSchedule(current: Current?): Array<Activity> {
        return scheduled.values.toTypedArray()
    }
}
