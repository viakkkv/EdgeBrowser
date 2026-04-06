package com.mc.edgebrowser.render;

public final class ColorConverter {

    private static final int[] PALETTE_RGB = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final float[][] PALETTE_HSL = new float[128][3];

    static {
        initPalette();
    }

    private static void initPalette() {
        int[] colors = {
            0xFF000000, 0xFF1E1B1B, 0xFF583333, 0xFF894444, 0xFFB26666, 0xFFD4946E, 0xFFE8C49A, 0xFFF2DCC4,
            0xFFFCEADE, 0xFFF9F4E8, 0xFFE4E4DA, 0xFFC0C0B8, 0xFF909088, 0xFF646460, 0xFF3C3C3A, 0xFF1A1A1A,
            0xFF551100, 0xFF882200, 0xFFAA3300, 0xFFCC5500, 0xFFEE7722, 0xFFBB5533, 0xFF994433, 0xFF773322,
            0xFF552211, 0xFF441100, 0xFF330800, 0xFF220400, 0xFF110200, 0xFF440000, 0xFF660000, 0xFF880000,
            0xFF004400, 0xFF006600, 0xFF008800, 0xFF22AA22, 0xFF44CC44, 0xFF228833, 0xFF116622, 0xFF004411,
            0xFF002200, 0xFF001100, 0xFF220000, 0xFF440000, 0xFF660000, 0xFF004400, 0xFF006600, 0xFF008800,
            0xFF443322, 0xFF665533, 0xFF887744, 0xFFAA9955, 0xFFCCBB66, 0xFFAA8855, 0xFF886633, 0xFF664422,
            0xFF442211, 0xFF331100, 0xFF220800, 0xFF110400, 0xFF221100, 0xFF332200, 0xFF443300, 0xFF554400,
            0xFF000044, 0xFF000066, 0xFF000088, 0xFF2222AA, 0xFF4444CC, 0xFF223388, 0xFF112266, 0xFF001144,
            0xFF000022, 0xFF000011, 0xFF000022, 0xFF000044, 0xFF000066, 0xFF000088, 0xFF0000AA, 0xFF0000CC,
            0xFF330044, 0xFF550066, 0xFF770088, 0xFF9922AA, 0xFFBB44CC, 0xFF883388, 0xFF662266, 0xFF441144,
            0xFF220022, 0xFF110011, 0xFF220022, 0xFF330044, 0xFF440066, 0xFF550088, 0xFF6600AA, 0xFF7700CC,
            0xFF004444, 0xFF006666, 0xFF008888, 0xFF22AAAA, 0xFF44CCCC, 0xFF228888, 0xFF116666, 0xFF004444,
            0xFF002222, 0xFF001111, 0xFF002222, 0xFF003333, 0xFF004444, 0xFF005555, 0xFF006666, 0xFF007777,
            0xFF111111, 0xFF222222, 0xFF333333, 0xFF444444, 0xFF555555, 0xFF666666, 0xFF777777, 0xFF888888,
            0xFF999999, 0xFFAAAAAA, 0xFFBBBBBB, 0xFFCCCCCC, 0xFFDDDDDD, 0xFFEEEEEE, 0xFFFFFFFF, 0xFFFFFFFF
        };

        for (int i = 0; i < 128 && i < colors.length; i++) {
            PALETTE_RGB[i] = colors[i];
            int r = (colors[i] >> 16) & 0xFF;
            int g = (colors[i] >> 8) & 0xFF;
            int b = colors[i] & 0xFF;
            float[] hsl = rgbToHsl(r, g, b);
            PALETTE_HSL[i][0] = hsl[0];
            PALETTE_HSL[i][1] = hsl[1];
            PALETTE_HSL[i][2] = hsl[2];
        }
    }

    public static byte rgbToMapColor(int r, int g, int b, int brightness) {
        r = clamp(r + brightness, 0, 255);
        g = clamp(g + brightness, 0, 255);
        b = clamp(b + brightness, 0, 255);
        return (byte) findClosestColor(r, g, b);
    }

    public static byte rgbToMapColor(int r, int g, int b) {
        return rgbToMapColor(r, g, b, 0);
    }

    public static byte argbToMapColor(int argb, int brightness) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        if (a < 255) {
            float af = a / 255f;
            r = (int)(r * af);
            g = (int)(g * af);
            b = (int)(b * af);
        }

        return rgbToMapColor(r, g, b, brightness);
    }

    private static int findClosestColor(int r, int g, int b) {
        float[] hsl = rgbToHsl(r, g, b);
        float h = hsl[0], s = hsl[1], l = hsl[2];

        int bestIdx = 0;
        float bestDist = Float.MAX_VALUE;

        for (int i = 0; i < 128; i++) {
            float dh = Math.abs(h - PALETTE_HSL[i][0]);
            if (dh > 0.5f) dh = 1.0f - dh;
            float ds = s - PALETTE_HSL[i][1];
            float dl = l - PALETTE_HSL[i][2];

            float dist = dh * dh * 0.2f + ds * ds * 0.3f + dl * dl * 3.0f;

            if (dist < bestDist) {
                bestDist = dist;
                bestIdx = i;
            }
        }

        return bestIdx;
    }

    public static void convertFrame(int[] argbPixels, byte[] mapColors, int brightness, int enhance) {
        int len = Math.min(argbPixels.length, mapColors.length);
        for (int i = 0; i < len; i++) {
            int argb = argbPixels[i];
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = argb & 0xFF;
            int a = (argb >> 24) & 0xFF;

            if (a < 255) {
                float af = a / 255f;
                r = (int)(r * af);
                g = (int)(g * af);
                b = (int)(b * af);
            }

            if (enhance > 0) {
                float ef = enhance / 100f;
                float gray = 0.299f * r + 0.587f * g + 0.114f * b;
                r = clamp((int)(r + (r - gray) * ef), 0, 255);
                g = clamp((int)(g + (g - gray) * ef), 0, 255);
                b = clamp((int)(b + (b - gray) * ef), 0, 255);
            }

            mapColors[i] = rgbToMapColor(r, g, b, brightness);
        }
    }

    private static float[] rgbToHsl(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float h, s, l = (max + min) / 2f;

        if (max == min) {
            h = s = 0;
        } else {
            float d = max - min;
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == rf) h = (gf - bf) / d + (gf < bf ? 6f : 0f);
            else if (max == gf) h = (bf - rf) / d + 2f;
            else h = (rf - gf) / d + 4f;
            h /= 6f;
        }
        return new float[]{h, s, l};
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
