package com.teslicek.txoptimizations;

public final class CountText {

    private static final String[] TEXTS = texts(1024);

    private CountText() {
    }

    public static String of(int count) {
        return count >= 0 && count < TEXTS.length ? TEXTS[count] : String.valueOf(count);
    }

    private static String[] texts(int size) {
        String[] texts = new String[size];

        for (int count = 0; count < size; count ++)
            texts[count] = String.valueOf(count);

        return texts;
    }
}
