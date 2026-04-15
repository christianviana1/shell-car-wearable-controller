package com.CTAP.shellcardebugcontroller.presentation

object CarProtocol {

    // ── Definições de Comando ─────────────────────────────────────────────────
    const val MOTOR_STOP    = 0x00.toByte()
    const val MOTOR_FORWARD = 0x01.toByte()
    const val MOTOR_REVERSE = 0x02.toByte()

    // Headers para pacotes de 5 bytes
    private const val H1 = 0x55.toByte()
    private const val H2 = 0xAA.toByte()

    /**
     * Monta o pacote dinamicamente:
     * - Se for FRENTE: usa o protocolo de 7 bytes [00 01 00...]
     * - Se for RÉ, PARAR ou DIREÇÃO: usa o protocolo de 5 bytes [55 AA...]
     */
    fun buildPacket(
        motor: Byte,
        left: Boolean = false,
        right: Boolean = false
    ): ByteArray {

        // CASO FRENTE (7 bytes)
        if (motor == MOTOR_FORWARD) {
            val packet = ByteArray(7)
            packet[0] = 0x00.toByte()
            packet[1] = 0x01.toByte()
            packet[2] = if (left) 0x01.toByte() else 0x00.toByte()
            packet[3] = if (right) 0x01.toByte() else 0x00.toByte()
            return packet
        }

        // CASO PADRÃO (5 bytes: Parado ou Ré)
        val packet = ByteArray(5)
        packet[0] = H1
        packet[1] = H2

        // No protocolo 55AA, a ré é acionada enviando 0x01 no byte de motor
        packet[2] = if (motor == MOTOR_REVERSE) 0x01.toByte() else 0x00.toByte()

        packet[3] = if (left)  0x01.toByte() else 0x00.toByte()
        packet[4] = if (right) 0x01.toByte() else 0x00.toByte()

        return packet
    }

    fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}