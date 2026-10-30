package dev.sharedadvancements.fabric;

import dev.sharedadvancements.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** A scrollable advancement collection with direct player selection. */
public final class HistoryScreen extends Screen {
    private final Screen parent;
    private UUID selected;
    private boolean timeline;
    private int scroll, playerOffset;
    private Button allTab, timeTab;
    private final java.util.List<Button> faceButtons=new ArrayList<>();
    private java.util.List<Ledger.Player> visiblePlayers=java.util.List.of();
    public HistoryScreen(Screen parent) { this(parent,null); }
    public HistoryScreen(Screen parent, UUID selected) { super(Component.literal("Shared Advancements")); this.parent=parent; this.selected=selected; }
    private int slots() { return Math.max(1,(width-110)/26); }
    private int top() { return 106; }
    private int bottom() { return height-30; }
    private int rowHeight() { return timeline?34:46; }
    private int columns() { return timeline?1:Math.max(1,Math.min(4,(width-24)/190)); }
    private java.util.List<HistoryView.Entry> entries() { return HistoryView.entries(SharedClient.ledger.snapshot(),SharedClient.catalog.keySet(),selected,timeline); }
    @Override protected void init() {
        faceButtons.clear();
        addRenderableWidget(Button.builder(Component.literal("전체 플레이어"),b->{selected=null;scroll=0;}).bounds(12,42,86,22).build());
        for(int i=0;i<slots();i++) { final int index=i;
            faceButtons.add(addRenderableWidget(Button.builder(Component.empty(),b->{if(index<visiblePlayers.size()){ selected=visiblePlayers.get(index).id();scroll=0; }}).bounds(width-14-(slots()-i)*26,42,22,22).build()));
        }
        allTab=addRenderableWidget(Button.builder(Component.literal("전체 도전과제"),b->{timeline=false;scroll=0;}).bounds(12,76,100,20).build());
        timeTab=addRenderableWidget(Button.builder(Component.literal("시간순 기록"),b->{timeline=true;scroll=0;}).bounds(116,76,92,20).build());
        addRenderableWidget(Button.builder(Component.literal("돌아가기"),b->onClose()).bounds(width-78,height-24,66,20).build());
    }
    @Override public void onClose() { ClientUi.open(minecraft,parent); }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(y>=40 && y<=66) playerOffset=Math.max(0,Math.min(Math.max(0,SharedClient.ledger.snapshot().players().size()-slots()),playerOffset-(int)Math.signum(vertical)));
        else scroll=Math.max(0,scroll-(int)(vertical*rowHeight()));
        return true;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        var snapshot=SharedClient.ledger.snapshot();
        playerOffset=Math.min(playerOffset,Math.max(0,snapshot.players().size()-slots()));
        visiblePlayers=snapshot.players().subList(playerOffset,Math.min(snapshot.players().size(),playerOffset+slots()));
        for(int i=0;i<faceButtons.size();i++) { faceButtons.get(i).visible=i<visiblePlayers.size(); if(i<visiblePlayers.size()) faceButtons.get(i).setX(width-14-(visiblePlayers.size()-i)*26); }
        allTab.active=timeline; timeTab.active=!timeline;
        g.fill(0,0,width,height,0xF210161E); g.fill(0,0,width,101,0xFA19232E);
        super.render(g,mx,my,delta);
        g.drawCenteredString(font,"Shared Advancements",width/2,10,0xFFF4F0E8);
        long done=HistoryView.entries(snapshot,SharedClient.catalog.keySet(),null,true).stream().filter(e->SharedClient.catalog.containsKey(e.id())).count();
        String summary=selected==null?"함께 달성한 도전과제  "+done+" / "+SharedClient.catalog.size():HistoryUi.name(selected)+" 님이 직접 달성한 기록";
        g.drawCenteredString(font,summary,width/2,26,selected==null?0xFFADC7C2:HistoryUi.color(selected));
        if(width>340) g.drawString(font,"얼굴을 눌러 플레이어 선택",218,82,0xFF92A5B5);
        var entries=entries(); int columns=columns(), step=rowHeight();
        int viewport=Math.max(1,bottom()-top()); int content=((entries.size()+columns-1)/columns)*step;
        scroll=Math.min(scroll,Math.max(0,content-viewport));
        int cardWidth=(width-30-(columns-1)*8)/columns;
        String hovered=null;
        g.enableScissor(10,top(),width-10,bottom());
        for(int i=0;i<entries.size();i++) {
            int y=top()+(i/columns)*step-scroll; if(y+step<top()||y>=bottom())continue;
            int x=12+(i%columns)*(cardWidth+8); var e=entries.get(i);
            boolean hover=mx>=x&&mx<x+cardWidth&&my>=Math.max(y,top())&&my<Math.min(y+step-5,bottom());
            int accent=e.credits().isEmpty()?(e.completed()?0xFFE3CD88:0xFF485767):HistoryUi.color(e.credits().getFirst().player());
            g.fill(x,y,x+cardWidth,y+step-5,hover?0xFF344555:0xFF222F3C); g.fill(x,y,x+3,y+step-5,accent);
            HistoryUi.icon(g,e.id(),x+10,y+7);
            g.drawString(font,font.plainSubstrByWidth(HistoryUi.title(e.id()),cardWidth-44),x+34,y+7,e.completed()?0xFFF3EEE1:0xFF94A2B1);
            String subtitle=!e.completed()?"미달성":e.credits().isEmpty()?"이전 기록":HistoryUi.name(e.credits().getFirst().player())+" 님이 달성";
            if(timeline)subtitle=HistoryUi.date(e.time())+" · "+subtitle;
            g.drawString(font,font.plainSubstrByWidth(subtitle,cardWidth-44),x+34,y+19,accent);
            if(hover)hovered=e.id();
        }
        g.disableScissor();
        if(entries.isEmpty()) g.drawCenteredString(font,selected==null?"아직 달성 기록이 없어요":"이 플레이어의 직접 달성 기록이 아직 없어요",width/2,top()+24,0xFFADBACA);
        if(content>viewport) {
            int thumb=Math.max(16,viewport*viewport/content), y=top()+(viewport-thumb)*scroll/(content-viewport);
            g.fill(width-8,top(),width-5,bottom(),0xFF283541);g.fill(width-8,y,width-5,y+thumb,0xFF91B7B1);
        }
        g.drawString(font,entries.size()+"개 항목 · 마우스 휠로 스크롤",12,height-17,0xFF92A5B5);
        UUID faceHover=null;
        for(int i=0;i<visiblePlayers.size();i++) {
            var p=visiblePlayers.get(i); int x=width-11-(visiblePlayers.size()-i)*26;
            HistoryUi.face(g,p.id(),x,45,p.id().equals(selected));
            if(mx>=x-3&&mx<x+19&&my>=42&&my<64)faceHover=p.id();
        }
        if(faceHover!=null)HistoryUi.tooltip(g,java.util.List.of(HistoryUi.name(faceHover),"직접 달성 "+SharedClient.ledger.count(faceHover,SharedClient.catalog.keySet())+"개", "클릭하여 기록 보기"),mx,my,width,height);
        else if(hovered!=null)HistoryUi.achievementTooltip(g,hovered,mx,my,width,height);
    }
}
