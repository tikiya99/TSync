package com.example.lumanotifier
import com.example.lumanotifier.databinding.ActivityMainBinding

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import android.provider.Settings
import android.content.ComponentName
import android.app.AlertDialog
import android.service.notification.NotificationListenerService

class MainActivity : AppCompatActivity() {

    private lateinit var deviceSpinner: Spinner
    private lateinit var binding: ActivityMainBinding
    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var selectAppsButton: Button
    private lateinit var inputField: EditText
    private lateinit var logView: TextView

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var selectedDevice: BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            Toast.makeText(this, "Connected to Luma", Toast.LENGTH_SHORT).show()
        }

        val deviceSpinner = binding.deviceSpinner
        val connectButton = binding.connectButton
        val sendButton = binding.sendButton
        val selectAppsButton = binding.selectAppsButton
        val inputField = binding.inputField
        val logView = binding.logView

        // Check Bluetooth support
        if (bluetoothAdapter == null) {
            logView.text = "Bluetooth not supported on this device"
            return
        }

        // Request permissions for Android 12+
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
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                100
            )
        }

        // Ask user to grant access if not enabled
        val cn = ComponentName(this, NotificationForwarderService::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")

        if (flat == null || !flat.contains(cn.flattenToString())) {
            AlertDialog.Builder(this)
                .setTitle("Notification Access")
                .setMessage("Please enable notification access for Luma Notifier to forward app notifications.")
                .setPositiveButton("Grant Access") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Populate paired devices
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceNames = pairedDevices?.map { it.name } ?: listOf("No paired devices")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, deviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        deviceSpinner.adapter = adapter

        deviceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: android.view.View,
                position: Int,
                id: Long
            ) {
                if (!pairedDevices.isNullOrEmpty()) {
                    selectedDevice = pairedDevices.elementAt(position)
                    logView.text = "Selected: ${selectedDevice?.name}\n"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Open app selection screen
        selectAppsButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        // Connect and start Bluetooth service
        connectButton.setOnClickListener {
            if (selectedDevice == null) {
                logView.append("No device selected\n")
                return@setOnClickListener
            }

            val address = selectedDevice!!.address
            logView.append("Connecting to ${selectedDevice!!.name}...\n")

            BluetoothHelper.connect(
                address,
                onSuccess = {
                    runOnUiThread { logView.append("Connected to ${selectedDevice!!.name}\n") }
                },
                onError = { msg ->
                    runOnUiThread { logView.append("$msg\n") }
                }
            )
        }

        // Manual test message sender
        sendButton.setOnClickListener {
            val msg = inputField.text.toString().trim()
            if (msg.isNotEmpty()) {
                try {
                    BluetoothLink.send?.invoke(msg)
                    logView.append("Sent: $msg\n")
                    inputField.text.clear()
                } catch (e: Exception) {
                    logView.append("Send failed: ${e.message}\n")
                }
            } else {
                logView.append("No message entered\n")
            }
        }
    }

    override fun onDestroy() {
            super.onDestroy()
            BluetoothHelper.disconnect()
    }
}
