package com.dafealru.smartlogin.qr;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import java.awt.Color;

/**
 * Pixel-Perfect Minecraft MapRenderer for 2FA QR Codes.
 */
public class QrMapRenderer extends MapRenderer {

    private final String qrData;
    private boolean rendered = false;

    public QrMapRenderer(String qrData) {
        super(true); // Contextual to the player
        this.qrData = qrData;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (rendered) return;

        // 128x128 Minecraft Map canvas
        boolean[][] matrix = QrMatrixGenerator.generateQrMatrix(qrData, 128, 128);

        @SuppressWarnings("deprecation")
        byte white = MapPalette.matchColor(Color.WHITE);
        @SuppressWarnings("deprecation")
        byte black = MapPalette.matchColor(Color.BLACK);

        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 128; y++) {
                byte color = matrix[x][y] ? black : white;
                canvas.setPixel(x, y, color);
            }
        }
        rendered = true;
    }
}
