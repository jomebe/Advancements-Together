package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.Ledger;
public record Message(String kind, Ledger.Player player, Ledger.Completion completion, String id, String title,
                      String titleKey, String description, String descriptionKey, String icon) {
    public Message(String kind, Ledger.Player player, Ledger.Completion completion, String id, String title) {
        this(kind, player, completion, id, title, null, null, null, null);
    }
    public static Message catalog(String id, net.minecraft.network.chat.Component title, net.minecraft.network.chat.Component description, String icon) {
        return new Message("catalog",null,null,id,title.getString(),key(title),description.getString(),key(description),icon);
    }
    private static String key(net.minecraft.network.chat.Component text) {
        return text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated && translated.getArgs().length == 0 ? translated.getKey() : null;
    }
    public static Message reset() { return new Message("reset", null, null, null, null); }
}
