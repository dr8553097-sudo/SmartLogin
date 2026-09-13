# Suite de Autenticación de Dos Factores (2FA)

SmartLogin incluye 4 métodos 2FA simultáneos:

### 1. Google Authenticator / TOTP
- Comando: `/2fa setup`
- El jugador recibe un mapa en mano con el código QR.
- Escanea con Google Authenticator / Authy y confirma con `/2fa verify <code>`.

### 2. Bot de Discord
- Comando: `/link`
- El jugador recibe un código de 6 dígitos y se lo envía por mensaje directo al Bot de Discord.

### 3. Bot de Telegram
- Comando: `/tlink`
- Recibe notificaciones y códigos OTP de un solo uso por Telegram.

### 4. Correo Electrónico (Email OTP)
- Comando: `/email add <correo>`
- Envía códigos de verificación vía servidor SMTP seguro.
