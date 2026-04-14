Shell Car Wearable Controller 🏎️⌚
Este é um controlador para Wear OS (Android) desenvolvido em Kotlin e Jetpack Compose, projetado especificamente para operar os carrinhos da coleção Shell Recharge / Ferrari Bburago via Bluetooth Low Energy (BLE).

O projeto foi refatorado para suportar um protocolo híbrido de comunicação, garantindo que o comando de aceleração total (frente) funcione corretamente em modelos que exigem pacotes de 7 bytes.

✨ Funcionalidades
Controle Híbrido: Suporta pacotes de 5 bytes (padrão 55AA) e pacotes de 7 bytes para aceleração.

Interface Direta: Três botões dedicados para controle de estado: FRENTE, PARAR e RÉ.

Controle de Direção: Integração com a coroa rotacional (Rotary Encoder) do smartwatch para virar para esquerda e direita.

Feedback Visual: Interface inspirada na estética Ferrari com indicadores de status de conexão e pacotes hexadecimais em tempo real.

🛠️ Protocolo de Comunicação
O projeto utiliza dois formatos de pacotes dependendo da ação:

1. Movimento à Frente (7 Bytes)
Byte 0	Byte 1	Byte 2	Byte 3	Bytes 4-6
0x00	0x01 (Motor)	Esquerda (0/1)	Direita (0/1)	0x00
2. Parada e Ré (5 Bytes)
Byte 0 (H1)	Byte 1 (H2)	Byte 2 (Motor)	Byte 3 (Esq)	Byte 4 (Dir)
0x55	0xAA	0x01 (Ré) / 0x00 (Stop)	0x01	0x01
🚀 Como usar
Clone o repositório em seu ambiente local.

Abra o projeto no Android Studio.

Certifique-se de que o CAR_MAC_ADDRESS no arquivo MainActivity.kt corresponde ao endereço MAC do seu carrinho.

Compile e instale em um smartwatch com Wear OS 3.0 ou superior.

Dê as permissões de Bluetooth e Localização ao abrir o app.

📱 Tecnologias Utilizadas
Linguagem: Kotlin

UI: Jetpack Compose for Wear OS

Comunicação: Bluetooth Low Energy (BLE) / GATT

Arquitetura: State-driven UI (Material 3)

👤 Autor
Desenvolvido por Christian — Desenvolvedor FullStack e entusiasta de eletrônica/hardware.
