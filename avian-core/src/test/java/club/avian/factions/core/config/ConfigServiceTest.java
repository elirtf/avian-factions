package club.avian.factions.core.config;

import club.avian.factions.api.config.ConfigSpec;
import club.avian.factions.api.module.AvianModule;
import club.avian.factions.api.module.ModuleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigServiceTest {

    @ConfigSerializable
    public static final class Sample {
        @Comment("How many.")
        int count = 3;
        @Comment("Where.")
        Nested nested = new Nested();
        @Required
        String secret;
    }

    @ConfigSerializable
    public static final class Nested {
        @Comment("A name.")
        String name = "default";
        double ratio = 0.5;
    }

    static final ConfigSpec<Sample> SPEC = ConfigSpec.of("sample.conf", Sample.class)
            .version(2)
            .envOverride("SAMPLE_SECRET", "secret")
            .validate((cfg, e) -> e.check(cfg.count > 0, "count", "must be > 0 (got %d)", cfg.count))
            .build();

    @TempDir Path dir;
    final Map<String, String> env = new HashMap<>();
    final List<String> logged = new ArrayList<>();
    ConfigService service;

    @BeforeEach
    void setUp() {
        var log = Logger.getLogger("ConfigServiceTest");
        log.setUseParentHandlers(false);
        log.getHandlers();
        log.addHandler(new Handler() {
            @Override public void publish(LogRecord r) { logged.add(r.getLevel() + " " + r.getMessage()); }
            @Override public void flush() { }
            @Override public void close() { }
        });
        service = new ConfigService(dir, log, env::get);
    }

    private AvianModule module() {
        return new AvianModule() {
            @Override public String id() { return "sample"; }
            @Override public List<ConfigSpec<?>> configs() { return List.of(SPEC); }
            @Override public void enable(ModuleContext ctx) { }
        };
    }

    private void write(String hocon) throws IOException {
        Files.writeString(dir.resolve("sample.conf"), hocon);
    }

    @Test
    void freshInstallWritesCommentedDefaultsAndRequiredFieldIsAnError() throws IOException {
        var e = assertThrows(ConfigLoadException.class, () -> service.loadAll(List.of(module())));
        assertEquals(1, e.errors().size());
        assertEquals("sample.conf", e.errors().getFirst().file());
        assertEquals("secret", e.errors().getFirst().path());
        var text = Files.readString(dir.resolve("sample.conf"));
        assertTrue(text.contains("# How many."), "fresh install still writes a commented file to edit: " + text);
        assertEquals("A value is required for this field", e.errors().getFirst().message());
    }

    @Test
    void freshInstallWithSecretFromEnvWritesDefaultsWithComments() throws IOException {
        env.put("SAMPLE_SECRET", "hunter2");
        service.loadAll(List.of(module()));
        var text = Files.readString(dir.resolve("sample.conf"));
        assertTrue(text.contains("# How many."), text);
        assertTrue(text.contains("count=3"), text);
        assertTrue(text.contains("config-version=2"), text);
        assertFalse(text.contains("hunter2"), "env-provided secrets must not be written to disk");
        assertEquals("hunter2", service.handle(SPEC).get().secret);
    }

    @Test
    void allErrorsAcrossValidationAndMappingAreAggregated() throws IOException {
        write("count = -1\nnested { name = x }\n");
        var e = assertThrows(ConfigLoadException.class, () -> service.loadAll(List.of(module())));
        var paths = e.errors().stream().map(ConfigError::path).toList();
        // Mapping (@Required secret) is reported; validation runs only on a mapped value, so a
        // second load with the secret present reports the invariant.
        assertEquals(List.of("secret"), paths);
        env.put("SAMPLE_SECRET", "s");
        var e2 = assertThrows(ConfigLoadException.class, () -> service.loadAll(List.of(module())));
        assertEquals("sample.conf → count: must be > 0 (got -1)", e2.errors().getFirst().toString());
    }

    @Test
    void missingKeysAreBackfilledWithWarningsAndSaved() throws IOException {
        env.put("SAMPLE_SECRET", "s");
        write("config-version = 2\ncount = 7\nnested { name = \"kept\" }\nbogus = 1\n");
        service.loadAll(List.of(module()));
        var cfg = service.handle(SPEC).get();
        assertEquals(7, cfg.count);
        assertEquals("kept", cfg.nested.name);
        assertEquals(0.5, cfg.nested.ratio);
        assertTrue(logged.stream().anyMatch(l -> l.contains("added missing key nested.ratio = 0.5")), logged.toString());
        assertTrue(logged.stream().anyMatch(l -> l.contains("unknown key bogus")), logged.toString());
        var text = Files.readString(dir.resolve("sample.conf"));
        assertTrue(text.contains("ratio=0.5"), text);
        assertTrue(text.contains("kept"), text);
    }

    @Test
    void newerFileVersionIsRejected() throws IOException {
        env.put("SAMPLE_SECRET", "s");
        write("config-version = 9\n");
        var e = assertThrows(ConfigLoadException.class, () -> service.loadAll(List.of(module())));
        assertEquals("config-version", e.errors().getFirst().path());
    }

    @Test
    void unparseableFileNamesTheFile() throws IOException {
        write("count = {{{\n");
        var e = assertThrows(ConfigLoadException.class, () -> service.loadAll(List.of(module())));
        assertEquals("sample.conf", e.errors().getFirst().file());
        assertTrue(e.errors().getFirst().message().startsWith("cannot be parsed"), e.errors().toString());
    }
}
