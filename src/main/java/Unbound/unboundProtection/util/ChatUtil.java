package Unbound.unboundProtection.util;

import Unbound.unboundProtection.UnboundProtection;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ChatUtil {
    private final UnboundProtection plugin;
    private final YamlConfiguration defaults;

    public ChatUtil(UnboundProtection plugin) {
        this.plugin = plugin;
        this.defaults = loadDefaults();
    }

    public void send(CommandSender recipient, String key, String... replacements) {
        if (!plugin.getConfig().getBoolean("messages.enabled", true)) return;

        String path = "messages.items." + key;
        if (!plugin.getConfig().getBoolean(path + ".enabled", defaults.getBoolean(path + ".enabled", true))) return;

        String message = plugin.getConfig().getString(path + ".text", defaults.getString(path + ".text"));
        if (message == null) return;
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("%" + (i + 1) + "%", replacements[i] == null ? "" : replacements[i]);
        }

        boolean usePrefix = plugin.getConfig().getBoolean(path + ".prefix", defaults.getBoolean(path + ".prefix", true));
        if (usePrefix && plugin.getConfig().getBoolean("messages.prefix-enabled", true)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&aUnbound&bProtection&8] &r");
            message = prefix + message;
        }
        recipient.sendMessage(color(message));
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private YamlConfiguration loadDefaults() {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return new YamlConfiguration();
        }
    }
}
