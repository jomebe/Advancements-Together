package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Shared visual language for the vanilla hover, player legend and detail screen. */
public final class HistoryUi {
    private static final int[] COLORS = {0xFF68D5C5,0xFFFFBC6B,0xFF91B7FF,0xFFEA94CD,0xFFB4DC7D,0xFFB49BFA,0xFFFF8C8C,0xFF80D5F4,0xFFE3CD88,0xFF9BC6A9,0xFFCFA8DA,0xFFAEBBCE};
    private static final DateTimeFormatter DATES = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(ZoneId.systemDefault());
    public static int color(UUID id) {
        var players=SharedClient.ledger.snapshot().players();
        for(int i=0;i<players.size();i++) if(players.get(i).id().equals(id)) return COLORS[i%COLORS.length];
        return COLORS[0];
    }
    public static String name(UUID id) { return SharedClient.ledger.snapshot().players().stream().filter(p->p.id().equals(id)).map(Ledger.Player::name).findFirst().orElse("알 수 없음"); }
    public static String date(long time) { return DATES.format(Instant.ofEpochMilli(time)); }
    private static net.minecraft.advancements.AdvancementHolder holder(String id) {
        var client=Minecraft.getInstance(); return client.getConnection()==null?null:client.getConnection().getAdvancements().get(Platform.parse(id));
    }
    public static String title(String id) {
        var h=holder(id); return h!=null && h.value().display().isPresent()?Platform.title(h.value().display().get()).getString():SharedClient.catalog.getOrDefault(id,"이전 도전과제 · "+id);
    }
    public static void icon(GuiGraphicsExtractor g, String id, int x, int y) {
        var h=holder(id); var fallback=net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(Platform.parse(SharedClient.icons.getOrDefault(id,"minecraft:book"))).orElse(Items.BOOK).getDefaultInstance(); var item=h==null?fallback:h.value().display().map(Platform::icon).orElse(Items.BOOK.getDefaultInstance());
        g.item(item,x,y);
    }
    public static void face(GuiGraphicsExtractor g, UUID id, int x, int y, boolean selected) {
        g.fill(x-2,y-2,x+18,y+18,selected?0xFFFFFFFF:color(id));
        g.fill(x-1,y-1,x+17,y+17,0xFF151C25);
        ClientUi.face(g,Minecraft.getInstance(),id,x,y);
        g.fill(x,y+17,x+16,y+19,color(id));
    }
    public static void tooltip(GuiGraphicsExtractor g, java.util.List<String> lines, int mouseX, int mouseY, int width, int height) {
        if(lines.isEmpty()) return;
        var font=Minecraft.getInstance().font;
        var text=new ArrayList<Component>();
        int limit=Math.max(1,(height-24)/12);
        for(int i=0;i<Math.min(lines.size(),limit);i++) text.add(Component.literal(font.plainSubstrByWidth(lines.get(i),width-24)).withStyle(i==0?net.minecraft.ChatFormatting.GOLD:net.minecraft.ChatFormatting.GRAY));
        TooltipUi.draw(g,font,text,mouseX,mouseY);
    }
    public static void achievementTooltip(GuiGraphicsExtractor g, String id, int mx, int my, int width, int height) {
        var lines=new ArrayList<String>(); lines.add(title(id));
        var h=holder(id);
        {
            var font=Minecraft.getInstance().font;
            for(var line:font.split(Component.literal(SharedClient.descriptions.getOrDefault(id,"")),Math.min(270,width-40))) {
                StringBuilder text=new StringBuilder(); line.accept((index,style,c)->{text.appendCodePoint(c);return true;}); lines.add(text.toString());
            }
        }
        var records=SharedClient.ledger.snapshot().completions().stream().filter(c->c.advancement().equals(id)).toList();
        var credits=records.stream().filter(Ledger.Completion::verified).toList();
        if(records.isEmpty()) lines.add("아직 달성하지 않았어요");
        else {
            lines.add("모두에게 공유된 도전과제");
            if(credits.isEmpty()) lines.add("이전 기록 · 달성자 확인 불가");
            else for(var c:credits.subList(0,Math.min(credits.size(),Math.max(1,(height-90)/12)))) lines.add(name(c.player())+" · "+date(c.time()));
        }
        tooltip(g,lines,mx,my,width,height);
    }
}
