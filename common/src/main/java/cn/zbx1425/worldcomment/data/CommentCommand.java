package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.network.CommentImage;
import cn.zbx1425.worldcomment.data.network.ImageDump;
import cn.zbx1425.worldcomment.data.network.upload.CommentAffinityInfo;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CommentCommand {

    public static boolean isCommand(CommentEntry comment) {
        return comment.message.startsWith("$SNCMD:");
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void executeCommandServer(CommentEntry comment, ServerWorldData worldData) {
        if (!isCommand(comment)) return;
        String commandContent = comment.message.substring(7);
        String command = commandContent.split(" ")[0].toLowerCase(Locale.ROOT);
        String[] args = commandContent.substring(command.length()).trim().split(" ");
        switch (command) {
            case "uplinksendall" -> {
                for (CommentEntry commentEntry : worldData.comments.timeIndex.values()) {
                    worldData.uplinkDispatcher.insert(commentEntry);
                }
            }
            case "migratehosting" -> {
                if (args.length < 2) return;
                String domainPredicate = args[1];
                boolean predicateIsPositive;
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "domainis" -> predicateIsPositive = true;
                    case "domainisnot" -> predicateIsPositive = false;
                    default -> { return; }
                };
                ImageUploader uploader = Main.SERVER_CONFIG.imageUploaders.value.getFirst();
                for (CommentEntry commentEntry : worldData.comments.timeIndex.values()) {
                    if (commentEntry.image.sourceUrl.isEmpty()) continue;
                    URI imageUrl;
                    try {
                        imageUrl = new URI(commentEntry.image.sourceUrl);
                        String domain = imageUrl.getHost();
                        if (domain.endsWith(domainPredicate) != predicateIsPositive) continue;
                    } catch (Exception ex) {
                        Main.LOGGER.error("Migrating hosting of {}", commentEntry.image.sourceUrl, ex);
                        continue;
                    }

                    HttpRequest request = HttpRequest.newBuilder(imageUrl)
                            .timeout(Duration.of(10, ChronoUnit.SECONDS))
                            .GET()
                            .build();
                    CommentAffinityInfo info = new CommentAffinityInfo(commentEntry);
                    HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                            .thenCompose(response -> {
                                if (response.statusCode() != 200) {
                                    throw new CompletionException(new IOException("HTTP Error Code " + response.statusCode()));
                                }
                                return uploader.uploadImage(response.body(), "migrated.webp", info);
                            })
                            .thenAccept(uploadResult -> {
                                commentEntry.image = new CommentImage(uploader.id, uploadResult.url(), "", "");
                                try {
                                    Main.DATABASE.updateAllFields(commentEntry, false);
                                } catch (Exception ex) {
                                    throw new CompletionException(ex);
                                }
                            })
                            .exceptionally(ex -> {
                                Main.LOGGER.error("Migrating hosting of {}", commentEntry.image.sourceUrl, ex);
                                return null;
                            });
                }
            }
        }
    }

    public static void executeCommandClient(CommentEntry comment) {
        if (!isCommand(comment)) return;
        String commandContent = comment.message.substring(7);
        String command = commandContent.split(" ")[0].toLowerCase(Locale.ROOT);
        String[] args = commandContent.substring(command.length()).trim().split(" ");
        switch (command) {
            case "imagedumpall" -> {
                if (args.length < 1) return;
                ImageDump.requestDumpComments(args[0]);
            }
        }
    }
}
