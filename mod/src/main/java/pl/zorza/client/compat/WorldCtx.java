package pl.zorza.client.compat;

import net.minecraft.client.render.Camera;

/** To, czego mod potrzebuje przy rysowaniu w świecie – niezależnie od wersji Fabric API. */
public record WorldCtx(Camera camera, float delta) {
}
