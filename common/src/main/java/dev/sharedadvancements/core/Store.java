package dev.sharedadvancements.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public final class Store {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path file;
    public Store(Path file) { this.file = file; }
    public Ledger load() throws IOException {
        if (!Files.exists(file)) return new Ledger();
        try {
            var snapshot = JSON.fromJson(Files.readString(file), Ledger.Snapshot.class);
            Ledger result = Ledger.restore(snapshot);
            Path legacy = file.resolveSibling("history.schema-1.json");
            if (snapshot.schema() == 1 && !Files.exists(legacy)) Files.copy(file, legacy);
            return result;
        }
        catch (RuntimeException e) { throw new IOException("Invalid ledger; original preserved: " + file, e); }
    }
    public void save(Ledger ledger) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, JSON.toJson(ledger.snapshot()), StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) { channel.force(true); }
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
    }
}
