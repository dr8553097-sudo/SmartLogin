# <div align="center">🛡️ SmartLogin v1.0.1-BETA</div>

<div align="center">
  <img src="https://raw.githubusercontent.com/dr8553097-sudo/SmartLogin/main/assets/logo.png" alt="SmartLogin Logo" width="180"/>
  <br/>
  <strong>La Suite Definitiva de Autenticación, Identidad y Ciberseguridad para Servidores Modernos de Minecraft.</strong>
  <br/><br/>
  
  [![Release](https://img.shields.io/badge/Release-v1.0.1--BETA-9333EA?style=for-the-badge&logo=github)](https://github.com/dr8553097-sudo/SmartLogin/releases)
  [![Platform](https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur%201.21.x-9333EA?style=for-the-badge&logo=minecraft)](https://papermc.io)
  [![Java](https://img.shields.io/badge/Java-21-9333EA?style=for-the-badge&logo=openjdk)](https://adoptium.net)
  [![Security](https://img.shields.io/badge/Crypto-Argon2id%20OWASP-9333EA?style=for-the-badge&logo=lock)](https://github.com/dr8553097-sudo/SmartLogin)
  [![Discord](https://img.shields.io/badge/Discord-Community-5865F2?style=for-the-badge&logo=discord)](https://discord.gg/smartlogin)
</div>

---

> [!NOTE]
> ⚠️ **Estado del Proyecto: FASE BETA PÚBLICA**  
> SmartLogin es un proyecto activo y de vanguardia. Al ser un sistema extenso con múltiples módulos avanzados, te invitamos a reportar cualquier comportamiento inusual o sugerencia en nuestro [Sistema de Tickets de GitHub](https://github.com/dr8553097-sudo/SmartLogin/issues) o en nuestro servidor de [Discord](https://discord.gg/smartlogin).

---

## 🎙️ Manifiesto: "Minecraft evolucionó, la seguridad también debía hacerlo"

Durante casi una década, los servidores han dependido de plugins de autenticación creados en 2014–2016, llenos de parches, algoritmos obsoletos y menús de texto plano en el chat.

**SmartLogin nace para cerrar esa brecha.** Demuestra que un plugin de login no tiene por qué ser aburrido ni antiguo; puede ser moderno, visual, cinematográfico y nativo para la era de **Paper 1.21 y Java 21**.

> *"No creamos otro plugin de login; creamos la suite de identidad que Minecraft moderno se merecía desde hace años."*

---

## 🌟 Los 4 Pilares de SmartLogin

### 1. 🛡️ Democratizar la Seguridad de Élite
Tecnologías como el **2FA con códigos QR en mapas**, la criptografía **Argon2id** (estándar militar de la OWASP, resistente a GPUs) o la detección de bots y multicuentas solían ser exclusivas de redes con miles de dólares de presupuesto o plugins cerrados de pago. **SmartLogin es 100% gratuito y de código abierto para toda la comunidad.**

### 2. 👥 Una Experiencia Humana, No Burocrática
- **Móvil / Bedrock (Geyser/Floodgate):** Teclado numérico táctil interactivo (PinPad GUI) y auto-login criptográfico seguro.
- **2FA con Google Authenticator / Authy:** Recibe un mapa visual en la mano con el código QR y vincúlalo en 5 segundos.
- **Recuperación Autónoma:** Si un jugador olvida su contraseña, su bot de Telegram o Discord le ayuda en 10 segundos sin esperar días por un administrador.
- **Modo Streamer Inteligente:** Intercepta contraseñas enviadas accidentalmente al chat sin barra (`/login`), oculta IPs y correos en menús.

### 3. ⚡ Rendimiento Asíncrono & Cero Humo (0.00% TPS Impact)
Todas las operaciones criptográficas, consultas de base de datos y validaciones de red se ejecutan en hilos secundarios asíncronos (`ForkJoinPool` / `CompletableFuture`). Con el comando `/smartlogin diag`, puedes auditar en tiempo real el impacto exacto y la salud de tu servidor.

### 4. 🚀 Migración Universal sin Dolor
Cambia a SmartLogin desde **AuthMe, nLogin, FastLogin, CrazyLogin, xAuth o LoginSecurity** en 1 solo comando (`/smartlogin migrate all`) importando más de 50.000 cuentas en 1 segundo gracias a nuestras **Batch Transactions**. Además, las contraseñas antiguas se actualizan automáticamente a **Argon2id** en el primer inicio de sesión del jugador.

---

## ⚙️ Características Principales

- 🔐 **Criptografía Multi-Algoritmo:** Argon2id (por defecto), PBKDF2-SHA512, BCrypt (cost=12) y SHA-256.
- 📱 **Suite 2FA Completa:**
  - Códigos QR renderizados en mapas de Minecraft.
  - Aprobación 2FA con 1 click mediante Bot de Discord (DM).
  - Códigos OTP instantáneos vía Bot de Telegram.
  - Códigos de verificación vía Correo Electrónico (SMTP).
- 🎮 **Soporte Nativo Bedrock (Geyser/Floodgate):** Detección criptográfica anti-suplantación y formularios Bedrock Forms.
- 👑 **Auto-Login Java Premium:** Detección de firmas criptográficas de texturas Mojang y compatibilidad con FastLogin.
- 🎨 **Inmersión Visual:** BossBar animada con cuenta regresiva, ActionBars, sonidos hápticos y títulos de bienvenida.
- 🔨 **Anvil GUI:** Posibilidad de registrarse e iniciar sesión mediante yunques interactivos.
- 🌍 **Geo-IP & Impossible Travel:** Detección de cambios de país inesperados y solicitud automática de 2FA.
- 🌐 **Multi-Idioma Dinámico:** Inglés, Español, Portugués, Francés, Alemán, Ruso y Chino con auto-detección del cliente.

---

## 📜 Comandos & Permisos

### Comandos de Jugador
| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/login <clave>` | Iniciar sesión en el servidor | *Todos* |
| `/register <clave> <repetir>` | Registrar nueva cuenta | *Todos* |
| `/changepassword <antigua> <nueva>` | Cambiar contraseña actual | *Todos* |
| `/security` | Abrir el Centro de Seguridad y 2FA | *Todos* |
| `/streamer` | Activar/desactivar Modo Streamer | *Todos* |
| `/2fa <setup|verify|disable>` | Gestionar Google Authenticator | *Todos* |
| `/link` | Vincular cuenta con Discord | *Todos* |
| `/tlink` | Vincular cuenta con Telegram | *Todos* |
| `/email <add|verify|code>` | Vincular y verificar correo | *Todos* |
| `/recover <email|telegram>` | Recuperar acceso a la cuenta | *Todos* |

### Comandos de Administrador (`smartlogin.admin`)
| Comando | Descripción |
| :--- | :--- |
| `/smartlogin gui` | Abre el Panel de Control interactivo de Administrador |
| `/smartlogin diag` | Diagnóstico de integridad de archivos y latencia en vivo |
| `/smartlogin benchmark` | Prueba de velocidad en tiempo real de los algoritmos de hashing |
| `/smartlogin migrate <all|authme|nlogin|fastlogin>` | Migración masiva e instantánea de bases de datos |
| `/smartlogin setpassword <usuario> <clave>` | Cambia forzosamente la clave de un jugador |
| `/smartlogin reset2fa <usuario>` | Restablece todos los métodos 2FA de un usuario |
| `/smartlogin unlink <usuario> [discord|email|all]` | Desvincula métodos específicos de seguridad |
| `/smartlogin unregister <usuario>` | Elimina una cuenta de la base de datos |
| `/smartlogin backup` | Genera una copia de seguridad SQLite en segundos |
| `/smartlogin reload` | Recarga en caliente todas las configs e idiomas |

---

## 📦 Instalación Rápida

1. Descarga el archivo **`SmartLogin-1.0.1.jar`** desde [Releases](https://github.com/dr8553097-sudo/SmartLogin/releases) o [Modrinth](https://modrinth.com/plugin/smartlogin-suite).
2. Coloca el archivo en la carpeta `/plugins/` de tu servidor **Paper / Purpur 1.21.x** (requiere **Java 21**).
3. Inicia el servidor.
4. *(Opcional)* Si vienes de otro plugin de login, ejecuta `/smartlogin migrate all` para transferir todas las cuentas al instante.
5. ¡Disfruta de la seguridad moderna en tu servidor!

---

## 💬 Comunidad & Soporte

- 🌐 **Wiki Oficial:** [https://github.com/dr8553097-sudo/SmartLogin/wiki](https://github.com/dr8553097-sudo/SmartLogin/wiki)
- 💬 **Discord Oficial:** [https://discord.gg/smartlogin](https://discord.gg/smartlogin)
- 🐛 **Reportar un Error:** [GitHub Issues](https://github.com/dr8553097-sudo/SmartLogin/issues)
- 👑 **Desarrollador:** **Dafealru** ([@dr8553097-sudo](https://github.com/dr8553097-sudo))

---

<div align="center">
  <sub>Construido con pasión para la comunidad de Minecraft. Licenciado bajo MIT / GPLv3.</sub>
</div>
