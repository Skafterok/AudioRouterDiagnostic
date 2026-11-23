package com.webster.audiorouterdiagnostic;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private TextView reportView;
    private String latestReport = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(18));
        page.setBackgroundColor(0xFFF4F6F8);

        TextView title = new TextView(this);
        title.setText("Audio Router Diagnostic");
        title.setTextSize(23);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(0xFF17212B);
        page.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Диагностика Bluetooth и аудиомаршрутов Android");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF52616B);
        subtitle.setPadding(0, dp(5), 0, dp(14));
        page.addView(subtitle);

        Button refresh = new Button(this);
        refresh.setText("ОБНОВИТЬ ДИАГНОСТИКУ");
        page.addView(refresh);

        Button copy = new Button(this);
        copy.setText("СКОПИРОВАТЬ ОТЧЁТ");
        page.addView(copy);

        ScrollView scroll = new ScrollView(this);
        reportView = new TextView(this);
        reportView.setTextSize(13);
        reportView.setTextColor(0xFF17212B);
        reportView.setTypeface(Typeface.MONOSPACE);
        reportView.setPadding(dp(12), dp(12), dp(12), dp(12));
        reportView.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(reportView);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        lp.topMargin = dp(12);
        page.addView(scroll, lp);

        setContentView(page);

        refresh.setOnClickListener(v -> refreshReport());
        copy.setOnClickListener(v -> copyReport());

        refreshReport();
    }

    private void refreshReport() {
        StringBuilder s = new StringBuilder();

        s.append("AUDIO ROUTER DIAGNOSTIC\n");
        s.append("Время: ")
                .append(new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date()))
                .append("\n");

        s.append("Устройство: ")
                .append(android.os.Build.MANUFACTURER)
                .append(" ")
                .append(android.os.Build.MODEL)
                .append("\n");

        s.append("Android: ")
                .append(android.os.Build.VERSION.RELEASE)
                .append(" (API ")
                .append(android.os.Build.VERSION.SDK_INT)
                .append(")\n");

        s.append("Прошивка: ")
                .append(android.os.Build.DISPLAY)
                .append("\n");

        s.append("MIUI: ")
                .append(getSystemProperty("ro.miui.ui.version.name"))
                .append("\n\n");

        AudioManager am = (AudioManager)
                getSystemService(Context.AUDIO_SERVICE);

        s.append("=== СИСТЕМНЫЙ АУДИОРЕЖИМ ===\n");

        s.append("AudioManager mode: ")
                .append(audioModeName(am.getMode()))
                .append(" (")
                .append(am.getMode())
                .append(")\n");

        s.append("Bluetooth A2DP active flag: ")
                .append(am.isBluetoothA2dpOn())
                .append("\n");

        s.append("Bluetooth SCO active flag: ")
                .append(am.isBluetoothScoOn())
                .append("\n");

        s.append("Speakerphone on: ")
                .append(am.isSpeakerphoneOn())
                .append("\n");

        s.append("Microphone muted: ")
                .append(am.isMicrophoneMute())
                .append("\n");

        s.append("Music stream volume: ")
                .append(am.getStreamVolume(AudioManager.STREAM_MUSIC))
                .append("/")
                .append(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                .append("\n");

        s.append("Voice-call stream volume: ")
                .append(am.getStreamVolume(AudioManager.STREAM_VOICE_CALL))
                .append("/")
                .append(am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL))
                .append("\n\n");

        s.append("=== BLUETOOTH ===\n");

        BluetoothAdapter adapter =
                BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            s.append("Bluetooth adapter: отсутствует\n");
        } else {
            s.append("Bluetooth state: ")
                    .append(adapter.isEnabled()
                            ? "ВКЛЮЧЁН"
                            : "ВЫКЛЮЧЕН")
                    .append("\n");

            s.append("A2DP profile state: ")
                    .append(profileState(
                            adapter.getProfileConnectionState(
                                    BluetoothProfile.A2DP)))
                    .append("\n");

            s.append("HEADSET profile state: ")
                    .append(profileState(
                            adapter.getProfileConnectionState(
                                    BluetoothProfile.HEADSET)))
                    .append("\n");

            s.append("Сопряжённые устройства:\n");

            try {
                Set<BluetoothDevice> bonded =
                        adapter.getBondedDevices();

                if (bonded == null || bonded.isEmpty()) {
                    s.append("  Нет сопряжённых устройств\n");
                } else {
                    for (BluetoothDevice d : bonded) {
                        s.append("  • ")
                                .append(safeName(d))
                                .append("\n");
                    }
                }
            } catch (SecurityException e) {
                s.append("  Нет разрешения на чтение Bluetooth-устройств\n");
            }
        }

        s.append("\n=== ДОСТУПНЫЕ АУДИОУСТРОЙСТВА ===\n");

        appendAudioDevices(
                s, am,
                AudioManager.GET_DEVICES_OUTPUTS,
                "ВЫХОДЫ");

        appendAudioDevices(
                s, am,
                AudioManager.GET_DEVICES_INPUTS,
                "ВХОДЫ");

        s.append("\n=== ВАЖНО ===\n");

        s.append("Отчёт показывает системные флаги и доступные устройства.\n");

        s.append("Он не раскрывает внутренние AudioTrack и AudioAttributes других приложений.\n");

        s.append("A2DP/SCO-флаги сами по себе не доказывают, какой звук сейчас слышен на каждом устройстве.\n");

        latestReport = s.toString();
        reportView.setText(latestReport);
    }

    private void appendAudioDevices(
            StringBuilder s,
            AudioManager am,
            int direction,
            String label) {

        s.append(label).append(":\n");

        AudioDeviceInfo[] devices = am.getDevices(direction);

        if (devices == null || devices.length == 0) {
            s.append("  Не обнаружены\n");
            return;
        }

        for (AudioDeviceInfo d : devices) {
            s.append("  • ")
                    .append(deviceTypeName(d.getType()))
                    .append(" | ")
                    .append(d.getProductName())
                    .append(" | id=")
                    .append(d.getId())
                    .append(" | ")
                    .append(d.isSource() ? "source" : "sink")
                    .append("\n");
        }
    }

    private String safeName(BluetoothDevice d) {
        try {
            String n = d.getName();

            return (n == null || n.trim().isEmpty())
                    ? "(имя не предоставлено)"
                    : n;

        } catch (SecurityException e) {
            return "(имя недоступно)";
        }
    }

    private String profileState(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "CONNECTED";

            case BluetoothProfile.STATE_CONNECTING:
                return "CONNECTING";

            case BluetoothProfile.STATE_DISCONNECTING:
                return "DISCONNECTING";

            case BluetoothProfile.STATE_DISCONNECTED:
                return "DISCONNECTED";

            default:
                return "UNKNOWN (" + state + ")";
        }
    }

    private String audioModeName(int mode) {
switch (mode) {
case AudioManager.MODE_NORMAL:
return "NORMAL";

    case AudioManager.MODE_RINGTONE:
        return "RINGTONE";

    case AudioManager.MODE_IN_CALL:
        return "IN_CALL";

    case AudioManager.MODE_IN_COMMUNICATION:
        return "IN_COMMUNICATION";

    default:
        return "UNKNOWN (" + mode + ")";
}

}
    private String deviceTypeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return "BUILTIN_EARPIECE";

            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return "BUILTIN_SPEAKER";

            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return "WIRED_HEADSET";

            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return "WIRED_HEADPHONES";

            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return "BLUETOOTH_SCO";

            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return "BLUETOOTH_A2DP";

            case AudioDeviceInfo.TYPE_HDMI:
                return "HDMI";

            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return "USB_DEVICE";

            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return "USB_HEADSET";

            case AudioDeviceInfo.TYPE_BUILTIN_MIC:
                return "BUILTIN_MIC";

            case AudioDeviceInfo.TYPE_TELEPHONY:
                return "TELEPHONY";

            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return "LINE_ANALOG";

            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return "LINE_DIGITAL";

            default:
                return "TYPE_" + type;
        }
    }

    private String getSystemProperty(String key) {
        try {
            Class<?> c =
                    Class.forName("android.os.SystemProperties");

            java.lang.reflect.Method m =
                    c.getMethod("get", String.class, String.class);

            return (String) m.invoke(
                    null, key, "не определена");

        } catch (Exception e) {
            return "недоступна";
        }
    }

    private void copyReport() {
        if (latestReport.isEmpty()) {
            refreshReport();
        }

        ClipboardManager clipboard =
                (ClipboardManager)
                        getSystemService(CLIPBOARD_SERVICE);

        clipboard.setPrimaryClip(
                ClipData.newPlainText(
                        "Audio Router Diagnostic",
                        latestReport));

        Toast.makeText(
                this,
                "Отчёт скопирован. Вставь его в чат.",
                Toast.LENGTH_LONG
        ).show();
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f);
    }
}
