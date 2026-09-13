# Guía de Migración Universal

Migra desde otros plugins de login sin perder ninguna cuenta:

```bash
# Migración automática de todo lo que exista
/smartlogin migrate all

# Migración específica
/smartlogin migrate authme
/smartlogin migrate nlogin
/smartlogin migrate fastlogin
/smartlogin migrate crazylogin
/smartlogin migrate xauth
/smartlogin migrate loginsecurity
```

Todas las contraseñas migradas se actualizan a **Argon2id** de forma transparente cuando el usuario inicia sesión.
