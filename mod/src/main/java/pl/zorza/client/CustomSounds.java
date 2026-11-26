package pl.zorza.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Util;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Własne dźwięki z folderu config/zorza/sounds (pliki .ogg i .wav).
 * Wbudowane dźwięki Minecrafta mają nazwy z listy, a pliki gracza – nazwę pliku.
 */
public final class CustomSounds {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("zorza").resolve("sounds");
    private static final Map<String, Integer> BUFFERS = new HashMap<>();
    private static final List<Integer> SOURCES = new ArrayList<>();
    private static long lastScan = 0;
    private static List<String> files = List.of();

    private CustomSounds() {}

    public static void init() {
        try {
            Files.createDirectories(DIR);
            Files.deleteIfExists(DIR.resolve("CZYTAJ.txt"));     // stara nazwa
            Path readme = DIR.resolve("README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, """
                        === POLSKI ===
                        Wrzuć tutaj własne dźwięki w formacie .ogg albo .wav.
                        Potem w grze: prawy Shift -> Mod Menu -> Hit Sounds (albo Kill Sounds) -> Sound
                        i wybierz swój plik strzałkami. Najlepiej działają krótkie dźwięki (do 2 sekund).


                        === ENGLISH ===
                        Put your own sounds here as .ogg or .wav files.
                        Then in game: Right Shift -> Mod Menu -> Hit Sounds (or Kill Sounds) -> Sound
                        and pick your file with the arrows. Short sounds (up to 2 seconds) work best.
                        """);
            }
        } catch (Exception e) {
            ZorzaClient.LOG.warn("Zorza: nie udało się utworzyć folderu dźwięków", e);
        }
    }

    /** Pliki dźwięków gracza (odświeżane co 2 s). */
    public static List<String> files() {
        long now = System.currentTimeMillis();
        if (now - lastScan > 2000) {
            lastScan = now;
            List<String> out = new ArrayList<>();
            try (Stream<Path> st = Files.list(DIR)) {
                st.map(f -> f.getFileName().toString())
                        .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".ogg") || n.toLowerCase(Locale.ROOT).endsWith(".wav"))
                        .sorted(String.CASE_INSENSITIVE_ORDER).forEach(out::add);
            } catch (Exception ignored) {
            }
            files = out;
        }
        return files;
    }

    public static List<String> options(Map<String, SoundEvent> builtins) {
        List<String> o = new ArrayList<>(builtins.keySet());
        o.addAll(files());
        return o;
    }

    public static Map<String, SoundEvent> map(Object... pairs) {
        Map<String, SoundEvent> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], (SoundEvent) pairs[i + 1]);
        return m;
    }

    public static void openFolder() {
        init();
        Util.getOperatingSystem().open(DIR.toFile());
    }

    public static void play(String name, Map<String, SoundEvent> builtins, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SoundEvent ev = builtins.get(name);
        if (ev != null || name == null) {
            if (ev == null) ev = builtins.values().iterator().next();
            mc.getSoundManager().play(pl.zorza.client.compat.Gfx.uiSound(ev, pitch, volume));
            return;
        }
        try {
            playFile(name, volume * mc.options.getSoundVolume(SoundCategory.MASTER), pitch);
        } catch (Throwable t) {
            ZorzaClient.LOG.warn("Zorza: nie udało się odtworzyć {}", name, t);
        }
    }

    private static void playFile(String name, float volume, float pitch) throws Exception {
        if (ALC10.alcGetCurrentContext() == 0L) return;     // brak dźwięku w grze
        cleanup();
        Path f = DIR.resolve(name).normalize();
        if (!f.startsWith(DIR) || !Files.isRegularFile(f)) return;
        String key = name + "|" + Files.getLastModifiedTime(f).toMillis();
        Integer buf = BUFFERS.get(key);
        if (buf == null) {
            buf = load(f);
            if (buf == null) return;
            BUFFERS.put(key, buf);
        }
        int src = AL10.alGenSources();
        AL10.alSourcei(src, AL10.AL_BUFFER, buf);
        AL10.alSourcef(src, AL10.AL_GAIN, Math.max(0f, Math.min(1f, volume)));
        AL10.alSourcef(src, AL10.AL_PITCH, pitch);
        AL10.alSourcei(src, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(src, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcePlay(src);
        SOURCES.add(src);
    }

    /** Usuwa źródła, które skończyły grać – wywoływane co tick. */
    public static void cleanup() {
        if (SOURCES.isEmpty() || ALC10.alcGetCurrentContext() == 0L) return;
        for (Iterator<Integer> it = SOURCES.iterator(); it.hasNext(); ) {
            int s = it.next();
            if (AL10.alGetSourcei(s, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(s);
                it.remove();
            }
        }
    }

    private static Integer load(Path f) throws Exception {
        String n = f.getFileName().toString().toLowerCase(Locale.ROOT);
        int buf;
        if (n.endsWith(".ogg")) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ch = stack.mallocInt(1), rate = stack.mallocInt(1);
                ShortBuffer pcm = STBVorbis.stb_vorbis_decode_filename(f.toAbsolutePath().toString(), ch, rate);
                if (pcm == null) return null;
                try {
                    buf = AL10.alGenBuffers();
                    AL10.alBufferData(buf, ch.get(0) == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, rate.get(0));
                } finally {
                    LibCStdlib.free(pcm);
                }
            }
            return buf;
        }
        // WAV (PCM 8/16 bit)
        byte[] all = Files.readAllBytes(f);
        ByteBuffer bb = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
        if (all.length < 12 || bb.getInt(0) != 0x46464952 || bb.getInt(8) != 0x45564157) return null; // RIFF / WAVE
        int pos = 12, channels = 0, rate = 0, bits = 0, dataPos = -1, dataLen = 0;
        while (pos + 8 <= all.length) {
            int id = bb.getInt(pos), len = bb.getInt(pos + 4);
            if (len < 0) break;
            if (id == 0x20746D66) { // "fmt "
                if (bb.getShort(pos + 8) != 1) return null;          // tylko PCM
                channels = bb.getShort(pos + 10);
                rate = bb.getInt(pos + 12);
                bits = bb.getShort(pos + 22);
            } else if (id == 0x61746164) { // "data"
                dataPos = pos + 8;
                dataLen = Math.min(len, all.length - dataPos);
                break;
            }
            pos += 8 + len + (len & 1);
        }
        if (dataPos < 0 || channels < 1 || channels > 2 || (bits != 8 && bits != 16)) return null;
        int fmt = channels == 1 ? (bits == 8 ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16)
                : (bits == 8 ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16);
        ByteBuffer data = MemoryUtil.memAlloc(dataLen);
        try {
            data.put(all, dataPos, dataLen).flip();
            buf = AL10.alGenBuffers();
            AL10.alBufferData(buf, fmt, data, rate);
        } finally {
            MemoryUtil.memFree(data);
        }
        return buf;
    }
}
