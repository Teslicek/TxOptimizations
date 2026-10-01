package com.teslicek.txoptimizations.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableContents.class)
public abstract class TranslatableTemplateCacheMixin {

    @Shadow
    @Final
    private static FormattedText TEXT_PERCENT;

    @Shadow
    @Final
    private static Pattern FORMAT_PATTERN;

    @Unique
    private static final int MAX_CACHED_TEMPLATES = 4096;

    @Unique
    private static final Object[] INVALID_TEMPLATE = new Object[0];

    @Unique
    private static final Map<String, Object[]> TEMPLATE_PARTS = new ConcurrentHashMap<>();

    @Shadow
    public abstract FormattedText getArgument(int index);

    @Inject(method = "decomposeTemplate", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$replayTemplate(String template, Consumer<FormattedText> parts, CallbackInfo ci) {
        Object[] templateParts = TEMPLATE_PARTS.get(template);

        if (templateParts == null) {
            templateParts = txoptimizations$parseTemplate(template);

            if (TEMPLATE_PARTS.size() >= MAX_CACHED_TEMPLATES)
                TEMPLATE_PARTS.clear();

            TEMPLATE_PARTS.put(template, templateParts);
        }

        if (templateParts == INVALID_TEMPLATE)
            return;

        for (Object part : templateParts)
            parts.accept(part instanceof Integer argumentIndex ? this.getArgument(argumentIndex) : (FormattedText) part);

        ci.cancel();
    }

    @Unique
    private static Object[] txoptimizations$parseTemplate(String template) {
        List<Object> parts         = new ArrayList<>();
        Matcher      matcher       = FORMAT_PATTERN.matcher(template);
        int          argumentIndex = 0;
        int          position      = 0;

        while (matcher.find(position)) {
            int start = matcher.start();
            int end   = matcher.end();

            if (start > position && !txoptimizations$addLiteral(parts, template.substring(position, start)))
                return INVALID_TEMPLATE;

            String type   = matcher.group(2);
            String format = template.substring(start, end);

            if ("%".equals(type) && "%%".equals(format)) {
                parts.add(TEXT_PERCENT);
            } else if ("s".equals(type)) {
                String explicitIndex = matcher.group(1);

                if (explicitIndex == null) {
                    parts.add(argumentIndex ++);
                } else {
                    try {
                        parts.add(Integer.parseInt(explicitIndex) - 1);
                    } catch (NumberFormatException exception) {
                        return INVALID_TEMPLATE;
                    }
                }
            } else {
                return INVALID_TEMPLATE;
            }

            position = end;
        }

        if (position < template.length() && !txoptimizations$addLiteral(parts, template.substring(position)))
            return INVALID_TEMPLATE;

        return parts.toArray();
    }

    @Unique
    private static boolean txoptimizations$addLiteral(List<Object> parts, String literal) {
        if (literal.indexOf('%') != -1)
            return false;

        parts.add(FormattedText.of(literal));

        return true;
    }
}
