package kr.ac.konkuk.smartcafe

import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.ExpandableListView.OnChildClickListener
import java.util.*


/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with `BluetoothLeService`, which in
 * turn interacts with the Bluetooth LE API.
 */
class DeviceControlActivity : Activity() {
    private var mConnectionState: TextView? = null
    private var mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mGattServicesList: ExpandableListView? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? =
        ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private var mConversationArrayAdapter: ArrayAdapter<String>? = null
    private var mConversationView: ListView? = null

    // blechat - characteristics for HM-10 serial
    private var characteristicTX: BluetoothGattCharacteristic? = null
    private var characteristicRX: BluetoothGattCharacteristic? = null
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    // Code to manage Service lifecycle.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            // get the BluetoothLeService object
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!(mBluetoothLeService?.initialize()!!)) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up. initialization.
            mBluetoothLeService?.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read  or notification operations.
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true
                updateConnectionState(R.string.connected)   // show connected state on UI
                invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false
                updateConnectionState(R.string.disconnected)  // show disconnected state on UI
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService?.supportedGattServices)

                // blechat
                // set serial chaacteristics
                setupSerial()
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    // If a given GATT characteristic is selected, check for supported features.
    // list of supported characteristic features.
    private val servicesListClickListener =
        OnChildClickListener { parent, v, groupPosition, childPosition, id ->
            if (mGattCharacteristics != null) {
                val characteristic = mGattCharacteristics!![groupPosition][childPosition]
                val charaProp = characteristic.properties
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user
                    // interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService?.setCharacteristicNotification(
                            mNotifyCharacteristic!!, false
                        )
                        mNotifyCharacteristic = null
                    }
                    mBluetoothLeService?.readCharacteristic(characteristic)
                }
                if (charaProp or BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
                    mNotifyCharacteristic = characteristic
                    mBluetoothLeService?.setCharacteristicNotification(
                        characteristic, true
                    )
                }
                return@OnChildClickListener true
            }
            false
        }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    //private GoogleApiClient client;
    private fun clearUI() {
        mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        mDataField!!.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)
        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)   // name of device
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)  // address of device

        // Sets up UI references.
        (findViewById<View>(R.id.device_address) as TextView).text = mDeviceAddress  // 1. display the address of device
        mGattServicesList = findViewById<View>(R.id.gatt_services_list) as ExpandableListView  // 2. init the list view
        mGattServicesList!!.setOnChildClickListener(servicesListClickListener)  //  3. insert the click listener on list view
        mConnectionState = findViewById<View>(R.id.connection_state) as TextView  // 4. init the text view which shows a connection state
        mDataField = findViewById<View>(R.id.data_value) as TextView  // 5. init the text view which shows a data from arduino
        actionBar!!.title = mDeviceName  // 6. set name of actionbar
        actionBar!!.setDisplayHomeAsUpEnabled(true)  // 7. insert arrow. When user touchs it, he moves to previous activity
        val gattServiceIntent = Intent(this@DeviceControlActivity, BluetoothLeService::class.java)  // 8. create intent
        /**
         * function in BluetoothLeService can be called by this code.
         * result of function will be send to mServiceConnection(), and you can use this result
         */
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE)
    }

    // setupSerial : set serial characteristics
    private fun setupSerial() {

        // blechat - set serial characteristics
        var uuid: String
        val unknownServiceString = resources.getString(
            R.string.unknown_service
        )
        for (gattService in mBluetoothLeService?.supportedGattServices!!) {
            uuid = gattService.uuid.toString()
            Log.d(TAG, "UUID : $uuid")

            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                // get characteristic when UUID matches RX/TX UUID
                Log.d(TAG, "CLEAR")
                characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
                characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX)
                mBluetoothLeService?.setCharacteristicNotification(characteristicTX!!, true)
                break
            } // if
        } // for
    }

    // blechat
    public override fun onStart() {
        super.onStart()
        // change view for chat
        setContentView(R.layout.communication_layout)

        // Initialize the array adapter for the conversation
        mConversationArrayAdapter = ArrayAdapter(this@DeviceControlActivity, R.layout.message)

        mConversationView = findViewById<View>(R.id.`in`) as ListView
        mConversationView!!.adapter = mConversationArrayAdapter
    }

    override fun onResume() {
        super.onResume()
        // enroll the broadcast where BluetoothLeService send a connection state or data.
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result: Boolean? = mBluetoothLeService?.connect(mDeviceAddress)  // connect
            Log.d(TAG, "Connect request result=$result")
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBluetoothLeService?.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBluetoothLeService?.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
    }

    private fun displayData(data: String?) {
        if (data != null) {
            mDataField!!.text = data
            val nlIdx = data.indexOf('\n') // index of newline

            // blechat : add received data to screen
            // if there is a newline
            if (nlIdx > 0) {
                mConversationArrayAdapter!!.add(data.substring(0, nlIdx))
            } else {
                // use whole string
                mConversationArrayAdapter!!.add(data)
            } // else
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // we populate the data structure that is bound to the ExpandableListView on the UI.
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString = resources.getString(
            R.string.unknown_service
        )
        val unknownCharaString = resources.getString(
            R.string.unknown_characteristic
        )
        val gattServiceData = ArrayList<HashMap<String, String?>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String?>>>()
        mGattCharacteristics = ArrayList()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String?>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownServiceString)
            currentServiceData[LIST_UUID] = uuid
            gattServiceData.add(currentServiceData)
            val gattCharacteristicGroupData = ArrayList<HashMap<String, String?>>()
            val gattCharacteristics = gattService
                .characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {
                charas.add(gattCharacteristic)
                val currentCharaData = HashMap<String, String?>()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = SampleGattAttributes.lookup(uuid, unknownCharaString)
                currentCharaData[LIST_UUID] = uuid
                gattCharacteristicGroupData.add(currentCharaData)
            }
            mGattCharacteristics!!.add(charas)
            gattCharacteristicData.add(gattCharacteristicGroupData)
            Log.d(TAG, "CharacteristicData : " + gattCharacteristicData[0].toString())
            Log.d(TAG, "ServiceData : " + gattServiceData[0].toString())
            Log.d(TAG, "gattService : $gattService")
            Log.d(TAG, "CharacteristicGroupData : $gattCharacteristicGroupData")
            Log.d(TAG, "Characteristics: $gattCharacteristics")
        }
        val gattServiceAdapter = SimpleExpandableListAdapter(
            this, gattServiceData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(LIST_NAME, LIST_UUID), intArrayOf(
                android.R.id.text1,
                android.R.id.text2
            ), gattCharacteristicData,
            android.R.layout.simple_expandable_list_item_2, arrayOf(
                LIST_NAME, LIST_UUID
            ), intArrayOf(
                android.R.id.text1,
                android.R.id.text2
            )
        )
        mGattServicesList!!.setAdapter(gattServiceAdapter)
    }

    // blechat
    // btnClick Click handler for Send button
    // communication_layout.xml
    fun btnClick(view: View?) {
        sendSerial()
    }

    // blechat
    // sendSerial : Send string io out field
    private fun sendSerial() {
        val view = findViewById<View>(R.id.edit_text_out) as TextView
        val message = view.text.toString()
        Log.d(TAG, "Sending: $message")
        val tx = message.toByteArray()
        if (mConnected) {
            characteristicTX!!.value = tx
            mBluetoothLeService?.writeCharacteristic(characteristicTX)

            // clear text
            view.text = ""
        } // if
    }

    public override fun onStop() {
        super.onStop()
    } // blechat

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
        // intent-filter reference : https://developer.android.com/guide/components/intents-filters?hl=ko#Receiving
    }
}