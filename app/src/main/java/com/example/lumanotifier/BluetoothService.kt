package com.example.lumanotifier

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.OutputStream
import java.util.UUID

object BluetoothHelper {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun connect(address: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (adapter == null) {
            onError("Bluetooth not supported")
            return
        }

        Thread {
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(address)
                val tmpSocket = device.createRfcommSocketToServiceRecord(uuid)
                adapter.cancelDiscovery()
                tmpSocket.connect()
                socket = tmpSocket
                outputStream = tmpSocket.outputStream

                BluetoothLink.send = { msg ->
                    try {
                        outputStream?.write((msg + "\n").toByteArray())
                    } catch (e: Exception) {
                        Log.e("BluetoothHelper", "Send failed", e)
                    }
                }

                Log.d("BluetoothHelper", "Connected to $address")
                onSuccess()
            } catch (e: Exception) {
                Log.e("BluetoothHelper", "Connection failed", e)
                onError("Connection failed: ${e.message}")
                disconnect()
            }
        }.start()
    }

    fun disconnect() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        BluetoothLink.send = null
    }
}
