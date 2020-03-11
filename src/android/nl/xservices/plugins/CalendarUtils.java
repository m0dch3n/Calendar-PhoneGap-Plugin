package nl.xservices.plugins;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public final class CalendarUtils {

    private static final Map<Integer, Integer> sUpdatedColors;

    static {
        Map hashMap = new HashMap();
        sUpdatedColors = hashMap;
        hashMap.put(-509406, -2818048);
        sUpdatedColors.put(-370884, -765666);
        sUpdatedColors.put(-35529, -1086464);
        sUpdatedColors.put(-21178, -1010944);
        sUpdatedColors.put(-339611, -606426);
        sUpdatedColors.put(-267901, -1784767);
        sUpdatedColors.put(-4989844, -4142541);
        sUpdatedColors.put(-8662712, -8604862);
        sUpdatedColors.put(-15292571, -16023485);
        sUpdatedColors.put(-12396910, -16738680);
        sUpdatedColors.put(-7151168, -13388167);
        sUpdatedColors.put(-6299161, -16540699);
        sUpdatedColors.put(-6306073, -12417548);
        sUpdatedColors.put(-11958553, -12627531);
        sUpdatedColors.put(-6644481, -8812853);
        sUpdatedColors.put(-4613377, -5005861);
        sUpdatedColors.put(-5997854, -6395473);
        sUpdatedColors.put(-3312410, -7461718);
        sUpdatedColors.put(-3365204, -5434281);
        sUpdatedColors.put(-618062, -2614432);
        sUpdatedColors.put(-3118236, -1672077);
        sUpdatedColors.put(-5475746, -8825528);
        sUpdatedColors.put(-4013374, -10395295);
        sUpdatedColors.put(-3490369, -5792882);
        sUpdatedColors.put(-2350809, -2818048);
        sUpdatedColors.put(-18312, -765666);
        sUpdatedColors.put(-272549, -606426);
        sUpdatedColors.put(-11421879, -16023485);
        sUpdatedColors.put(-8722497, -13388167);
        sUpdatedColors.put(-12134693, -16540699);
        sUpdatedColors.put(-11238163, -12627531);
        sUpdatedColors.put(-5980676, -8812853);
        sUpdatedColors.put(-2380289, -7461718);
        sUpdatedColors.put(-30596, -1672077);
        sUpdatedColors.put(-1973791, -10395295);
        sUpdatedColors.put(-2883584, -2818048);
        sUpdatedColors.put(-831459, -765666);
        sUpdatedColors.put(-1152256, -1086464);
        sUpdatedColors.put(-1076736, -1010944);
        sUpdatedColors.put(-672219, -606426);
        sUpdatedColors.put(-1914036, -1784767);
        sUpdatedColors.put(-4208334, -4142541);
        sUpdatedColors.put(-8670655, -8604862);
        sUpdatedColors.put(-16089278, -16023485);
        sUpdatedColors.put(-16738937, -16738680);
        sUpdatedColors.put(-16606492, -16540699);
        sUpdatedColors.put(-12483341, -12417548);
        sUpdatedColors.put(-12624727, -12627531);
        sUpdatedColors.put(-8878646, -8812853);
        sUpdatedColors.put(-5071654, -5005861);
        sUpdatedColors.put(-7527511, -7461718);
        sUpdatedColors.put(-5500074, -5434281);
        sUpdatedColors.put(-2680225, -2614432);
        sUpdatedColors.put(-1737870, -1672077);
        sUpdatedColors.put(-8891321, -8825528);
        sUpdatedColors.put(-10263709, -10395295);
    }

    public static int getDisplayColor(int color) {
        if (sUpdatedColors.containsKey(color)) {
            return (sUpdatedColors.get(color));
        }
        if (sUpdatedColors.containsValue(color)) {
            return color;
        }
        float[] fArr = new float[3];
        Color.colorToHSV(color, fArr);
        if (fArr[2] > 0.79f) {
            fArr[1] = Math.min(fArr[1] * 1.3f, 1.0f);
            fArr[2] = fArr[2] * 0.8f;
        }
        return Color.HSVToColor(Color.alpha(color), fArr);
    }

    public static String getDisplayColorHex(int color) {
        return String.format("#%06X", 0xFFFFFF & CalendarUtils.getDisplayColor(color));
    }
}