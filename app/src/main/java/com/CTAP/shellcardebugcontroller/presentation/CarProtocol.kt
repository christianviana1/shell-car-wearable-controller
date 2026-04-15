package com.CTAP.shellcardebugcontroller.presentation

object CarProtocol {

    const val MOTOR_STOP    = 0x00.toByte()
    const val MOTOR_FORWARD = 0x01.toByte()
    const val MOTOR_REVERSE = 0x02.toByte()

    private const val H1 = 0x55.toByte()
    private const val H2 = 0xAA.toByte()

    fun buildPacket(
        motor: Byte,
        left: Boolean = false,
        right: Boolean = false
    ): ByteArray {

        // ── PROTOCOLO DE 7 BYTES (FRENTE) ──
        if (motor == MOTOR_FORWARD) {
            val packet = ByteArray(7)
            packet[0] = 0x00.toByte()
            packet[1] = 0x01.toByte()
            packet[2] = 0x00.toByte() // Neutro

            // Conforme seu teste: 00 01 00 01 00 é Esquerda
            packet[3] = if (left) 0x01.toByte() else 0x00.toByte()

            // Conforme seu teste: 00 01 00 00 01 é Direita
            packet[4] = if (right) 0x01.toByte() else 0x00.toByte()

            // Bytes 5 e 6 preenchidos com 00
            return packet
        }

        // ── PROTOCOLO DE 5 BYTES (RÉ / PARADO) ──
        val packet = ByteArray(5)
        packet[0] = H1
        packet[1] = H2
        packet[2] = if (motor == MOTOR_REVERSE) 0x01.toByte() else 0x00.toByte()
        packet[3] = if (left)  0x01.toByte() else 0x00.toByte()
        packet[4] = if (right) 0x01.toByte() else 0x00.toByte()

        return packet
    }

    fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}