package com.example.vitalwearclonev1.communication

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.example.vitalwearclonev1.monster.BattleOpponent
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.*

@SuppressLint("MissingPermission")
class BluetoothBattleManager(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? get() = adapter?.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? get() = adapter?.bluetoothLeScanner

    private val BATTLE_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    private val CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    private var gattServer: BluetoothGattServer? = null
    private var activeGatt: BluetoothGatt? = null
    private var isServiceAdded = false


    var onBattleStarted: (BattleOpponent) -> Unit = { _ -> }

    fun startAdvertising(myState: PhoneMonsterManager.MonsterState) {
        val currentAdapter = adapter
        if (currentAdapter == null || !currentAdapter.isEnabled) {
            Timber.e("Bluetooth is disabled or not supported!")
            return
        }
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Save space for UUID
            .addServiceUuid(ParcelUuid(BATTLE_SERVICE_UUID))
            .build()

        Timber.d("Starting BLE Advertising for UUID: $BATTLE_SERVICE_UUID")
        advertiser?.startAdvertising(settings, data, advertiseCallback)
        setupGattServer(myState)
    }

    private fun setupGattServer(myState: PhoneMonsterManager.MonsterState) {
        if (gattServer != null && isServiceAdded) {
            Timber.d("GATT Server already setup")
            return
        }
        
        gattServer?.close()
        isServiceAdded = false
        
        gattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == CHARACTERISTIC_UUID) {
                    val payload = serializeStats(myState, 0L) // Server doesn't know seed yet
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, payload)
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (characteristic.uuid == CHARACTERISTIC_UUID) {
                    try {
                        val (opponent, _) = deserializeStats(value)
                        // Server is the responder
                        onBattleStarted(opponent.copy(isInitiator = false))
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error deserializing stats from client")
                        if (responseNeeded) {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                        }
                    }
                }
            }

        })

        val service = BluetoothGattService(BATTLE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val char = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(char)
        val success = gattServer?.addService(service) == true
        isServiceAdded = success
        Timber.d("GATT Service added: $success")
    }


    fun startScanning() {
        val currentAdapter = adapter
        if (currentAdapter == null || !currentAdapter.isEnabled) {
            Timber.e("Bluetooth is disabled or not supported!")
            return
        }
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(BATTLE_SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        Timber.d("Starting BLE Scan for UUID: $BATTLE_SERVICE_UUID")
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Timber.d("Scan result found: ${result.device.address}")
            scanner?.stopScan(this)
            connectToDevice(result.device)
        }
        
        override fun onScanFailed(errorCode: Int) {
            Timber.e("Scan failed with error code: $errorCode")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Timber.e("Missing BLUETOOTH_CONNECT permission!")
            return
        }
        Timber.d("Connecting to device: ${device.address}")
        activeGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.d("Connected to GATT server, requesting MTU...")
                    gatt.requestMtu(512)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.d("Disconnected from GATT server")
                    gatt.close()
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Timber.d("MTU changed to $mtu, discovering services...")
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Timber.d("Services discovered. Status: $status")
                val service = gatt.getService(BATTLE_SERVICE_UUID)
                val char = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (char != null) {
                    Timber.d("Characteristic found, reading stats...")
                    gatt.readCharacteristic(char)
                } else {
                    Timber.e("Battle Characteristic not found!")
                }
            }

            // Modern API 33+ override
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    handleCharacteristicData(gatt, characteristic, value)
                } else {
                    Timber.e("Failed to read characteristic! Status: $status")
                }
            }

            // Legacy override for older versions
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    @Suppress("DEPRECATION")
                    handleCharacteristicData(gatt, characteristic, characteristic.value)
                } else {
                    Timber.e("Failed to read characteristic! Status: $status")
                }
            }

            private fun handleCharacteristicData(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray) {
                Timber.d("Stats read successfully from server")
                try {
                    val (opponent, _) = deserializeStats(data)
                    
                    val monsterManager = PhoneMonsterManager(context)
                    val myState = monsterManager.getCurrentMonster()
                    if (myState != null) {
                        val seed = System.currentTimeMillis()
                        val payload = serializeStats(myState, seed)
                        
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(characteristic, payload, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                        } else {
                            @Suppress("DEPRECATION")
                            characteristic.value = payload
                            @Suppress("DEPRECATION")
                            gatt.writeCharacteristic(characteristic)
                        }
                        
                        Timber.d("Sent our stats to server with seed: $seed")
                        onBattleStarted(opponent.copy(seed = seed, isInitiator = true))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error handling characteristic data")
                }
            }
        }, BluetoothDevice.TRANSPORT_LE)
    }


    private fun serializeStats(state: PhoneMonsterManager.MonsterState, seed: Long): ByteArray {
        val nameBytes = (state.nickname ?: "Rival").toByteArray()
        val cardBytes = state.cardName.trim().toByteArray()
        val dimId = com.example.vitalwearclonev1.card.CardManager(context).getCard(state.cardName)?.header?.dimId ?: 0
        
        val buffer = ByteBuffer.allocate(1024)
        buffer.putInt(nameBytes.size)
        buffer.put(nameBytes)
        buffer.putInt(cardBytes.size)
        buffer.put(cardBytes)
        buffer.putInt(state.characterId)
        buffer.putInt(state.attackBonus)
        buffer.putInt(state.healthBonus)
        buffer.putInt(state.speedBonus)
        buffer.putInt(state.defenseBonus)
        buffer.putLong(seed)
        buffer.put(if (state.isBem) 1.toByte() else 0.toByte())
        buffer.putInt(dimId)
        
        val result = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(result)
        return result
    }

    private fun deserializeStats(data: ByteArray): Pair<BattleOpponent, Long> {
        val buffer = ByteBuffer.wrap(data)
        val nameSize = buffer.getInt()
        val nameBytes = ByteArray(nameSize)
        buffer.get(nameBytes)
        val name = String(nameBytes)
        
        val cardSize = buffer.getInt()
        val cardBytes = ByteArray(cardSize)
        buffer.get(cardBytes)
        val cardName = String(cardBytes).trim()
        
        val charId = buffer.getInt()
        val atk = buffer.getInt()
        val hp = buffer.getInt()
        val spd = buffer.getInt()
        val def = buffer.getInt()
        val seed = buffer.getLong()
        val isBem = try { buffer.get() == 1.toByte() } catch (e: Exception) { false }
        val dimId = try { buffer.getInt() } catch (e: Exception) { 0 }
        
        return BattleOpponent(cardName, charId, atk, hp, spd, def, name = name, seed = seed, isBem = isBem, dimId = dimId) to seed
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.d("BLE Advertising started")
        }
    }

    fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        activeGatt?.close()
        activeGatt = null
        isServiceAdded = false
    }
}

