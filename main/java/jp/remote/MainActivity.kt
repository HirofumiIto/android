package jp.remote

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.widget.Button
import android.widget.ListView

class MainActivity : AppCompatActivity() {

    lateinit var ble : BluetoothLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bleListView = findViewById(R.id.bleListView) as ListView
        val arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        val bleDeviceList = arrayListOf<ScanResult?>()

        val buttonScanStart = findViewById(R.id.btnScanStart) as Button
        val buttonScanStop = findViewById(R.id.btnScanStop) as Button
        val buttonDisconnect= findViewById(R.id.btnDisconnect) as Button


        // BluetootLEクラスの初期化
        ble = this.getApplication() as BluetoothLE
        ble.init_BluetoothLE()
        ble.init_MainActivity(bleListView, arrayAdapter, bleDeviceList, this)

        buttonScanStop.setEnabled(false)
        buttonDisconnect.setEnabled(false)
        buttonScanStart.setEnabled(true)


        // GPSを有効にしないとBLEスキャンができないらしいので、承認要求の画面を出す
        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0)

        bleListView.adapter = arrayAdapter



        // STARTボタン押しでBLEスキャン開始
        buttonScanStart.setOnClickListener {

            buttonScanStart.setEnabled(false)

            ble.startLeScan()

            arrayAdapter.clear()
            bleDeviceList.clear()


            buttonScanStop.setEnabled(true)
            buttonDisconnect.setEnabled(false)
        }

        // STOPボタン押しでBLEスキャン終了
        buttonScanStop.setOnClickListener {

            buttonScanStop.setEnabled(false)

            ble.stopLeScan()

            buttonScanStart.setEnabled(true)

        }

        // Disconnect押しで切断
        buttonDisconnect.setOnClickListener {
            ble.disconnect()

            buttonScanStop.setEnabled(false)
            buttonDisconnect.setEnabled(false)
            buttonScanStart.setEnabled(true)
        }


        // リストをクリック
        bleListView.setOnItemClickListener { parent, view, position, id ->

            buttonScanStop.setEnabled(false)
            buttonScanStart.setEnabled(false)
            buttonDisconnect.setEnabled(true)

            ble.stopLeScan()

            bleDeviceList.get(position)?.device?.connectGatt(this, false, ble.bleGattCallback)

        }
    }

    fun bleServiceDiscoveredCallback() {
        val intent = Intent(this, JoystickActivity::class.java)
        startActivity(intent)
    }

}
