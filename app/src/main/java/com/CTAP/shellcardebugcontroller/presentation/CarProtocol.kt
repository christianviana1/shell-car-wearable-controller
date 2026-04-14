package com.CTAP.shellcardebugcontroller.presentation

object CarProtocol {

    // ── Definições de Comando ─────────────────────────────────────────────────
    const val MOTOR_STOP    = 0x00.toByte()
    const val MOTOR_FORWARD = 0x01.toByte()
    const val MOTOR_REVERSE = 0x02.toByte() // No protocolo 55AA, ré é 01

    // Headers
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

        // CASO ESPECIAL: Frente (7 bytes conforme sua descoberta)
        if (motor == MOTOR_FORWARD) {
            val packet = ByteArray(7)
            packet[0] = 0x00.toByte()
            packet[1] = 0x01.toByte()
            packet[2] = if (left) 0x01.toByte() else 0x00.toByte()
            packet[3] = if (right) 0x01.toByte() else 0x00.toByte()
            // bytes 4, 5, 6 ficam 0x00
            return packet
        }

        // CASO PADRÃO: Ré, Parado e Direção (5 bytes)
        val packet = ByteArray(5)
        packet[0] = H1
        packet[1] = H2

        // Se motor for REVERSE (0x02), no protocolo 55AA ele vira 0x01
        packet[2] = if (motor == MOTOR_REVERSE) 0x01.toByte() else 0x00.toByte()

        // Direção nos bits 4 e 5 (índices 3 e 4 do array)
        packet[3] = if (left)  0x01.toByte() else 0x00.toByte()
        packet[4] = if (right) 0x01.toByte() else 0x00.toByte()

        return packet
    }

    /** Converte para string legível */
    fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
}