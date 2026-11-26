package pl.zorza.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.zorza.client.gui.VanillaStyle;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/** Direct Connection: po wpisaniu IP nad "Join Server" pokazuje się serwer jak na liście (opis, gracze, ping). */
@Mixin(DirectConnectScreen.class)
public abstract class DirectConnectMixin extends Screen {
    @Shadow
    private TextFieldWidget addressField;
    @Shadow
    private ButtonWidget selectServerButton;

    @Unique
    private static final Identifier ZORZA_ICON = Identifier.of("zorza", "direct_connect_icon");
    @Unique
    private static final int CARD_H = 40;
    @Unique
    private final MultiplayerServerListPinger zorza$pinger = new MultiplayerServerListPinger();
    @Unique
    private volatile ServerInfo zorza$info;
    @Unique
    private String zorza$addr = "";
    @Unique
    private long zorza$changed = 0;
    @Unique
    private byte[] zorza$iconBytes;
    @Unique
    private boolean zorza$hasIcon;

    protected DirectConnectMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void zorza$init(CallbackInfo ci) {
        int from = selectServerButton.getY();
        for (Element e : children()) {
            if (e instanceof ButtonWidget b && b.getY() >= from) b.setY(b.getY() + CARD_H + 8);   // miejsce na podgląd
        }
    }

    @Unique
    private void zorza$startPing(String address) {
        ServerInfo info = new ServerInfo(address, address, ServerInfo.ServerType.OTHER);
        info.label = Text.translatable("multiplayer.status.pinging").formatted(Formatting.DARK_GRAY);
        info.playerCountLabel = Text.empty();
        info.ping = -2;
        zorza$info = info;
        zorza$hasIcon = false;
        zorza$iconBytes = null;
        Thread t = new Thread(() -> {
            try {
                zorza$pinger.add(info, () -> {}, () -> {});
            } catch (UnknownHostException e) {
                info.ping = -1;
                info.label = Text.translatable("multiplayer.status.cannot_resolve").formatted(Formatting.DARK_RED);
            } catch (Exception e) {
                info.ping = -1;
                info.label = Text.translatable("multiplayer.status.cannot_connect").formatted(Formatting.DARK_RED);
            }
        }, "Zora direct connect ping");
        t.setDaemon(true);
        t.start();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void zorza$render(DrawContext ctx, int mx, int my, float delta, CallbackInfo ci) {
        zorza$pinger.tick();
        String a = addressField.getText().trim();
        long now = System.currentTimeMillis();
        if (!a.equals(zorza$addr)) {
            zorza$addr = a;
            zorza$changed = now;
            zorza$info = null;
        }
        if (zorza$info == null && !a.isEmpty() && now - zorza$changed > 600 && ServerAddress.isValid(a)) zorza$startPing(a);
        ServerInfo info = zorza$info;
        if (info == null) return;

        int w = Math.min(305, width - 20), x = width / 2 - w / 2, y = selectServerButton.getY() - CARD_H - 6;
        VanillaStyle.button(ctx, this, x, y, w, CARD_H, true, false, 1f);
        // ikona serwera
        byte[] fav = info.getFavicon();
        if (fav != null && !Arrays.equals(fav, zorza$iconBytes)) {
            zorza$iconBytes = fav;
            try {
                MinecraftClient.getInstance().getTextureManager().registerTexture(ZORZA_ICON, pl.zorza.client.compat.Gfx.nativeTexture(NativeImage.read(fav)));
                zorza$hasIcon = true;
            } catch (Exception e) {
                zorza$hasIcon = false;
            }
        }
        Identifier icon = zorza$hasIcon ? ZORZA_ICON : Identifier.ofVanilla("textures/misc/unknown_server.png");
        pl.zorza.client.compat.Gfx.texture(ctx, icon, x + 4, y + 4, 0, 0, 32, 32, 32, 32, -1);

        int tx = x + 41, right = x + w - 6;
        ctx.drawText(textRenderer, a, tx, y + 5, 0xFFFFFFFF, false);
        // ping
        String sprite = info.ping < 0 ? (info.ping == -2 ? "server_list/pinging_" + (1 + (int) (now / 100 % 5)) : "server_list/unreachable")
                : info.ping < 150 ? "server_list/ping_5" : info.ping < 300 ? "server_list/ping_4"
                : info.ping < 600 ? "server_list/ping_3" : info.ping < 1000 ? "server_list/ping_2" : "server_list/ping_1";
        pl.zorza.client.compat.Gfx.guiSprite(ctx, Identifier.ofVanilla(sprite), right - 10, y + 5, 10, 8);
        if (info.playerCountLabel != null && info.ping >= 0) {
            int pw = textRenderer.getWidth(info.playerCountLabel);
            ctx.drawText(textRenderer, info.playerCountLabel, right - 14 - pw, y + 5, 0xFF808080, false);
        }
        List<OrderedText> lines = textRenderer.wrapLines(info.label == null ? Text.empty() : info.label, right - tx);
        for (int i = 0; i < Math.min(2, lines.size()); i++) ctx.drawText(textRenderer, lines.get(i), tx, y + 16 + i * 10, 0xFF808080, false);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void zorza$removed(CallbackInfo ci) {
        zorza$pinger.cancel();
        if (zorza$hasIcon) MinecraftClient.getInstance().getTextureManager().destroyTexture(ZORZA_ICON);
    }
}
