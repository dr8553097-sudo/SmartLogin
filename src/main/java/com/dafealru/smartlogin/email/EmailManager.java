package com.dafealru.smartlogin.email;

import com.dafealru.smartlogin.SmartLogin;
import com.dafealru.smartlogin.database.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Enterprise Email / Gmail 2FA Engine for SmartLogin.
 * Sends asynchronous SMTP STARTTLS authentication codes, 2FA challenges, and recovery OTPs.
 */
public class EmailManager {

    private final SmartLogin plugin;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public record EmailVerification(String email, String code, long expiresAt) {}

    private final Map<UUID, EmailVerification> pendingVerifications = new ConcurrentHashMap<>();
    private final Map<UUID, EmailVerification> pending2faCodes = new ConcurrentHashMap<>();

    public EmailManager(SmartLogin plugin) {
        this.plugin = plugin;
    }

    public boolean isConfigured() {
        boolean enabled = plugin.getModularConfig().getEmailConfig().getBoolean("enabled", false);
        String host = plugin.getModularConfig().getEmailConfig().getString("smtp.host", "smtp.gmail.com");
        String username = plugin.getModularConfig().getEmailConfig().getString("smtp.username", "");
        return enabled && host != null && !host.isEmpty() && username != null && !username.isEmpty() && !username.contains("your-server-email");
    }

    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "N/A";
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.substring(0, 2) + "***@" + domain;
    }

    public boolean startVerification(Player player, String email) {
        if (!isValidEmail(email)) return false;

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        long expiresAt = System.currentTimeMillis() + (10 * 60 * 1000); // 10 minutes
        pendingVerifications.put(player.getUniqueId(), new EmailVerification(email.trim(), code, expiresAt));

        sendSmtpEmail(
                email.trim(),
                "🔐 SmartLogin — Código de Verificación de Correo",
                "Hola " + player.getName() + ",\n\n" +
                "Tu código de verificación para vincular tu correo en el servidor es: " + code + "\n\n" +
                "Escribe en el juego:\n/email verify " + code + "\n\n" +
                "Este código expira en 10 minutos.\n\n— SmartLogin Security Suite"
        );
        return true;
    }

    public boolean completeVerification(Player player, String code) {
        EmailVerification ver = pendingVerifications.get(player.getUniqueId());
        if (ver == null) return false;
        if (System.currentTimeMillis() > ver.expiresAt()) {
            pendingVerifications.remove(player.getUniqueId());
            return false;
        }
        if (!ver.code().equals(code.trim())) {
            return false;
        }

        PlayerProfile profile = plugin.getAuthManager().getProfile(player.getUniqueId());
        if (profile != null) {
            profile.setEmail(ver.email());
            plugin.getDatabaseManager().saveProfile(profile);
            pendingVerifications.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean send2faCode(Player player, PlayerProfile profile) {
        if (profile.getEmail() == null || profile.getEmail().isEmpty()) return false;

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        long expiresAt = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes
        pending2faCodes.put(player.getUniqueId(), new EmailVerification(profile.getEmail(), code, expiresAt));

        sendSmtpEmail(
                profile.getEmail(),
                "🚨 SmartLogin — Código 2FA de Inicio de Sesión",
                "Hola " + player.getName() + ",\n\n" +
                "Se ha detectado un intento de inicio de sesión en tu cuenta.\n" +
                "Tu código de verificación 2FA es: " + code + "\n\n" +
                "Escribe en el chat o consola del juego: /2fa verify " + code + "\n\n" +
                "Si tú no fuiste, entra al servidor y presiona el Botón de Pánico en /security.\n\n— SmartLogin Security Suite"
        );
        return true;
    }

    public boolean verify2faCode(Player player, String code) {
        EmailVerification ver = pending2faCodes.get(player.getUniqueId());
        if (ver == null) return false;
        if (System.currentTimeMillis() > ver.expiresAt()) {
            pending2faCodes.remove(player.getUniqueId());
            return false;
        }
        if (ver.code().equals(code.trim())) {
            pending2faCodes.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void sendSmtpEmail(String recipient, String subject, String bodyText) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String host = plugin.getModularConfig().getEmailConfig().getString("smtp.host", "smtp.gmail.com");
                int port = plugin.getModularConfig().getEmailConfig().getInt("smtp.port", 587);
                boolean starttls = plugin.getModularConfig().getEmailConfig().getBoolean("smtp.starttls", true);
                String user = plugin.getModularConfig().getEmailConfig().getString("smtp.username", "");
                String pass = plugin.getModularConfig().getEmailConfig().getString("smtp.password", "");
                String from = plugin.getModularConfig().getEmailConfig().getString("smtp.from", user);

                if (user == null || user.isEmpty() || pass == null || pass.isEmpty() || user.contains("your-server-email")) {
                    plugin.getLogger().warning("[SmartLogin Email] No se pudo enviar el correo a " + recipient + ": Credenciales SMTP no configuradas en 2fa/email.yml");
                    return;
                }

                Socket socket = new Socket();
                socket.connect(new java.net.InetSocketAddress(host, port), 5000);
                socket.setSoTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                reader.readLine(); // 220 banner

                writer.println("EHLO localhost");
                readResponse(reader);

                if (starttls) {
                    writer.println("STARTTLS");
                    reader.readLine(); // 220 2.0.0 Ready to start TLS

                    SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                    SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, host, port, true);
                    sslSocket.setSoTimeout(5000);
                    sslSocket.startHandshake();

                    reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), StandardCharsets.UTF_8));
                    writer = new PrintWriter(new OutputStreamWriter(sslSocket.getOutputStream(), StandardCharsets.UTF_8), true);

                    writer.println("EHLO localhost");
                    readResponse(reader);
                }

                // AUTH LOGIN
                writer.println("AUTH LOGIN");
                reader.readLine(); // 334 VXNlcm5hbWU6
                writer.println(Base64.getEncoder().encodeToString(user.getBytes(StandardCharsets.UTF_8)));
                reader.readLine(); // 334 UGFzc3dvcmQ6
                writer.println(Base64.getEncoder().encodeToString(pass.getBytes(StandardCharsets.UTF_8)));
                String authResp = reader.readLine(); // 235 2.7.0 Authentication successful or 535 error
                if (authResp != null && authResp.startsWith("535")) {
                    plugin.getLogger().warning("[SmartLogin Email] Error de autenticación SMTP con " + host + ": Credenciales inválidas. Si usas Gmail, asegúrate de usar una 'Contraseña de Aplicación' (App Password) de 16 letras, no tu contraseña normal.");
                    socket.close();
                    return;
                }

                writer.println("MAIL FROM:<" + user + ">");
                reader.readLine();
                writer.println("RCPT TO:<" + recipient + ">");
                reader.readLine();
                writer.println("DATA");
                reader.readLine();

                writer.println("From: " + from);
                writer.println("To: " + recipient);
                writer.println("Subject: " + subject);
                writer.println("Content-Type: text/plain; charset=UTF-8");
                writer.println();
                writer.println(bodyText);
                writer.println(".");
                reader.readLine();

                writer.println("QUIT");
                socket.close();
                plugin.getLogger().info("[SmartLogin Email] Correo OTP enviado con éxito a " + maskEmail(recipient));
            } catch (Exception e) {
                plugin.getLogger().warning("[SmartLogin Email] Error al enviar correo SMTP a " + recipient + ": " + e.getMessage());
            }
        });
    }

    private void readResponse(BufferedReader reader) throws Exception {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() >= 4 && line.charAt(3) == ' ') break;
        }
    }
}
