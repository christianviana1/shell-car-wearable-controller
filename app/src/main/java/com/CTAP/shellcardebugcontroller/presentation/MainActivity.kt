package com.CTAP.shellcardebugcontroller.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material3.*
import com.CTAP.shellcardebugcontroller.presentation.CarProtocol.MOTOR_FORWARD
import com.CTAP.shellcardebugcontroller.presentation.CarProtocol.MOTOR_REVERSE
import com.CTAP.shellcardebugcontroller.presentation.CarProtocol.MOTOR_STOP
import com.CTAP.shellcardebugcontroller.presentation.CarProtocol.toHexString
import com.CTAP.shellcardebugcontroller.presentation.theme.ShellCarDebugControllerTheme
import java.util.*

private val SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
private val CHAR_UUID    = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
private val BATTERY_CHAR_UUID    = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

private val ColorBackground = Color(0xFF0A0A0A)
private val ColorFerrariRed = Color(0xFFDC143C)
private val ColorGold       = Color(0xFFFFD700)
private val ColorGreen      = Color(0xFF00E676)
private val ColorGray       = Color(0xFF444444)
private val ColorDimText    = Color(0xFF555555)

enum class MotorState(val label: String, val color: Color, val motor: Byte) {
    PARADO ("PARAR",  ColorFerrariRed, MOTOR_STOP),
    FRENTE ("FRENTE", ColorGreen,      MOTOR_FORWARD),
    RE     ("RÉ",     ColorGold,       MOTOR_REVERSE)
}

class MainActivity : ComponentActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var isScanning = false

    private val motorState     = mutableStateOf(MotorState.PARADO)
    private val connectionText = mutableStateOf("Buscando…")
    private val batteryLevel   = mutableStateOf("--")
    private val lastPacket     = mutableStateOf("-- -- -- -- --")

    private var isTurningLeft  by mutableStateOf(false)
    private var isTurningRight by mutableStateOf(false)
    private var steerDir       by mutableStateOf(0)

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        ), 1)

        startScanning()

        setContent {
            ShellCarDebugControllerTheme {
                FerrariControlScreen(
                    currentMotorState = motorState.value,
                    connectionText    = connectionText.value,
                    batteryLevel      = batteryLevel.value,
                    lastPacket        = lastPacket.value,
                    steerDir          = steerDir,
                    onStateSelected   = { newState: MotorState ->
                        motorState.value = newState
                        sendToCar()
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        if (isScanning) return
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val scanner = manager.adapter.bluetoothLeScanner ?: return

        connectionText.value = "Buscando…"
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: ""

            if (isScanning && (name.startsWith("SL-") || name.contains("Shell", ignoreCase = true))) {
                isScanning = false
                val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                manager.adapter.bluetoothLeScanner?.stopScan(this)
                connectionText.value = "Conectando…"
                bluetoothGatt = device.connectGatt(this@MainActivity, false, gattCallback)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionText.value = "● ON"
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionText.value = "○ OFF"
                batteryLevel.value = "--"
                bluetoothGatt = null
                writeCharacteristic = null
                startScanning()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            writeCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)

            val bService = gatt.getService(BATTERY_SERVICE_UUID)
            bService?.getCharacteristic(BATTERY_CHAR_UUID)?.let { char ->
                gatt.readCharacteristic(char)
                gatt.setCharacteristicNotification(char, true)
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, char: BluetoothGattCharacteristic, status: Int) {
            if (char.uuid == BATTERY_CHAR_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                val data = char.value
                if (data != null && data.isNotEmpty()) {
                    batteryLevel.value = "${data[0].toInt()}%"
                }
            }
        }

        @Deprecated("Deprecated in Java")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, char: BluetoothGattCharacteristic) {
            if (char.uuid == BATTERY_CHAR_UUID) {
                val data = char.value
                if (data != null && data.isNotEmpty()) {
                    batteryLevel.value = "${data[0].toInt()}%"
                }
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
            isTurningRight = delta > 0
            isTurningLeft  = delta < 0
            steerDir = if (delta > 0) 1 else -1
            sendToCar()
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).postDelayed({
                isTurningLeft = false; isTurningRight = false; steerDir = 0; sendToCar()
            }, 400)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    @SuppressLint("MissingPermission")
    private fun sendToCar() {
        val packet = CarProtocol.buildPacket(motorState.value.motor, isTurningLeft, isTurningRight)
        lastPacket.value = packet.toHexString()

        val gatt = bluetoothGatt ?: return
        val char = writeCharacteristic ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(char, packet, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        } else {
            @Suppress("DEPRECATION")
            char.value = packet
            @Suppress("DEPRECATION")
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }
}

@Composable
fun FerrariControlScreen(
    currentMotorState: MotorState,
    connectionText: String,
    batteryLevel: String,
    lastPacket: String,
    steerDir: Int,
    onStateSelected: (MotorState) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(ColorBackground).padding(14.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = connectionText, color = if (connectionText.contains("●")) ColorGreen else ColorDimText, fontSize = 9.sp)
                Text(text = "🔋 $batteryLevel", color = if (batteryLevel == "--") ColorDimText else ColorGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            MotorButton(MotorState.FRENTE, currentMotorState == MotorState.FRENTE) { onStateSelected(MotorState.FRENTE) }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                MotorButton(MotorState.PARADO, currentMotorState == MotorState.PARADO) { onStateSelected(MotorState.PARADO) }
                MotorButton(MotorState.RE, currentMotorState == MotorState.RE) { onStateSelected(MotorState.RE) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("◄", color = if (steerDir == -1) ColorGold else ColorDimText, fontSize = 12.sp)
                Text(text = lastPacket, color = ColorGold.copy(0.35f), fontSize = 7.sp, textAlign = TextAlign.Center)
                Text("►", color = if (steerDir == 1) ColorGold else ColorDimText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MotorButton(state: MotorState, isSelected: Boolean, onClick: () -> Unit) {
    val color = if (isSelected) state.color else ColorGray
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = 78.dp, height = 42.dp).border(width = if (isSelected) 2.dp else 1.dp, color = color.copy(alpha = if (isSelected) 1f else 0.3f), shape = RoundedCornerShape(21.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) state.color.copy(0.15f) else Color.Transparent)
    ) {
        Text(text = state.label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Black, style = TextStyle(shadow = if (isSelected) Shadow(color.copy(0.8f), blurRadius = 8f) else null))
    }
}