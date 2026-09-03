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
import dev.isxander.yacl3.gui.controllers.string.IStringController;
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

        Option<UUID> optInitiator = Option.<UUID>createBuilder()
            .name(Component.literal("Initiator"))
            .description(OptionDescription.of(Component.literal("UUID of the player who sent this comment.")))
            .binding(prefill.initiator, () -> prefill.initiator,v -> prefill.initiator = v)
            .customController(UUIDController::new)
            .available(false)
            .build();
        Option<String> optInitiatorName = Option.<String>createBuilder()
            .name(Component.literal("Initiator Name"))
            .description(OptionDescription.of(Component.literal("Name of the player who sent this comment.")))
            .binding(prefill.initiatorName, () -> prefill.initiatorName, v -> prefill.initiatorName = v)
            .controller(StringControllerBuilder::create)
            .build();
        Option<BlockPos> optImagePosition = Option.<BlockPos>createBuilder()
            .name(Component.literal("Image Location"))
            .description(OptionDescription.of(Component.literal(
                "The Block Position of the player who took this screenshot, at the time when it was taken.")))
            .binding(prefill.imageLocation, () -> prefill.imageLocation, v -> prefill.imageLocation = v)
            .customController(BlockPosController::new)
            .build();
        final String[] timestampStr = { "" };
        Option<String> optTimestamp = Option.<String>createBuilder()
            .name(Component.literal("Timestamp"))
            .description(OptionDescription.of(Component.literal(
                "The send time of this comment. Format: yyyy-MM-dd HH:mm:ss or yyyy-MM-ddTHH:mm:ss. Leave empty to use current time.")))
            .binding("", () -> timestampStr[0], v -> timestampStr[0] = v)
            .customController(TimestampController::new)
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
            .customController(InputImageController::new)
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

        return YetAnotherConfigLib.createBuilder()
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
                    if (!timestampStr[0].isEmpty()) {
                        prefill.overrideTimestamp = parseTimestamp(timestampStr[0]);
                    }
                    prefill.imagePngBytes = Files.readAllBytes(getInputImageDir().resolve(imagePath[0]));
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
                } catch (IOException ex) {
                    Minecraft.getInstance().setScreen(YetAnotherConfigLib.createBuilder()
                        .category(ConfigCategory.createBuilder()
                            .name(Component.literal("Error"))
                            .option(LabelOption.create(Component.literal(ex.toString())))
                            .build())
                        .build()
                        .generateScreen(parent)
                    );
                }
            })
            .build()
            .generateScreen(parent);
    }

    private record UUIDController(Option<UUID> option) implements IStringController<UUID> {
        @Override
        public String getString() {
            return option.pendingValue().toString();
        }

        @Override
        public void setFromString(String value) {
            try {
                option.requestSet(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private record BlockPosController(Option<BlockPos> option) implements IStringController<BlockPos> {
        @Override
        public String getString() {
            BlockPos blockPos = option.pendingValue();
            return String.format("(%d, %d, %d)", blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }

        @Override
        public void setFromString(String value) {
            BlockPos parsed = parseBlockPos(value);
            if (parsed != null) option.requestSet(parsed);
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
    }

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static long parseTimestamp(String input) {
        String normalized = input.trim().replace('T', ' ');
        LocalDateTime ldt = LocalDateTime.parse(normalized, TIMESTAMP_FORMATTER);
        return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private record TimestampController(Option<String> option) implements IStringController<String> {
        @Override
        public String getString() {
            return option.pendingValue();
        }

        @Override
        public void setFromString(String value) {
            if (value.isEmpty()) {
                option.requestSet("");
                return;
            }
            try {
                parseTimestamp(value);
                option.requestSet(value);
            } catch (DateTimeParseException ignored) { }
        }
    }

    private record InputImageController(Option<String> option) implements IStringController<String> {
        @Override
        public String getString() {
            return option.pendingValue();
        }

        @Override
        public void setFromString(String value) {
            if (!value.contains("..")
                && value.toLowerCase(Locale.ROOT).endsWith(".png")
                && Files.isRegularFile(getInputImageDir().resolve(value))) {
                option.requestSet(value);
            }
        }
    }

    private static Path getInputImageDir() {
        return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("worldcomment").resolve("input_image");
    }
}
