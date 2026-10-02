package com.teslicek.txoptimizations.gpu;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

final class ReportFiles {

    private static final String            DIRECTORY = "txoptimizations";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");

    private ReportFiles() {
    }

    static Path write(String prefix, String report) {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve(DIRECTORY);
        Path file      = directory.resolve(prefix + "-" + LocalDateTime.now().format(FILE_TIME) + ".txt");

        try {
            Files.createDirectories(directory);
            Files.writeString(file, report);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write " + file, exception);
        }

        return file;
    }

    static MutableComponent savedMessage(String title, Path file) {
        Path absolute = file.toAbsolutePath();

        return Component.literal(title + " saved to " + DIRECTORY + "/" + file.getFileName() + " ")
            .append(link("[Open folder]", new ClickEvent.OpenFile(absolute.getParent()), "Open " + absolute.getParent() + " in the file explorer"))
            .append(" ")
            .append(link("[Copy path]", new ClickEvent.CopyToClipboard(absolute.toString()), "Copy " + absolute));
    }

    private static MutableComponent link(String text, ClickEvent click, String hover) {
        return Component.literal(text).withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withUnderlined(true)
            .withClickEvent(click)
            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }
}
