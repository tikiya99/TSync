package com.example.lumanotifier

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var deviceSpinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var inputField: EditText
    private lateinit var logView: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var selectedDevice: BluetoothDevice? = null

    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        deviceSpinner = findViewById(R.id.deviceSpinner)
        connectButton = findViewById(R.id.connectButton)
        sendButton = findViewById(R.id.sendButton)
        inputField = findViewById(R.id.inputField)
        logView = findViewById(R.id.logView)



        // Bluetooth check
        if (bluetoothAdapter == null) {
            logView.text = "Bluetooth not supported on this device"
            return
        }

        // Runtime permission request (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                100
            )
        }

        // Populate dropdown with paired devices
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceNames = pairedDevices?.map { it.name } ?: listOf("No paired devices")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        // Handle device selection
        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View, position: Int, id: Long
            ) {
                if (pairedDevices != null && pairedDevices.isNotEmpty()) {
                    selectedDevice = pairedDevices.elementAt(position)
                    logView.text = "Selected: ${selectedDevice?.name}\n"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Connect button
        connectButton.setOnClickListener {
            if (selectedDevice == null) {
                logView.append("No device selected\n")
                return@setOnClickListener
            }
            Thread {
                try {
                    bluetoothSocket =
                        selectedDevice!!.createRfcommSocketToServiceRecord(uuid)
                    bluetoothAdapter.cancelDiscovery()
                    bluetoothSocket!!.connect()
                    outputStream = bluetoothSocket!!.outputStream
                    runOnUiThread {
                        logView.append("Connected to ${selectedDevice!!.name}\n")
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        logView.append("Connection failed: ${e.message}\n")
                    }
                }
            }.start()
        }

        // Send button
        sendButton.setOnClickListener {
            val msg = inputField.text.toString()
            if (msg.isNotEmpty() && outputStream != null) {
                try {
                    outputStream!!.write("$msg\n".toByteArray())
                    logView.append("Sent: $msg\n")
                    inputField.text.clear()
                } catch (e: Exception) {
                    logView.append("Send failed: ${e.message}\n")
                }
            } else {
                logView.append("No device connected\n")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (_: Exception) {}
    }
}
