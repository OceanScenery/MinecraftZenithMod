package com.oceanscenery.zenith.bootstrap;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileLoadingException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ZenithBootstrap implements ITransformationService, IModLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger("TheZenithSwordBootstrap");
    private static final String PAYLOAD_RESOURCE = "/META-INF/the_zenith_sword/payload.jar";
    private static final String CACHE_DIRECTORY = "the_zenith_sword/bootstrap-cache";
    private ModFile payloadModFile;

    public ZenithBootstrap() {
        LOGGER.info("[ZenithBootstrap] constructed");
    }

    @Override
    public String name() {
        return "the_zenith_sword_bootstrap";
    }

    @Override
    public void initialize(IEnvironment environment) {
        LOGGER.info("[ZenithBootstrap] initialize");
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
        LOGGER.info("[ZenithBootstrap] onLoad, otherServices={}", otherServices);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        Set<String> targets;
        try {
            targets = LivingEntityTargetIndex.findTargets();
        } catch (Throwable throwable) {
            LOGGER.error("[ZenithBootstrap] failed to scan LivingEntity targets; using base class only", throwable);
            targets = LivingEntityTargetIndex.baseTargets();
        }

        if (targets.isEmpty()) {
            targets = LivingEntityTargetIndex.baseTargets();
        }

        LOGGER.info("[ZenithBootstrap] creating transformer for {} target classes", targets.size());
        return List.of(
            new ZenithTransformer(targets)
        );
    }

    @Override
    public List<ModFileOrException> scanMods() {
        try {
            Path payloadPath = extractPayload();
            if (payloadModFile == null) {
                payloadModFile = createPayloadModFile(payloadPath);
            }

            LOGGER.info("[ZenithBootstrap] registered payload mod: {}", payloadPath);
            return List.of(new ModFileOrException(payloadModFile, null));
        } catch (Throwable throwable) {
            LOGGER.error("[ZenithBootstrap] failed to extract or register payload", throwable);
            return List.of(new ModFileOrException(
                null,
                new ModFileLoadingException(
                    "Failed to register embedded payload: "
                        + throwable.getClass().getName() + ": "
                        + throwable.getMessage()
                )
            ));
        }
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> consumer) {
        Path root;
        try {
            root = modFile.getSecureJar().getRootPath();
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to access the payload jar root", throwable);
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .forEach(consumer);
            LOGGER.info("[ZenithBootstrap] scanFile completed for {}", modFile.getFileName());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to scan payload jar", exception);
        }
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        LOGGER.info("[ZenithBootstrap] initArguments");
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    private static Path extractPayload() throws IOException, NoSuchAlgorithmException {
        try (InputStream input = ZenithBootstrap.class.getResourceAsStream(PAYLOAD_RESOURCE)) {
            if (input == null) {
                throw new IOException("Missing embedded payload resource: " + PAYLOAD_RESOURCE);
            }

            byte[] payload = input.readAllBytes();
            String hash = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(payload)
            );

            Path cacheDirectory = FMLPaths.CONFIGDIR.get().resolve(CACHE_DIRECTORY);
            Files.createDirectories(cacheDirectory);

            Path target = cacheDirectory.resolve(
                "the_zenith_sword-payload-" + hash + ".jar"
            );
            if (Files.isRegularFile(target)) {
                return target;
            }

            Path temporary = Files.createTempFile(cacheDirectory, "payload-", ".tmp");
            Files.write(
                temporary,
                payload,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );

            try {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
                );
            } catch (FileAlreadyExistsException exception) {
                Files.deleteIfExists(temporary);
            }

            return target;
        }
    }

    private ModFile createPayloadModFile(Path payloadPath) {
        ModFile metadataFile = new ModFile(
            SecureJar.from(payloadPath),
            this,
            ModFileParser::modsTomlParser
        );

        String moduleName = metadataFile.getModFileInfo().moduleName();
        LOGGER.info("[ZenithBootstrap] payload module name: {}", moduleName);

        return new ModFile(
            createPayloadSecureJar(payloadPath, moduleName),
            this,
            ModFileParser::modsTomlParser
        );
    }

    private static SecureJar createPayloadSecureJar(
        Path payloadPath,
        String moduleName
    ) {
        return SecureJar.from(
            jar -> {
                SimpleJarMetadata original = JarMetadata.fromFileName(
                    payloadPath,
                    jar.getPackages(),
                    jar.getProviders()
                );
                return new SimpleJarMetadata(
                    moduleName,
                    original.version(),
                    original.pkgs(),
                    original.providers()
                );
            },
            payloadPath
        );
    }
}
