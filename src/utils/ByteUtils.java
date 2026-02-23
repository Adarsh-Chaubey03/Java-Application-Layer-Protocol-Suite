package utils;

/**
 * Byte manipulation utilities for binary protocol operations.
 * Provides hex dump, byte-to-integer conversions, and other
 * low-level helpers used by DNS and DHCP packet builders.
 */
public class ByteUtils {

    /** Hex characters for formatting */
    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * Convert a byte array to a hex dump string for debugging.
     * Format: offset  hex-values  |  ASCII
     *
     * @param data the byte array to dump
     * @return formatted hex dump string
     */
    public static String hexDump(byte[] data) {
        return hexDump(data, data.length);
    }

    /**
     * Convert a byte array to a hex dump string with specified length.
     *
     * @param data   the byte array to dump
     * @param length number of bytes to include
     * @return formatted hex dump string
     */
    public static String hexDump(byte[] data, int length) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(length, data.length);

        for (int i = 0; i < limit; i += 16) {
            /* Offset */
            sb.append(String.format("%04X  ", i));

            /* Hex values */
            for (int j = 0; j < 16; j++) {
                if (i + j < limit) {
                    sb.append(String.format("%02X ", data[i + j] & 0xFF));
                } else {
                    sb.append("   ");
                }
                if (j == 7) sb.append(" ");
            }

            /* ASCII representation */
            sb.append(" |");
            for (int j = 0; j < 16 && (i + j) < limit; j++) {
                char c = (char) (data[i + j] & 0xFF);
                sb.append(c >= 32 && c < 127 ? c : '.');
            }
            sb.append("|\n");
        }
        return sb.toString();
    }

    /**
     * Convert a single byte to an unsigned integer.
     *
     * @param b the byte value
     * @return unsigned integer (0-255)
     */
    public static int toUnsignedInt(byte b) {
        return b & 0xFF;
    }

    /**
     * Read a 16-bit unsigned integer from two bytes (big-endian).
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @return the unsigned 16-bit value
     */
    public static int readUint16(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    /**
     * Read a 32-bit unsigned integer from four bytes (big-endian).
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @return the unsigned 32-bit value as a long
     */
    public static long readUint32(byte[] data, int offset) {
        return ((long) (data[offset] & 0xFF) << 24)
                | ((long) (data[offset + 1] & 0xFF) << 16)
                | ((long) (data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    /**
     * Write a 16-bit value to a byte array (big-endian).
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @param value  the 16-bit value to write
     */
    public static void writeUint16(byte[] data, int offset, int value) {
        data[offset] = (byte) ((value >> 8) & 0xFF);
        data[offset + 1] = (byte) (value & 0xFF);
    }

    /**
     * Write a 32-bit value to a byte array (big-endian).
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @param value  the 32-bit value to write
     */
    public static void writeUint32(byte[] data, int offset, long value) {
        data[offset] = (byte) ((value >> 24) & 0xFF);
        data[offset + 1] = (byte) ((value >> 16) & 0xFF);
        data[offset + 2] = (byte) ((value >> 8) & 0xFF);
        data[offset + 3] = (byte) (value & 0xFF);
    }

    /**
     * Convert a byte array to a hex string (no spaces).
     *
     * @param data the byte array
     * @return hex string representation
     */
    public static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(HEX_CHARS[(b >> 4) & 0x0F]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        return sb.toString();
    }

    /**
     * Convert an IP address from 4 bytes to dotted-decimal string.
     *
     * @param data   the byte array
     * @param offset the starting offset
     * @return dotted-decimal IP string (e.g., "192.168.1.1")
     */
    public static String bytesToIp(byte[] data, int offset) {
        return String.format("%d.%d.%d.%d",
                data[offset] & 0xFF, data[offset + 1] & 0xFF,
                data[offset + 2] & 0xFF, data[offset + 3] & 0xFF);
    }

    /**
     * Convert a dotted-decimal IP string to 4 bytes.
     *
     * @param ip the IP string (e.g., "192.168.1.1")
     * @return 4-byte array
     */
    public static byte[] ipToBytes(String ip) {
        String[] parts = ip.split("\\.");
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) Integer.parseInt(parts[i]);
        }
        return result;
    }
}
