#!/usr/bin/env python3
"""Recolours the Leaf Client 1.8.9 UI art into Aether's transparent-black + white palette.

Why this exists
---------------
Aether's GUI reproduces the Leaf Client 1.8.9 screen architecture (see docs/GUI_REBUILD.md), and a
later pass made the geometry match Leaf exactly. Leaf paints its panels with pre-rendered PNGs whose
shape lives in the alpha channel and whose detail is a second flat tone, authored 1:1 for 1080p, so
the closest possible match is to ship that same art - recoloured, because the palette must be
translucent black surfaces with white detail instead of Leaf's opaque grey panels and green hover.

This script is a *build-time* tool: it writes the recoloured copies into
src/main/resources/assets/aether/leaf/, and the mod only ever loads the results (no runtime image
processing, no ImageIO in the shipped client).

How the source art is actually built
------------------------------------
Measuring the files (``--analyze``) shows they are not photographic panels at all. Almost every
texture uses two or three *flat* luminance tones and two or three *flat* alpha levels:

* the panel interior sits at one alpha "plateau" (128 for the cards and tiles, 255 for the glyphs);
* a soft baked drop shadow is a *ramp below* that plateau, and it carries the *same* luminance as the
  panel's dark tone - so luminance alone cannot tell a shadow from an icon;
* the detail (an icon cut into a tab, a knob on a slider) is a second luminance tone at the plateau.

That is why the classification is driven by the alpha plateau: the interior of the shape is where the
alpha has reached the plateau, and only there is a luminance difference real detail rather than the
shadow ramp. (An earlier version of this script gated the interior on ``alpha >= 200``, which is above
every one of these textures' plateau and therefore silently dropped every icon. ``--ascii`` could not
show it because a black glass body and a fully transparent pixel both render as blank there;
``--sheet`` exists so the result is inspectable as an image instead.)

Rules
-----
``glass(strength)``              black, alpha = source alpha x strength. Flat surfaces.
``white(strength)``              flat white at the shape's own alpha. Glyphs and knobs.
``shade(strength, lo, hi)``      luminance remapped to greyscale. The "bright means on" art.
``detail(strength, lo, hi)``     black glass body + the panel's own detail keyed white.

Usage
-----
    python scripts/leaf_assets.py --analyze           # classification table over the source art
    python scripts/leaf_assets.py --ascii mod.png     # coarse ASCII view of one source texture
    python scripts/leaf_assets.py --convert           # write the recoloured assets
    python scripts/leaf_assets.py --sheet             # write build/leaf-sheet.html and open it

``LEAF_SOURCE`` overrides where the source art is read from (default: the Leaf checkout under the
system temp directory), which is also how ``--ascii`` can be pointed at the *generated* art.

The source art is Leaf Client 1.8.9 (GPLv3). Aether already carries Leaf-derived GUI code with the
attribution headers that licence requires; the generated art is covered by the same notice, recorded
in assets/aether/leaf/NOTICE.txt.
"""

import argparse
import base64
import io
import os
import sys

try:
    from PIL import Image, ImageDraw
except ImportError:
    print("Pillow is required: pip install pillow", file=sys.stderr)
    raise

SRC = os.environ.get(
    "LEAF_SOURCE",
    os.path.join(os.path.expanduser("~"), "AppData", "Local", "Temp", "leaf-client-ref",
                 "leafclient-1.8.9", "src", "main", "resources", "assets", "minecraft",
                 "leafclient"),
)
OUT = os.path.join("src", "main", "resources", "assets", "aether", "leaf")
SHEET = os.path.join("build", "leaf-sheet.html")

# Output name -> the Leaf source it is cloned from, for the assets Leaf does not have. Aether's own
# appearance and profiles tiles are Leaf tabs run through the same rule, so the pipeline stays the
# single source of truth for every file the mod ships and --convert can rebuild it from scratch.
SOURCE_NAMES = {
    "button/appearance.png": "system.png",
    "button/profiles.png": "system.png",
}

# name -> (rule, a, b, c). Read the rules in convert_image() before changing a row.
#
#   glass(strength)              black at strength x the source alpha - plain surfaces
#   detail(strength, lo, hi)     black glass body + the texture's own detail keyed white
#   detail(strength, lo, hi, g)  as above, with the white detail's alpha gained by g (clamped to
#                                opaque), for art Leaf painted translucent that has to read as solid
#   white(strength)              the alpha shape painted flat white - glyphs and knobs
#   shade(strength, lo, hi)      luminance remapped to greyscale - the "bright means on" art
ASSETS = {
    # Screen backdrops. Measured, these are a full-screen scrim (alpha 38) with a rounded panel
    # sitting on it (alpha 142) - the container Leaf draws its tabs and card grid inside. Their
    # luminance is a flat light haze, which has no black-glass equivalent: recolouring it as detail
    # would smear a grey blob over the game. Painted as plain glass, the two alpha plateaus become
    # the scrim and the panel, so the panel edge survives as a step in the black instead.
    "main.png":                ("glass", 0.80),
    "main_mod.png":            ("glass", 0.80),

    # Module cards: 170x182 tiles whose own art is a dark icon cut into a bright body, so the icon
    # becomes the white detail and the body becomes black glass.
    "mod.png":                 ("detail", 1.00, 34.0, 78.0),

    # Category tabs and the small square buttons. Leaf's art is 187x117 and 117x117; all of them are
    # stretched into their design rectangle, exactly as Leaf's drawModalRectWithCustomSizedTexture
    # does, so the source aspect ratio is irrelevant here.
    "button/mod.png":          ("detail", 1.00, 34.0, 78.0),
    "button/cosmetic.png":     ("detail", 1.00, 34.0, 78.0),
    "button/location.png":     ("detail", 1.00, 34.0, 78.0),
    "button/setting.png":      ("detail", 1.00, 34.0, 78.0),
    # Aether's own Appearance and Profiles tabs: Leaf's system.png art under our names, so the
    # navigation tiles are all cut from Leaf's set and nothing in the bar is drawn by hand.
    # system.png is the one tab Leaf painted at half alpha (a 128 plateau against the others' 255),
    # which in Leaf sat on a bright panel but here would make the tile read dimmer than its
    # neighbours even when it is the current screen - so the alpha is gained to opaque, which is
    # what the other four tabs are.
    "button/appearance.png":   ("detail", 1.00, 34.0, 78.0, 2.00),
    "button/profiles.png":     ("detail", 1.00, 34.0, 78.0, 2.00),
    "button/home.png":         ("detail", 1.00, 34.0, 78.0),
    "button/close.png":        ("detail", 1.00, 34.0, 78.0),
    "button/arrow_left.png":   ("detail", 1.00, 34.0, 78.0),
    "button/arrow_right.png":  ("detail", 1.00, 34.0, 78.0),
    "system.png":              ("detail", 1.00, 34.0, 78.0),

    # Setting controls.
    "true.png":                ("shade", 0.75, 96.0, 232.0),   # enabled: bright surface
    "false.png":               ("shade", 0.85, 96.0, 232.0),   # disabled: black body, white rim
    "select.png":              ("detail", 1.00, 34.0, 78.0),   # pills and cosmetic entries
    "bar_main.png":            ("glass", 0.45),                # slider track
    "bar_point.png":           ("white", 0.88),                # slider knob: white, unmistakable
    "scroll_main.png":         ("glass", 0.45),                # scrollbar track
    "scroll_bar.png":          ("white", 0.62),                # scrollbar thumb
    "field/search.png":        ("detail", 1.00, 34.0, 78.0),

    # Pure glyphs: art whose shape lives entirely in the alpha channel. Painted flat white.
    "gear_small.png":          ("white", 1.0),
    "hide.png":                ("white", 1.0),
    "show.png":                ("white", 1.0),
    "next.png":                ("white", 1.0),
    "back.png":                ("white", 1.0),
    "play.png":                ("white", 1.0),
    "stop.png":                ("white", 1.0),
    "recent.png":              ("white", 1.0),
    "leaf.png":                ("white", 1.0),
}

def _paint_appearance(draw, ox, oy, size):
    """Half-filled ring: the contrast between the worn palette and every other one."""
    inset = int(size * 0.10)
    ring = max(2, int(size * 0.075))
    box = (ox + inset, oy + inset, ox + size - inset, oy + size - inset)
    draw.ellipse(box, outline=255, width=ring)
    draw.chord(box, 90, 270, fill=255)


def _paint_profiles(draw, ox, oy, size):
    """Two stacked cards: the profile manager's own object."""
    stroke = max(2, int(size * 0.07))
    radius = max(2, int(size * 0.10))
    back = (ox + int(size * 0.08), oy + int(size * 0.04),
            ox + int(size * 0.58), oy + int(size * 0.44))
    front = (ox + int(size * 0.32), oy + int(size * 0.40),
             ox + int(size * 0.92), oy + int(size * 0.90))
    draw.rounded_rectangle(back, radius=radius, outline=255, width=stroke)
    # Knock the back card out under the front one so the stack reads as two separate cards.
    draw.rounded_rectangle(front, radius=radius, fill=0)
    draw.rounded_rectangle(front, radius=radius, outline=255, width=stroke)
    line = (ox + int(size * 0.44), oy + int(size * 0.56),
            ox + int(size * 0.80), oy + int(size * 0.62))
    draw.rounded_rectangle(line, radius=max(1, stroke // 2), fill=255)


# Tabs whose source art carries no icon of its own, and the glyph each is stamped with after the
# recolour. Leaf's system.png is a plain frame - which is why the old Themes tab shipped as a blank
# tile - and it is the source of both Aether-added tabs, so their glyphs are drawn here, through the
# same pipeline that writes every other texture: no hand-edited PNGs and no runtime stand-in.
TAB_GLYPHS = {
    "button/appearance.png": _paint_appearance,
    "button/profiles.png": _paint_profiles,
}


# Source name -> the alpha plateau measured by --analyze, for the --analyze report only. Kept out of
# the conversion path: the plateau is measured per file, never assumed.
PLATEAU_NOTE = "\n".join([
    "Alpha plateaus measured from the source art:",
    "  128   cards (mod.png), tabs and square buttons (button/*), selects, sliders, scrollbars,",
    "        text fields - i.e. everything that is a panel over the game",
    "  255   pure glyphs (gear_small, next, back, play, stop, hide, show, leaf)",
    "  192   the toggles' knobs, on 128 plates (true/false)",
    "   38   the full-screen scrim of the backdrops (main, main_mod), whose panel sits at 142",
])


def luminance(r, g, b):
    return 0.299 * r + 0.587 * g + 0.114 * b


def smoothstep(edge0, edge1, value):
    if value <= edge0:
        return 0.0
    if value >= edge1:
        return 1.0
    t = (value - edge0) / (edge1 - edge0)
    return t * t * (3.0 - 2.0 * t)


def load(name, root=None):
    """Loads an asset by its Aether (output) name. Source reads follow SOURCE_NAMES, so an asset
    with no Leaf original of its own (the appearance/profiles tiles) resolves to the art it was
    cloned from."""
    if root is None:
        name = SOURCE_NAMES.get(name, name)
    path = os.path.join(root or SRC, name.replace("/", os.sep))
    if not os.path.exists(path):
        return None
    return Image.open(path).convert("RGBA")


def alpha_plateau(image):
    """The alpha level of the shape's interior. Every texture here ramps its alpha down through a
    soft baked shadow around the border, so the most common non-zero alpha is the interior rather
    than some arbitrary threshold."""
    pixels = image.load()
    width, height = image.size
    histogram = {}
    for y in range(height):
        for x in range(width):
            alpha = pixels[x, y][3]
            if alpha > 0:
                histogram[alpha] = histogram.get(alpha, 0) + 1
    if not histogram:
        return 255
    return max(histogram.items(), key=lambda item: item[1])[0]


def surface_mask(image):
    """1 inside the shape, 0 out in the baked shadow ramp, as a lookup over alpha.

    The two edges are placed relative to the measured plateau: 0.80 of it is where the interior
    clearly starts, 0.95 is where it is fully opaque. Both matter - too low and the shadow's ramp
    gets mistaken for detail (the shadow shares the dark tone with the icons), too high and
    anti-aliased icon edges drop out.
    """
    plateau = alpha_plateau(image)
    low = plateau * 0.80
    high = plateau * 0.95

    def mask(alpha):
        return smoothstep(low, high, alpha)

    return mask


def body_luminance(image, mask):
    """The panel's own colour: the most common luminance of the fully-interior pixels. The texture's
    detail deliberately does not move this value, which is what lets the detail be keyed out without
    a hand-tuned threshold per file."""
    pixels = image.load()
    width, height = image.size
    histogram = {}
    for y in range(height):
        for x in range(width):
            r, g, b, alpha = pixels[x, y]
            if mask(alpha) < 0.995:
                continue
            bucket = int(luminance(r, g, b) // 4)
            histogram[bucket] = histogram.get(bucket, 0) + 1
    if not histogram:
        return 0.0
    best = max(histogram.items(), key=lambda item: item[1])[0]
    return (best + 0.5) * 4.0


def stamp_tab_glyph(image, painter):
    """Composites a programmatic white glyph over a converted tab's art.

    The glyph is drawn 4x and downsampled, so the edges land anti-aliased like the rest of the art.
    The box is a centred square at 60% of the tile height - the same visual weight Leaf's own tile
    glyphs have (mod.png's icon spans about 65% of the tile height).
    """
    width, height = image.size
    box = int(round(height * 0.60))
    supersample = 4
    layer = Image.new("L", (width * supersample, height * supersample), 0)
    draw = ImageDraw.Draw(layer)
    origin_x = (width * supersample - box * supersample) // 2
    origin_y = (height * supersample - box * supersample) // 2
    painter(draw, origin_x, origin_y, box * supersample)
    layer = layer.resize((width, height), Image.LANCZOS)
    image.paste(Image.new("RGBA", (width, height), (255, 255, 255, 255)), (0, 0), layer)
    return image


def convert_image(image, rule, a1, a2, a3, a4=1.0):
    """Returns the recoloured copy: translucent black surfaces plus white detail.

    a1-a3 are the rule's own parameters; a4 is the optional alpha gain of the detail rule.
    """
    width, height = image.size
    source = image.load()
    out = Image.new("RGBA", (width, height))
    target = out.load()

    mask = surface_mask(image) if rule == "detail" else None
    body = body_luminance(image, mask) if rule == "detail" else 0.0

    for y in range(height):
        for x in range(width):
            r, g, b, alpha_source = source[x, y]
            if alpha_source == 0:
                continue
            coverage = alpha_source / 255.0
            # The gain lifts the *detail* only: the glyphs of a translucent original have to be as
            # solid as their neighbours' in a row of tiles, while the surface keeps Leaf's glass.
            detail_coverage = min(1.0, coverage * a4)
            lum = luminance(r, g, b)

            if rule == "white":
                target[x, y] = (255, 255, 255, int(round(alpha_source * a1)))
                continue

            if rule == "glass":
                target[x, y] = (0, 0, 0, int(round(alpha_source * a1)))
                continue

            if rule == "shade":
                # Bright art stays bright: this is the "enabled" look, so it has to read as the
                # prominent surface rather than being flattened into the black glass.
                value = smoothstep(a2, a3, lum)
                channel = int(round(value * 255.0))
                target[x, y] = (channel, channel, channel, int(round(alpha_source * a1)))
                continue

            if rule == "detail":
                # Only a luminance difference *inside* the panel is detail; the shadow ramp shares the
                # dark tone with the icons, so the surface mask is what keeps it out.
                detail = smoothstep(a2, a3, abs(lum - body)) * mask(alpha_source)
            else:
                continue

            detail_alpha = detail * detail_coverage
            glass_alpha = coverage * a1 * (1.0 - detail)
            if detail_alpha <= 0.0 and glass_alpha <= 0.0:
                continue
            alpha = detail_alpha + glass_alpha * (1.0 - detail_alpha)
            if alpha <= 0.0:
                continue
            white = min(1.0, detail_alpha / alpha)
            channel = int(round(white * 255.0))
            target[x, y] = (channel, channel, channel, int(round(alpha * 255.0)))
    return out


def analyze():
    print("%-26s %-9s %-6s %-9s %-8s %-8s %s" % (
        "asset", "size", "cov", "plateau", "body", "bright", "rule"))
    for name in sorted(ASSETS):
        image = load(name)
        if image is None:
            print("%-26s MISSING" % name)
            continue
        width, height = image.size
        pixels = image.load()
        total = width * height
        coverage = 0
        bright = 0
        lit = 0
        for y in range(height):
            for x in range(width):
                r, g, b, alpha = pixels[x, y]
                if alpha > 8:
                    coverage += 1
                    lit += 1
                    if luminance(r, g, b) > 200:
                        bright += 1
        mask = surface_mask(image)
        body = body_luminance(image, mask) if ASSETS[name][0] == "detail" else 0.0
        print("%-26s %4dx%-4d %-6s %-9d %-8.0f %-8s %s" % (
            name, width, height, "%.2f" % (coverage / float(total)), alpha_plateau(image), body,
            "%.2f" % (bright / float(lit)) if lit else "-", ASSETS[name][0]))
    print()
    print(PLATEAU_NOTE)


def ascii_preview(name, columns=72):
    """Prints the texture twice: once as luminance art and once as an alpha mask.

    Note that a black glass body and a transparent pixel both render as blank in the luminance view,
    so this is a shape check, not a colour check - use ``--sheet`` to actually look at the result.
    """
    image = load(name)
    if image is None:
        print("missing: " + name)
        return
    width, height = image.size
    pixels = image.load()
    rows = max(1, int(columns * height / float(width) * 0.5))
    ramp = " .:-=+*#%@"

    def sample(column, row):
        x = min(width - 1, int((column + 0.5) * width / float(columns)))
        y = min(height - 1, int((row + 0.5) * height / float(rows)))
        return pixels[x, y]

    print("--- %s (%dx%d) luminance where opaque, ' ' = transparent/black ---" % (name, width, height))
    for row in range(rows):
        line = []
        for column in range(columns):
            r, g, b, alpha = sample(column, row)
            if alpha <= 8:
                line.append(" ")
                continue
            line.append(ramp[max(0, min(9, int(luminance(r, g, b) / 255.0 * 9)))])
        print("".join(line))

    print("--- %s alpha mask ---" % name)
    for row in range(rows):
        line = []
        for column in range(columns):
            line.append(ramp[max(0, min(9, int(sample(column, row)[3] / 255.0 * 9)))])
        print("".join(line))


def convert():
    if not os.path.isdir(SRC):
        print("Leaf source not found at " + SRC + "\nset LEAF_SOURCE to the leafclient asset dir",
              file=sys.stderr)
        return 1
    os.makedirs(OUT, exist_ok=True)
    written = 0
    for name in sorted(ASSETS):
        image = load(name)
        if image is None:
            print("missing: " + name, file=sys.stderr)
            continue
        rule = ASSETS[name][0]
        params = list(ASSETS[name][1:])
        # A row may carry three or four parameters; the missing alpha gain is 1.0 (no gain).
        strength = params[0] if len(params) > 0 else 0.0
        low = params[1] if len(params) > 1 else 0.0
        high = params[2] if len(params) > 2 else 0.0
        gain = params[3] if len(params) > 3 else 1.0
        result = convert_image(image, rule, strength, low, high, gain)
        painter = TAB_GLYPHS.get(name)
        if painter is not None:
            result = stamp_tab_glyph(result, painter)
        destination = os.path.join(OUT, name.replace("/", os.sep))
        os.makedirs(os.path.dirname(destination), exist_ok=True)
        result.save(destination)
        written += 1
    print("wrote %d recoloured assets to %s" % (written, OUT))
    return 0


def thumb(image, limit=150):
    """A small PNG data URI, so the sheet can show the source art that lives outside the workspace
    and the generated art in the same document."""
    copy = image.copy()
    copy.thumbnail((limit, limit), Image.LANCZOS)
    buffer = io.BytesIO()
    copy.save(buffer, format="PNG")
    return "data:image/png;base64," + base64.b64encode(buffer.getvalue()).decode("ascii")


def sheet():
    """Writes a contact sheet of every asset, source next to result, on a checkerboard so the
    transparency is visible. This is the review step for the art pass."""
    if not os.path.isdir(SRC):
        print("Leaf source not found at " + SRC, file=sys.stderr)
        return 1
    rows = []
    for name in sorted(ASSETS):
        source = load(name)
        generated = load(name, root=OUT)
        if source is None:
            rows.append("<tr><td colspan='6'>missing: %s</td></tr>" % name)
            continue
        written = "<em>not generated</em>"
        if generated is not None:
            written = "<img src='%s'>" % thumb(generated, 260)
        rows.append(
            "<tr><th>%s</th><td>%dx%d</td><td>%s</td><td class='rule'>%s</td><td>%s</td><td>%s</td></tr>"
            % (name, source.size[0], source.size[1], thumb(source, 260), ASSETS[name][0], written,
               thumb(source, 60)))
    template = """<!doctype html>
<meta charset="utf-8">
<title>Aether - Leaf art recolour sheet</title>
<style>
  body { background:#14151a; color:#e8e8ee; font:13px/1.5 -apple-system,Segoe UI,sans-serif; margin:24px; }
  h1 { font-size:18px; font-weight:600; margin:0 0 4px; }
  p { color:#9a9bab; margin:0 0 20px; }
  table { border-collapse:collapse; }
  th, td { padding:8px 12px; text-align:left; vertical-align:middle;
           border-bottom:1px solid #2a2c36; }
  th { font-weight:500; white-space:nowrap; font-size:12px; color:#c8c9d6; }
  td.rule { color:#8f90a0; font-family:ui-monospace,Consolas,monospace; font-size:11px; }
  /* Checkerboard: transparent pixels must not read as black, and the recoloured surfaces are black. */
  img { display:block; image-rendering:pixelated;
        background-image:linear-gradient(45deg,#5a5c68 25%,transparent 25%,transparent 75%,#5a5c68 75%),
                         linear-gradient(45deg,#5a5c68 25%,transparent 25%,transparent 75%,#5a5c68 75%);
        background-size:16px 16px; background-position:0 0,8px 8px; background-color:#9496a2; }
</style>
<h1>Leaf art recoloured to transparent black + white</h1>
<p>Left: the Leaf Client 1.8.9 source. Right: what Aether ships. Both on a checkerboard, because the
result is mostly transparent.</p>
<table>
<thead><tr><th>asset</th><th>size</th><th>source</th><th>rule</th><th>converted</th><th>alpha</th></tr></thead>
<tbody>
<!--ROWS-->
</tbody></table>
"""
    html = template.replace("<!--ROWS-->", "\n".join(rows))
    os.makedirs(os.path.dirname(SHEET), exist_ok=True)
    handle = open(SHEET, "w", encoding="utf-8")
    try:
        handle.write(html)
    finally:
        handle.close()
    print("wrote " + SHEET)
    return 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--analyze", action="store_true")
    parser.add_argument("--convert", action="store_true")
    parser.add_argument("--sheet", action="store_true")
    parser.add_argument("--ascii", metavar="NAME")
    args = parser.parse_args()
    if args.analyze:
        analyze()
    elif args.convert:
        return convert()
    elif args.sheet:
        return sheet()
    elif args.ascii:
        ascii_preview(args.ascii)
    else:
        parser.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
