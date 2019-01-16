package jp.remote

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.ListView
import java.util.*
import kotlin.collections.ArrayList


// 複数のActivityで変数を共有したいため、Applicationクラスを継承する
class BluetoothLE : Application() {

    private lateinit var bluetoothManager :BluetoothManager
    private lateinit var bluetoothAdapter :BluetoothAdapter
    private lateinit var bluetoothLeScanner :BluetoothLeScanner
    private lateinit var characteristic_RX : BluetoothGattCharacteristic
    private lateinit var characteristic_TX: BluetoothGattCharacteristic

    private var rxd = ArrayDeque<ByteArray>()
    private var txd = ArrayDeque<ByteArray>()
    private lateinit var bluetoothGatt : BluetoothGatt

    private lateinit var bleListView : ListView
    private lateinit var arrayAdapter : ArrayAdapter<String>
    private lateinit var bleDeviceList :ArrayList<ScanResult?>
    private lateinit var mainActivity :MainActivity

    fun init_BluetoothLE() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    // BluetoothLE初期化処理
    fun init_MainActivity(view:ListView, adapter:ArrayAdapter<String>, list:ArrayList<ScanResult?>, p :MainActivity) {
        bleListView = view
        arrayAdapter = adapter
        bleDeviceList = list
        mainActivity = p
    }

    // BluetoothLEを切断する
    fun disconnect(){
        bluetoothGatt.close()
        bluetoothGatt.disconnect()
    }

    // BluetoothLEのスキャンを行う
    fun startLeScan(){
        bluetoothLeScanner.startScan(bleScannerCallback)
    }

    // BluetoothLEのスキャンを停止する
    fun stopLeScan() {
        bluetoothLeScanner.stopScan(bleScannerCallback)
    }

    // BluetoothLE送信要求
    fun transmit(tx : ByteArray) {
        txd.addFirst(tx)
        characteristic_TX.setValue(txd.last)
        bluetoothGatt.writeCharacteristic(characteristic_TX)
    }

    // BLEデバイスをスキャン関連のコールバッククラス
    val bleScannerCallback = object : ScanCallback() {

        // デバイスが見つかったら呼ばれるコールバック関数
        override fun onScanResult(callbackType: Int, result: ScanResult?) {

            // リストに登録が無い場合のみリストに追加する
            // 本来はスキャナー開始時にフィルターを掛ける方法が正解。難しかったのでこっちにした。
            if (-1 == arrayAdapter.getPosition("${result?.device?.address} - ${result?.device?.name}")) {
                arrayAdapter.add("${result?.device?.address} - ${result?.device?.name}")
                arrayAdapter.notifyDataSetChanged()

                bleDeviceList.add(result)
            }
        }
    }

    // BLEデバイスへの接続関連のコールバッククラス
    val bleGattCallback = object : BluetoothGattCallback() {

        // 太陽誘電のやつ
//        val SERVICE_UUID = "442f1570-8a00-9a28-cbe1-e1d4212d53eb" as String
//        val RX_UUID = "442f1571-8a00-9a28-cbe1-e1d4212d53eb" as String
//        val TX_UUID = "442f1572-8a00-9a28-cbe1-e1d4212d53eb" as String

        // Adafruitのやつ
        val SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" as? String
        val RX_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" as? String
        val TX_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" as? String

        val CCCD_UUID = "00002902-0000-1000-8000-00805F9B34FB" as? String

        // とりあえず接続できたら呼ばれるコールバック関数
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {

                bluetoothGatt = gatt
                gatt.discoverServices() // 接続状態になったら使えるサービスを検索しなければならないらしい。
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // 接続失敗。
                gatt.close()
                gatt.disconnect()
            }
        }

        // サービスの検索が終わった時に呼ばれるコールバック関数
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val bluetoothGattService = gatt.getService(UUID.fromString(SERVICE_UUID)) as BluetoothGattService

                if (bluetoothGattService != null) {
                    characteristic_RX = bluetoothGattService.getCharacteristic(UUID.fromString(RX_UUID)) as BluetoothGattCharacteristic
                    characteristic_TX = bluetoothGattService.getCharacteristic(UUID.fromString(TX_UUID)) as BluetoothGattCharacteristic

                    if ((characteristic_RX != null) && (characteristic_TX != null)) {

                        gatt.setCharacteristicNotification(characteristic_RX, true)

                        val descriptor = characteristic_RX.getDescriptor(UUID.fromString(CCCD_UUID)) as BluetoothGattDescriptor

                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        gatt.writeDescriptor(descriptor)

                        mainActivity.bleServiceDiscoveredCallback()

                    } else {
                        // 接続失敗
                        disconnect()
                    }
                } else {
                    // 接続失敗
                    disconnect()
                }
            } else {
                // 接続失敗
                disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (RX_UUID.equals(characteristic?.uuid.toString())) {

//                rxd.addFirst("01234567890123456789".toByteArray())
//
//                if(rxd.isNotEmpty()) {
//
//                    characteristic_TX.setValue(rxd.last)
//                    bluetoothGatt.writeCharacteristic(characteristic_TX)
//
//                }

            }
        }


        // 送信結果を通知してくるコールバック関数。呼ばれたからと言って送信成功している訳ではないので注意。
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if(status == BluetoothGatt.GATT_SUCCESS) {
                txd.removeLast()
            }

            // 送信リトライ処理
            if (txd.isNotEmpty()) {

                characteristic_TX.setValue(txd.last)
                bluetoothGatt.writeCharacteristic(characteristic_TX)

            }
        }
    }

}