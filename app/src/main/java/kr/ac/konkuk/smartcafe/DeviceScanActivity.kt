package kr.ac.konkuk.smartcafe

import android.app.ListActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.util.*

class DeviceScanActivity : ListActivity(){
    private var mLeDeviceListAdapter: LeDeviceListAdapter? = null   // scan list adapter
    private var mBluetoothAdapter: BluetoothAdapter? = null   // bluetooth adapter
    private var mScanning = false   // whether to processing scanning
    private var mHandler: Handler? = null

    private val REQUEST_ENABLE_BT = 1

    private val mInflater : LayoutInflater = layoutInflater

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.setTitle(R.string.title_devices)
        mHandler = Handler()

        // Check if bluetooth supports the BLE
        packageManager.takeIf { !(it.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) }?.also {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        // get a reference to BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).isVisible = false
            menu.findItem(R.id.menu_scan).isVisible = true
            menu.findItem(R.id.menu_refresh).actionView = null
        } else {
            menu.findItem(R.id.menu_stop).isVisible = true
            menu.findItem(R.id.menu_scan).isVisible = false
            menu.findItem(R.id.menu_refresh).setActionView(
                R.layout.actionbar_indeterminate_progress
            )
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_scan -> {
                mLeDeviceListAdapter!!.clear()
                scanLeDevice(true)
            }
            R.id.menu_stop -> scanLeDevice(false)
        }
        return true
    }

    // In fact, Scanning activity starts here. For giving this service irrelevant to exit the app or not.
    override fun onResume() {
        super.onResume()

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        mBluetoothAdapter?.takeIf { it.isEnabled }?.apply {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = LeDeviceListAdapter()
        listAdapter = mLeDeviceListAdapter
        // Start scanning
        scanLeDevice(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_CANCELED) {
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
        mLeDeviceListAdapter!!.clear()
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val device = mLeDeviceListAdapter!!.getDevice(position) ?: return
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (mScanning) {
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
            mScanning = false
        }
        startActivity(intent)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                mScanning = false
                mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
                invalidateOptionsMenu()  // recall onCreateOptionsMenu method
            }, SCAN_PERIOD)
            mScanning = true
            mBluetoothAdapter!!.startLeScan(mLeScanCallback)  // Funtion mLeScanCallback is called by startLeScan / stopLeScan
        } else {
            mScanning = false
            mBluetoothAdapter!!.stopLeScan(mLeScanCallback)
        }
        invalidateOptionsMenu()
    }

    // Adapter for holding devices found through scanning.
    inner class LeDeviceListAdapter : BaseAdapter() {
        private val mLeDevices: ArrayList<BluetoothDevice> = ArrayList()

        fun addDevice(device: BluetoothDevice) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device)
            }
        }

        fun getDevice(position: Int): BluetoothDevice {
            return mLeDevices[position]
        }

        fun clear() {
            mLeDevices.clear()
        }

        override fun getCount(): Int {
            return mLeDevices.size
        }

        override fun getItem(i: Int): Any {
            return mLeDevices[i]
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getView(i: Int, view: View, viewGroup: ViewGroup): View {
            var view = view
            val viewHolder: ViewHolder
            // General ListView optimization code.
            if (view == null) {  // only occurs if the getView function is called for the first time.
                view = mInflater.inflate(R.layout.listitem_device, null)  // memorization of xml file to use it as object
                viewHolder = ViewHolder()  // create viewHolder object
                viewHolder.deviceAddress = view.findViewById<View>(R.id.device_address) as TextView
                viewHolder.deviceName = view.findViewById<View>(R.id.device_name) as TextView
                view.tag = viewHolder  // set tag
            } else {
                viewHolder = view.tag as ViewHolder
            }
            val device = mLeDevices[i]
            val deviceName = device.name
            if (deviceName != null && deviceName.isNotEmpty()) viewHolder.deviceName!!.text =
                deviceName else viewHolder.deviceName?.setText(R.string.unknown_device)
            viewHolder.deviceAddress?.text = device.address
            return view
        }
    }

    // Device scan callback. Reflect information for being added device on UI
    private val mLeScanCallback =
        LeScanCallback { device, rssi, scanRecord ->
            runOnUiThread {
                mLeDeviceListAdapter!!.addDevice(device)   // add device information to list adapter.
                mLeDeviceListAdapter!!.notifyDataSetChanged()  // call getView() function.
            }
        }

    internal class ViewHolder {
        var deviceName: TextView? = null
        var deviceAddress: TextView? = null
    }
}