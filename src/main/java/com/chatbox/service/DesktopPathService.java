package com.chatbox.service;

import java.io.File;

/**
 * Desktop implementation of {@link AppPathService}.
 * Resolves application data directories under the user's home directory (~/.chatbox).
 */
public class DesktopPathService implements AppPathService {

    private final String appDataDir;
    private final String dbPath;

    public DesktopPathService() {
        this(System.getProperty("user.home", "."));
    }

    public DesktopPathService(String userHome) {
        String base = (userHome != null && !userHome.isBlank()) ? userHome : ".";
        this.appDataDir = base + File.separator + ".chatbox";
        File dir = new File(this.appDataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.dbPath = this.appDataDir + File.separator + "chatbox.db";
    }

    @Override
    public String getAppDataDirectory() {
        return appDataDir;
    }

    @Override
    public String getDatabasePath() {
        return dbPath;
    }
}
