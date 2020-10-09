package kr.ac.konkuk.smartcafe

import java.util.*


/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    private val attributes: HashMap<String?, String?> = HashMap()
    const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    var HM_RX_TX = "19b10001-e8f2-537e-4f6c-d104768a1214"
    fun lookup(uuid: String?, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }

    init {
        // Sample Services.
        attributes["19b10001-e8f2-537e-4f6c-d104768a1214"] = "Device Information Service"

        // blechat - HM-10 serial service
        attributes["19b10000-e8f2-537e-4f6c-d104768a1214"] = "HM 10 Serial"

        // blechat - HM-10 serial characteristic
        attributes[HM_RX_TX] = "RX/TX data"
    }
}