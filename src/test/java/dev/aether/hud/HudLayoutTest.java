package dev.aether.hud;

import dev.aether.TestSupport;

public final class HudLayoutTest {
    public static void main(String[] args) {
        HudLayout layout = new HudLayout(4);
        layout.add("hud.fps", 1, 1);
        layout.add("hud.coords", 10, 10);

        layout.move("hud.fps", 7, 10, true);
        layout.scale("hud.fps", 1.5F);
        layout.opacity("hud.fps", 0.75F);
        layout.layer("hud.coords", -1);

        TestSupport.assertEquals(Integer.valueOf(8), Integer.valueOf(layout.get("hud.fps").x()), "X should snap to grid.");
        TestSupport.assertEquals(Integer.valueOf(12), Integer.valueOf(layout.get("hud.fps").y()), "Y should snap to grid.");
        TestSupport.assertTrue(layout.get("hud.fps").scale() == 1.5F, "Scale should be stored.");
        TestSupport.assertEquals("hud.coords", layout.renderOrder().get(0).id(), "Lower layer should render first.");
    }
}

