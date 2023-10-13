package nxt;

import nxt.util.Logger;

public class AppVersion {

    private final int[] versionArray;
    private final String versionStr;
    private final boolean valid;

    public AppVersion(String versionStr) {
        this.versionStr = versionStr;
        versionArray = parse(versionStr);
        valid = versionArray != null;
    }

    public AppVersion(int[] versionArray) {
        this.versionArray = versionArray;
        valid = versionArray.length == 3;
        versionStr = versionArray[0] + "." + versionArray[1] + "." + versionArray[2];
    }

    public AppVersion(int major, int minor, int patch) {
        this(new int[] { major, minor, patch} );
    }

    @Override
    public String toString() {
        return versionStr;
    }

    public boolean getIsValid() {
        return valid;
    }

    /**
     * Parse version string of format [MAJOR].[MINOR].[PATCH] where each
     * part is an integer.
     * Optionally versions can include pre-release information which must be
     * appended to the version following a hyphen (example 0.6.1-alpha.1).
     *
     * Everything beyond the hyphen is ignored.
     * Failure to parse the version string will result in a null return.
     *
     * @param versionStr String
     * @return int[] or null
     */
    int[] parse(String versionStr) {
        String[] parts = versionStr.split("\\.");
        if (parts.length == 3) {
            parts[2] = parts[2].split("-")[0]; // removes pre-release information (if any)
            int[] version = new int[3];
            for (int i=0; i<parts.length; i++) {
                try {
                    version[i] = Integer.parseInt(parts[i]);
                }
                catch(NumberFormatException e) {
                    Logger.logMessage("Invalid version string: " + versionStr);
                    return null;
                }
            }
            return version;
        }
        Logger.logMessage("Invalid version string: " + versionStr);
        return null;
    }

    /**
     * Returns the value 0 if this Version is equal to the argument Version;
     * a value less than 0 if this Version is numerically less than the
     * argument Version; and a value greater than 0 if this Version is
     * numerically greater than the argument Version.
     *
     * @param other Version
     * @return int
     */
    public int compareTo(AppVersion other) {
        for (int i=0; i<versionArray.length; i++) {
            if (versionArray[i] > other.versionArray[i]) {
                return 1;
            }
            if (versionArray[i] < other.versionArray[i]) {
                return -1;
            }
        }
        return 0;
    }
}
