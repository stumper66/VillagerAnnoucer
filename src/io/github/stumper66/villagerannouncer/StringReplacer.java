package io.github.stumper66.villagerannouncer;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class StringReplacer {
    public StringReplacer(final String text){
        this.text = text;
    }

    public String text;

    public void replaceIfExists(final String target, final @NotNull Supplier<String> operation){
        final String newText = operation.get();

        if (text.contains(target)) {
            replace(target, newText);
        }
    }

    public void replaceIfExistsInt(final String target, final @NotNull Supplier<Integer> operation){
        final Integer value = operation.get();

        replace(target, value != null ? String.valueOf(value) : "");
    }

    public void replace(final String replace, final String replaceWith) {
        if (replaceWith == null) return;

        text = text.replace(replace, replaceWith);
    }

    public boolean isEmpty(){
        return text.isEmpty();
    }

    public boolean contains(final CharSequence s) {
        return text.contains(s);
    }

    @Override
    public String toString() {
        return this.text;
    }
}
