package pl.zorza.client;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Util;
import pl.zorza.client.gui.ZoraButton;
import pl.zorza.client.gui.ZoraModsScreen;

import java.util.List;

/** "Zora Mods" + folder z modami: w menu głównym pod Realms, w menu pauzy nad "Back to Game". */
public final class MenuButtons {
    private MenuButtons() {}

    private static ClickableWidget find(List<ClickableWidget> list, String key) {
        for (ClickableWidget w : list) {
            if (w.getMessage().getContent() instanceof TranslatableTextContent t && t.getKey().equals(key)) return w;
        }
        return null;
    }

    public static void add(MinecraftClient client, Screen screen) {
        List<ClickableWidget> buttons = Screens.getButtons(screen);
        int x, y, w;
        if (screen instanceof TitleScreen) {
            ClickableWidget anchor = find(buttons, "menu.online");
            if (anchor == null) anchor = find(buttons, "menu.multiplayer");
            if (anchor == null) return;
            x = anchor.getX();
            w = anchor.getWidth();
            y = anchor.getY() + 24;
            int lowest = 0;
            for (ClickableWidget b : buttons) if (b.getY() >= y) lowest = Math.max(lowest, b.getY() + b.getHeight());
            if (lowest + 24 <= screen.height - 14) {
                for (ClickableWidget b : buttons) if (b.getY() >= y) b.setY(b.getY() + 24);   // Options/Quit w dół
            } else {
                // mało miejsca (małe okno) – główne przyciski idą w górę
                for (ClickableWidget b : buttons) if (b.getY() < y && b.getY() > 20 && !(b instanceof net.minecraft.client.gui.widget.TextWidget)) b.setY(b.getY() - 24);
                y -= 24;
            }
        } else if (screen instanceof GameMenuScreen) {
            ClickableWidget back = find(buttons, "menu.returnToGame");
            if (back == null) return;                       // menu bez przycisków (F3+Esc)
            // tytuł "Game Menu" to też widżet – nasz przycisk musi być pod nim
            int titleBottom = 0;
            for (ClickableWidget b : buttons)
                if (b instanceof net.minecraft.client.gui.widget.TextWidget) titleBottom = Math.max(titleBottom, b.getY() + b.getHeight());
            int need = titleBottom + 6 - (back.getY() - 24);
            if (need > 0) for (ClickableWidget b : buttons) if (!(b instanceof net.minecraft.client.gui.widget.TextWidget)) b.setY(b.getY() + need);
            x = back.getX();
            w = back.getWidth();
            y = back.getY() - 24;
        } else return;
        buttons.add(new ZoraButton(x, y, w, 20, "Zora Mods", null, false,
                () -> client.setScreen(new ZoraModsScreen(screen))));
        buttons.add(new ZoraButton(x - 24, y, 20, 20, "Mods folder", "folder", true,
                () -> Util.getOperatingSystem().open(FabricLoader.getInstance().getGameDir().resolve("mods").toFile())));
    }
}
