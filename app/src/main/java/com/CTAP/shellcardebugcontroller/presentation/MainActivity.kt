package com.CTAP.shellcardebugcontroller.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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

// ── Configurações BLE ──────────────────────────────────────────────────────────
private const val CAR_MAC_ADDRESS = "13:05:AA:02:4E:0B"
private val SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
private val CHAR_UUID    = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

// ── Paleta Ferrari ─────────────────────────────────────────────────────────────
private val ColorBackground  = Color(0xFF0A0A0A)
private val ColorFerrariRed  = Color(0xFFDC143C)
private val ColorGold        = Color(0xFFFFD700)
private val ColorGreen       = Color(0xFF00E676)
private val ColorGray        = Color(0xFF444444)
private val ColorDimText     = Color(0xFF555555)

enum class MotorState(val label: String, val color: Color, val motor: Byte) {
    PARADO ("PARAR",  ColorFerrariRed, MOTOR_STOP),
    FRENTE ("FRENTE", ColorGreen,      MOTOR_FORWARD),
    RE     ("RÉ",     ColorGold,       MOTOR_REVERSE)
}

class MainActivity : ComponentActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var motorState     = mutableStateOf(MotorState.PARADO)
    private var connectionText = mutableStateOf("Conectando…")
    private var lastPacket     = mutableStateOf("-- -- -- -- --")
    private var steerDir       = mutableStateOf(0)
    private var isTurningLeft  = false
    private var isTurningRight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )

        connectToCar()

        setContent {
            ShellCarDebugControllerTheme {
                FerrariControlScreen(
                    currentMotorState = motorState.value,
                    connectionText    = connectionText.value,
                    lastPacket       = lastPacket.value,
                    steerDir         = steerDir.value,
                    onStateSelected  = { newState ->
                        motorState.value = newState
                        sendToCar()
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToCar() {
        val adapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        try {
            bluetoothGatt = adapter.getRemoteDevice(CAR_MAC_ADDRESS)
                .connectGatt(this, false, gattCallback)
        } catch (e: Exception) {
            connectionText.value = "Erro de MAC"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            connectionText.value = when (newState) {
                BluetoothProfile.STATE_CONNECTED    -> { gatt.discoverServices(); "● Conectado" }
                BluetoothProfile.STATE_DISCONNECTED -> "○ Desconectado"
                else                                -> "○ Aguardando"
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            writeCharacteristic = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_UUID)
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {

            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL)
            isTurningRight = delta > 0
            isTurningLeft  = delta < 0
            steerDir.value  = if (delta > 0) 1 else -1

            sendToCar()

            Handler(Looper.getMainLooper()).postDelayed({
                isTurningLeft  = false
                isTurningRight = false
                steerDir.value  = 0
                sendToCar()
            }, 300)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    @SuppressLint("MissingPermission")
    private fun sendToCar() {
        val packet = CarProtocol.buildPacket(
            motor = motorState.value.motor,
            left  = isTurningLeft,
            right = isTurningRight
        )
        lastPacket.value = packet.toHexString()

        writeCharacteristic?.let { char ->
            char.value     = packet
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            bluetoothGatt?.writeCharacteristic(char)
        }
    }
}

@Composable
fun FerrariControlScreen(
    currentMotorState: MotorState,
    connectionText: String,
    lastPacket: String,
    steerDir: Int,
    onStateSelected: (MotorState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Text(
                text = connectionText,
                color = if (connectionText.startsWith("●")) ColorGreen else ColorDimText,
                fontSize = 8.sp,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Grade de Botões ─────────────────────────────────────────────

            // Botão FRENTE (Topo)
            MotorButton(
                state = MotorState.FRENTE,
                isSelected = currentMotorState == MotorState.FRENTE,
                onClick = { onStateSelected(MotorState.FRENTE) }
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botão PARAR (Esquerda)
                MotorButton(
                    state = MotorState.PARADO,
                    isSelected = currentMotorState == MotorState.PARADO,
                    onClick = { onStateSelected(MotorState.PARADO) }
                )

                // Botão RÉ (Direita)
                MotorButton(
                    state = MotorState.RE,
                    isSelected = currentMotorState == MotorState.RE,
                    onClick = { onStateSelected(MotorState.RE) }
                )
            }

            // ── Direção ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("◄", color = if (steerDir == -1) ColorGold else ColorDimText)
                Text(lastPacket, color = ColorGold.copy(0.5f), fontSize = 7.sp)
                Text("►", color = if (steerDir == 1) ColorGold else ColorDimText)
            }
        }
    }
}

@Composable
fun MotorButton(
    state: MotorState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) state.color else ColorGray
    val alpha = if (isSelected) 1f else 0.4f

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(width = 75.dp, height = 42.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = color.copy(alpha = alpha),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) state.color.copy(0.2f) else Color.Transparent
        )
    ) {
        Text(
            text = state.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(
                shadow = if (isSelected) Shadow(color.copy(0.8f), blurRadius = 8f) else null
            )
        )
    }
}