package com.teslicek.txoptimizations;

import java.util.List;
import java.util.Map;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import org.jspecify.annotations.Nullable;

public record PreparedLanguage(Map<String, LanguageInfo> languages, List<String> languageStack, boolean defaultRightToLeft, @Nullable ClientLanguage language, @Nullable RuntimeException failure) {
}
