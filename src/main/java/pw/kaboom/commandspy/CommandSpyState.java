package pw.kaboom.commandspy;

import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public final class CommandSpyState {
    private static final Logger LOGGER = JavaPlugin.getPlugin(Main.class).getSLF4JLogger();

    private final ObjectOpenHashSet<UUID> users = new ObjectOpenHashSet<>();
    private final StampedLock usersLock = new StampedLock();
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final File file;

    public CommandSpyState(final @NotNull File file) {
        this.file = file;

        try {
            this.load();
        } catch (final FileNotFoundException exception) {
            try {
                this.save(); // Create file if it doesn't exist
            } catch (IOException ignored) {
            }
        } catch (final IOException exception) {
            LOGGER.error("Failed to load state file:", exception);
        }
    }

    private void load() throws IOException {
        final InputStream reader = new BufferedInputStream(new FileInputStream(this.file));

        int read;
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);

        // Loop until we read less than 16 bytes
        while ((read = reader.readNBytes(buffer.array(), 0, 16)) == 16) {
            this.users.add(new UUID(buffer.getLong(0), buffer.getLong(8)));
        }

        reader.close();
        if (read != 0) {
            throw new IOException("Found " + read + " bytes extra whilst reading file");
        }
    }

    private void save() throws IOException {
        Files.createParentDirs(this.file);
        final OutputStream writer = new BufferedOutputStream(new FileOutputStream(this.file));
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);

        final long stamp = this.usersLock.readLock();
        for (final UUID uuid : this.users) {
            buffer.putLong(0, uuid.getMostSignificantBits());
            buffer.putLong(8, uuid.getLeastSignificantBits());
            writer.write(buffer.array());
        }
        this.usersLock.unlockRead(stamp);

        writer.flush();
        writer.close();
    }

    public void trySave() {
        // If the state is not dirty, then we don't need to do anything.
        if (!this.dirty.compareAndExchange(true, false)) {
            return;
        }

        try {
            this.save();
        } catch (final IOException exception) {
            LOGGER.error("Failed to save state file:", exception);
        }
    }

    public boolean getCommandSpyState(final @NotNull UUID playerUUID) {
        final long stamp = this.usersLock.readLock();
        final boolean result = this.users.contains(playerUUID);
        this.usersLock.unlockRead(stamp);

        return result;
    }

    public void setCommandSpyState(final @NotNull UUID playerUUID, final boolean state) {
        final long stamp = this.usersLock.writeLock();

        final boolean dirty;
        if (state) {
            dirty = this.users.add(playerUUID);
        } else {
            dirty = this.users.remove(playerUUID);
        }

        this.usersLock.unlockWrite(stamp);
        if (dirty) {
            this.dirty.set(true);
        }
    }

    public void broadcastSpyMessage(final @NotNull Component message) {
        // Raw access here, so we can get more performance by not locking/unlocking over and over
        final long stamp = this.usersLock.readLock();
        final Collection<Player> players = Bukkit.getOnlinePlayers()
                .stream()
                .filter(p -> this.users.contains(p.getUniqueId()))
                .collect(Collectors.toUnmodifiableSet());
        this.usersLock.unlockRead(stamp);

        for (final Player recipient : players) {
            recipient.sendMessage(message);
        }
    }
}
