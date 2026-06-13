package com.mchub.util;

import java.io.IOException;
import java.io.InputStream;

public final class AudioMagicBytesValidator {

    private AudioMagicBytesValidator() {}

    /**
     * Returns true if the file's first bytes match a known audio format signature.
     * Prevents MIME-type spoofing (e.g., renaming a .html file to .webm).
     */
    public static boolean isValidAudio(InputStream stream) throws IOException {
        byte[] header = new byte[12];
        int read = stream.read(header);
        if (read < 4) return false;

        // WebM / MKV: starts with 0x1A 0x45 0xDF 0xA3
        if (header[0] == 0x1A && header[1] == 0x45 && (byte)header[2] == (byte)0xDF && (byte)header[3] == (byte)0xA3) return true;

        // WAV: RIFF....WAVE
        if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && read >= 12 && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E') return true;

        // MP3: ID3 tag or sync frame 0xFF 0xEx / 0xFF 0xFx
        if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') return true;
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0) return true;

        // OGG: OggS
        if (header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S') return true;

        // MP4 / M4A: ftyp box at offset 4
        if (read >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p') return true;

        return false;
    }
}
