package ice

import com.zeroc.Ice.Current
import com.zeroc.Ice.Object
import com.zeroc.Ice.ServantLocator

class Locator<T : Object>(private val availableObjects: Array<String>, private val buildBlock: (String) -> T) :
    ServantLocator {
    override fun locate(curr: Current?): ServantLocator.LocateResult? {
        val id = curr?.id
        val name = id?.name

        return if (name != null && availableObjects.contains(name)) {
            val servant = buildBlock(name)
            curr.adapter?.add(servant, id)
            ServantLocator.LocateResult(servant, null)
        } else {
            null
        }
    }

    override fun finished(curr: Current?, servant: Object?, cookie: Any?) {}

    override fun deactivate(category: String?) {}
}
