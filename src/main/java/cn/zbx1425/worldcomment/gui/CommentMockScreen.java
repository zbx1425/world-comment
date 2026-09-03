package cn.zbx1425.worldcomment.gui;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.client.CommentPrefillInfo;
import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.util.OffHeapAllocator;
import com.mojang.blaze3d.platform.NativeImage;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class CommentMockScreen {

    public static Screen create(Screen parent) {
        LocalPlayer player = Minecraft.getInstance().player;
        CommentPrefillInfo prefill = new CommentPrefillInfo(player, player.blockPosition(), null);

        final String[] initiatorStr = { prefill.initiator != null ? prefill.initiator.toString() : "" };
        Option<String> optInitiator = Option.<String>createBuilder()
            .name(Component.literal("Initiator"))
            .description(OptionDescription.of(Component.literal("UUID of the player who sent this comment.")))
            .binding(initiatorStr[0], () -> initiatorStr[0], v -> initiatorStr[0] = v)
            .controller(StringControllerBuilder::create)
            .available(false)
            .build();

        Option<String> optInitiatorName = Option.<String>createBuilder()
            .name(Component.literal("Initiator Name"))
            .description(OptionDescription.of(Component.literal("Name of the player who sent this comment.")))
            .binding(prefill.initiatorName, () -> prefill.initiatorName, v -> prefill.initiatorName = v)
            .controller(StringControllerBuilder::create)
            .build();

        final String[] imageLocationStr = { String.format("(%d, %d, %d)", prefill.imageLocation.getX(), prefill.imageLocation.getY(), prefill.imageLocation.getZ()) };
        Option<String> optImagePosition = Option.<String>createBuilder()
            .name(Component.literal("Image Location"))
            .description(OptionDescription.of(Component.literal(
                "The Block Position of the player who took this screenshot, at the time when it was taken.")))
            .binding(imageLocationStr[0], () -> imageLocationStr[0], v -> imageLocationStr[0] = v)
            .controller(StringControllerBuilder::create)
            .build();

        final String[] timestampStr = { "" };
        Option<String> optTimestamp = Option.<String>createBuilder()
            .name(Component.literal("Timestamp"))
            .description(OptionDescription.of(Component.literal(
                "The send time of this comment. Format: yyyy-MM-dd HH:mm:ss or yyyy-MM-ddTHH:mm:ss. Leave empty to use current time.")))
            .binding("", () -> timestampStr[0], v -> timestampStr[0] = v)
            .controller(StringControllerBuilder::create)
            .build();

        Option<Boolean> optUnlisted = Option.<Boolean>createBuilder()
            .name(Component.literal("Unlisted"))
            .description(OptionDescription.of(Component.literal(
                "If true, this comment will not appear in Nearby/Recent lists.")))
            .binding(false, () -> prefill.unlisted, v -> prefill.unlisted = v)
            .controller(option -> BooleanControllerBuilder.create(option).yesNoFormatter())
            .build();

        OptionGroup.Builder groupImage = OptionGroup.createBuilder()
            .name(Component.literal("Image"))
            .description(OptionDescription.of(Component.literal("The image file to use for mocking.")));

        final String[] imagePath = { "" };
        Map<String, ButtonOption> imageButtons = new HashMap<>();
        Option<String> optImage = Option.<String>createBuilder()
            .name(Component.literal("Image"))
            .binding("", () -> imagePath[0], v -> imagePath[0] = v)
            .controller(StringControllerBuilder::create)
            .addListener((option, value) -> {
                for (Map.Entry<String, ButtonOption> entry1 : imageButtons.entrySet()) {
                    entry1.getValue().setAvailable(!entry1.getKey().equals(option.pendingValue()));
                }
            })
            .build();

        groupImage.option(optImage);
        groupImage.option(ButtonOption.createBuilder()
            .name(Component.literal("[Open Folder]"))
            .action((screen, btn) -> {
                Util.getPlatform().openPath(getInputImageDir());
            })
            .build());
        groupImage.option(ButtonOption.createBuilder()
            .name(Component.literal("[Refresh]"))
            .action((screen, btn) -> {
                Minecraft.getInstance().setScreen(CommentMockScreen.create(parent));
            })
            .build());

        try {
            Files.createDirectories(getInputImageDir());
            try (Stream<Path> stream = Files.list(getInputImageDir())) {
                stream.forEach(path -> {
                    if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) return;
                    ButtonOption imageBtn = ButtonOption.createBuilder()
                        .name(Component.literal(path.getFileName().toString()))
                        .text(Component.empty())
                        .available(!path.getFileName().toString().equals(optImage.pendingValue()))
                        .action((screen, btn) -> {
                            optImage.requestSet(path.getFileName().toString());
                        })
                        .build();
                    groupImage.option(imageBtn);
                    imageButtons.put(path.getFileName().toString(), imageBtn);
                });
            }
        } catch (IOException ex) {
            groupImage.option(LabelOption.create(Component.literal(ex.toString())));
        }

        Screen[] mockScreen = new Screen[] { null };
        mockScreen[0] = YetAnotherConfigLib.createBuilder()
            .title(Component.literal("Mock a Comment"))
            .category(ConfigCategory.createBuilder()
                .name(Component.literal("Mock a Comment"))
                .group(OptionGroup.createBuilder()
                    .name(Component.literal("Metadata"))
                    .option(optInitiator)
                    .option(optInitiatorName)
                    .option(optTimestamp)
                    .option(optImagePosition)
                    .option(optUnlisted)
                    .build()
                )
                .group(groupImage.build())
                .build()
            )
            .save(() -> {
                try {
                    if (!player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(2)))) return;

                    if (!initiatorStr[0].isEmpty()) {
                        prefill.initiator = UUID.fromString(initiatorStr[0]);
                    }

                    BlockPos parsedPos = parseBlockPos(imageLocationStr[0]);
                    if (parsedPos == null) {
                        throw new IllegalArgumentException("Invalid BlockPos format: " + imageLocationStr[0]);
                    }
                    prefill.imageLocation = parsedPos;

                    if (!timestampStr[0].isEmpty()) {
                        prefill.overrideTimestamp = parseTimestamp(timestampStr[0]);
                    }

                    if (imagePath[0].isEmpty() || imagePath[0].contains("..") || !imagePath[0].toLowerCase(Locale.ROOT).endsWith(".png")) {
                        throw new IllegalArgumentException("Invalid image file path: " + imagePath[0]);
                    }
                    Path imageFilePath = getInputImageDir().resolve(imagePath[0]);
                    if (!Files.isRegularFile(imageFilePath)) {
                        throw new IOException("Image file does not exist: " + imagePath[0]);
                    }

                    prefill.imagePngBytes = Files.readAllBytes(imageFilePath);
                    ByteBuffer offHeapBuffer = OffHeapAllocator.allocate(prefill.imagePngBytes.length);
                    try {
                        offHeapBuffer.put(prefill.imagePngBytes);
                        offHeapBuffer.rewind();
                        try (NativeImage ignored = NativeImage.read(offHeapBuffer)) {
                            // We test if this image can be parsed
                        }
                    } finally {
                        OffHeapAllocator.free(offHeapBuffer);
                    }

                    Minecraft.getInstance().setScreen(new CommentSendScreen(prefill, true));
                } catch (Exception ex) {
                    Minecraft.getInstance().setScreen(YetAnotherConfigLib.createBuilder()
                        .title(Component.literal("Error"))
                        .category(ConfigCategory.createBuilder()
                            .name(Component.literal("Error"))
                            .option(LabelOption.create(Component.literal(ex.toString())))
                            .build())
                        .build()
                        .generateScreen(mockScreen[0])
                    );
                }
            })
            .build()
            .generateScreen(parent);
        return mockScreen[0];
    }

    private static @Nullable BlockPos parseBlockPos(String input) {
        input = StringUtils.stripStart(input, " ([");
        input = StringUtils.stripEnd(input, " )]");
        String[] split = input.split(",");
        if (split.length != 3) return null;
        try {
            return new BlockPos(
                Integer.parseInt(split[0].trim()),
                Integer.parseInt(split[1].trim()),
                Integer.parseInt(split[2].trim())
            );
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static long parseTimestamp(String input) {
        String normalized = input.trim().replace('T', ' ');
        LocalDateTime ldt = LocalDateTime.parse(normalized, TIMESTAMP_FORMATTER);
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static Path getInputImageDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("worldcomment").resolve("input_image");
    }
}