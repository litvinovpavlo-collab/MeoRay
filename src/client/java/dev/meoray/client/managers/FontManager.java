package dev.meoray.client.managers;

import com.google.common.base.Suppliers;
import dev.meoray.client.util.render.msdf.MsdfFont;
import java.util.function.Supplier;

public class FontManager {
    public static final Supplier<MsdfFont> SUISSEINTMEDIUM = Suppliers.memoize(() -> MsdfFont.builder().atlas("suisseintlmedium").data("suisseintlmedium").build());
    public static final Supplier<MsdfFont> SUISSEINTREGULAR = Suppliers.memoize(() -> MsdfFont.builder().atlas("suisseintlregular").data("suisseintlregular").build());
    public static final Supplier<MsdfFont> WILD = Suppliers.memoize(() -> MsdfFont.builder().atlas("wild").data("wild").build());
    public static final Supplier<MsdfFont> CATEGORY = Suppliers.memoize(() -> MsdfFont.builder().atlas("font").data("font").build());
    public static final Supplier<MsdfFont> ICONS = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons_atlas").data("icons_font").build());
    public static final Supplier<MsdfFont> SF = Suppliers.memoize(() -> MsdfFont.builder().atlas("sf").data("sf").build());
    public static final Supplier<MsdfFont> MAINMENU = Suppliers.memoize(() -> MsdfFont.builder().atlas("mainmenu").data("mainmenu").build());
    public static final Supplier<MsdfFont> ICONS4 = Suppliers.memoize(() -> MsdfFont.builder().atlas("icons4").data("icons4").build());
    public static final Supplier<MsdfFont> BOLD = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());

    public static MsdfFont getFont(String name) {
        return switch (name) {
            case "wild" -> WILD.get();
            case "icons" -> ICONS.get();
            default -> CATEGORY.get();
        };
    }
}
