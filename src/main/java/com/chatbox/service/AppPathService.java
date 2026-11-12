package com.chatbox.service;

/**
 * Service interface for platform-specific filesystem paths and application storage.
 * Isolates desktop-specific paths (e.g. user.home/.chatbox) from business logic
 * and enables seamless future Android platform integration (e.g. context.getFilesDir()).
 */
public interface AppPathService {

    /**
     * Returns the platform-specific directory where the application stores its data.
     * On desktop: ~/.chatbox
     * On Android: /data/user/0/<package>/files or context.getFilesDir()
     */
    String getAppDataDirectory();

    /**
     * Returns the full file path to the SQLite database file.
     * On desktop: ~/.chatbox/chatbox.db
     * On Android: context.getDatabasePath("chatbox.db").getAbsolutePath()
     */
    String getDatabasePath();

    /**
     * Global singleton accessor.
     */
    static AppPathService getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Allows setting a custom implementation (e.g., for Android or custom test fixtures).
     */
    static void setInstance(AppPathService service) {
        Holder.INSTANCE = service != null ? service : new DesktopPathService();
    }

    class Holder {
        private static volatile AppPathService INSTANCE = new DesktopPathService();
    }
}
