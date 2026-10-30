package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class SharedClient implements ClientModInitializer {
    // Weak keys identify our widgets without retaining closed screens through button callbacks.
    private static final Set<Button> ownedButtons=Collections.newSetFromMap(new WeakHashMap<>());
    public static Ledger ledger=new Ledger();
    public static final Map<String,Component> catalog=new LinkedHashMap<>();
    public static final Map<String,String> icons=new HashMap<>();
    public static final Map<String,Component> descriptions=new HashMap<>();
    public static boolean connected;
    public static String hovered;
    public static void clear(){ledger=new Ledger();catalog.clear();icons.clear();descriptions.clear();connected=false;hovered=null;}
    @Override public void onInitializeClient(){
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear());
        ClientPlayNetworking.registerGlobalReceiver(HistoryPayload.TYPE,(payload,context)->context.client().execute(()->{
            Message m=Store.JSON.fromJson(payload.json(),Message.class);
            switch(m.kind()){
                case "reset"->{clear();connected=true;}
                case "player"->ledger.join(m.player().id(),m.player().name());
                case "completion"->ledger.award(m.completion().player(),m.completion().advancement(),m.completion().time(),m.completion().verified());
                case "catalog"->{catalog.put(m.id(),m.titleKey()==null?Component.literal(m.title()):Component.translatable(m.titleKey())); icons.put(m.id(),m.icon()); descriptions.put(m.id(),m.descriptionKey()==null?Component.literal(m.description()):Component.translatable(m.descriptionKey()));}
                case "catalog-reset"->{catalog.clear();icons.clear();descriptions.clear();}
                default->throw new IllegalArgumentException("Unsupported Advancements Together protocol");
            }
        }));
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            if(!(screen instanceof AdvancementsScreen)||!connected)return;
            Screens.getButtons(screen).removeIf(ownedButtons::contains);
            var details=Button.builder(Component.translatable("shared_advancements.details"),b->ClientUi.open(client,new HistoryScreen(screen))).bounds(8,4,Math.max(84,client.font.width(HistoryUi.text("details"))+16),20).build();
            ownedButtons.add(details);Screens.getButtons(screen).add(details);
            int slots=Math.max(1,(width/3)/26);
            var buttons=new ArrayList<Button>();
            for(int i=0;i<slots;i++){final int index=i;
                var button=Button.builder(Component.empty(),b->{var ps=ledger.snapshot().players();if(index<ps.size())ClientUi.open(client,new HistoryScreen(screen,ps.get(index).id()));}).bounds(width-8-(slots-i)*26,4,22,22).build();
                button.visible=false;ownedButtons.add(button);
                buttons.add(button);Screens.getButtons(screen).add(button);
            }
            ScreenEvents.beforeRender(screen).register((s,g,mx,my,delta)->{hovered=null;int count=Math.min(slots,ledger.snapshot().players().size());for(int i=0;i<buttons.size();i++){buttons.get(i).visible=i<count;buttons.get(i).setX(s.width-8-(count-i)*26);}});
            ScreenEvents.afterRender(screen).register((s,g,mx,my,delta)->{
                var players=ledger.snapshot().players();UUID faceHover=null;
                for(int i=0;i<Math.min(slots,players.size());i++){
                    var p=players.get(i);int x=s.width-5-(Math.min(slots,players.size())-i)*26;HistoryUi.face(g,p.id(),x,7,false);
                    if(mx>=x-3&&mx<x+19&&my>=4&&my<26)faceHover=p.id();
                }
                if(faceHover!=null)HistoryUi.tooltip(g,java.util.List.of(HistoryUi.name(faceHover),HistoryUi.text("earned_count",ledger.count(faceHover,catalog.keySet())),HistoryUi.text("view_records")),mx,my,s.width,s.height);
                else if(hovered!=null)HistoryUi.achievementTooltip(g,hovered,mx,my,s.width,s.height);
            });
        });
    }
}
