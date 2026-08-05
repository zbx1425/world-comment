package cn.zbx1425.worldcomment.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SizedFormattedText {

    public final float sizeModifier;
    public final FormattedCharSequence ordered;

    private SizedFormattedText(FormattedText formattedText, float sizeModifier) {
        this.sizeModifier = sizeModifier;
        this.ordered = Language.getInstance().getVisualOrder(formattedText);
    }

    private static final Pattern TITLE_PATTERN = Pattern.compile("^(#{1,6}) ");
    private static final float[] TITLE_EM = new float[] { 0, 2f, 1.6f, 1.45f, 1.3f, 1f, 1f };

    public static List<SizedFormattedText> splitLines(String text, Font font, int textWidth, Style initialStyle,
                                                      boolean allowMarkdownSyntax, boolean backgroundIsDark) {
        List<SizedFormattedText> result = new ArrayList<>();
        text.lines().forEach(line -> {
            float lineSize = 1;
            boolean lineIsHeading = false;
            if (allowMarkdownSyntax && line.startsWith("#")) {
                Matcher titleMatcher = TITLE_PATTERN.matcher(line);
                if (titleMatcher.find()) {
                    int titleLevel = titleMatcher.group(1).length();
                    line = line.substring(titleLevel + 1);
                    lineIsHeading = true;
                    lineSize = TITLE_EM[titleLevel];
                }
            }
            if (line.isEmpty()) {
                result.add(new SizedFormattedText(FormattedText.EMPTY, lineSize));
            } else {
                MutableComponent lineComponent = Component.literal(line);
                if (lineIsHeading) {
                    lineComponent.withStyle(Style.EMPTY.withColor(backgroundIsDark ? ChatFormatting.WHITE : ChatFormatting.BLACK));
                }
                for (FormattedText subLine : font.getSplitter().splitLines(lineComponent, (int) Math.floor(textWidth / lineSize), initialStyle)) {
                    result.add(new SizedFormattedText(subLine, lineSize));
                }
            }
        });
        return result;
    }
}