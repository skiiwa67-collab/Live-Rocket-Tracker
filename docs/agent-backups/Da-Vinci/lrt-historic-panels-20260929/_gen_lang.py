#!/usr/bin/env python3
"""Settings skins with agency language + marks. Drafts only. Not wired."""
from PIL import Image, ImageDraw, ImageFont
import math, os

OUT = "/workspace/agent-backups/Da-Vinci/lrt-historic-panels-20260929/skins-lang"
os.makedirs(OUT, exist_ok=True)
W = 480
CJK = "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
CJKB = "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
HI = "/usr/share/fonts/truetype/sand-box/google/Noto Sans Devanagari/NotoSansDevanagari-VariableFont_wdth,wght.ttf"
EN = "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"
ENR = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf"

def font(path, size, index=0):
    return ImageFont.truetype(path, size, index=index)

def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i+2], 16) for i in (0, 2, 4))

ENT_TITLE = "ENTERTAINMENT ONLY"
ENT_BODY = ("We sync launches as close to real time as public sources allow, "
            "but no rocket company provides exact live telemetry. Delays and holds "
            "can make this display differ from the actual launch. Confirm with the official stream.")
HIST_TITLE = "HISTORIC DATA"
HIST_BODY = ("Historic profiles use public reports. We add anomalies when we can find them, "
             "without digging into private systems. A monthly data push updates past launches "
             "as better public data turns up. A real-time failure or anomaly may not show "
             "correctly until that data push.")

# local lines follow the app's existing helpers where those words exist
# (zhFor / ruFor / frFor). Settings words those helpers do not cover use the same
# short bilingual shape: English on top, agency language under.
LANG = {
    "zh": {
        "script": "cjk", "idx": 2,
        "agency": "中国航天",
        "LIVE": "实时", "HISTORIC": "历史", "CURRENT": "当前",
        "SEARCH HISTORIC": "搜索历史", "REFRESH": "刷新",
        "FOLLOW": "跟踪", "AUTO ON": "自动", "MANUAL": "手动",
        "UNITS": "单位", "HOME SCREENS": "主屏", "TRACKING": "跟踪",
        "UPCOMING WINDOW": "即将窗口",
        "1 WK": "1周", "1 MO": "1月", "6 MO": "6月",
    },
    "ru": {
        "script": "en",
        "agency": "РОСКОСМОС",
        "LIVE": "ПРЯМОЙ", "HISTORIC": "ИСТОРИЯ", "CURRENT": "ТЕКУЩИЙ",
        "SEARCH HISTORIC": "ПОИСК", "REFRESH": "ОБНОВИТЬ",
        "FOLLOW": "СЛЕЖЕНИЕ", "AUTO ON": "АВТО", "MANUAL": "РУЧНОЙ",
        "UNITS": "ЕДИНИЦЫ", "HOME SCREENS": "ЭКРАНЫ", "TRACKING": "СЛЕЖЕНИЕ",
        "UPCOMING WINDOW": "ОКНО",
        "1 WK": "1 НЕД", "1 MO": "1 МЕС", "6 MO": "6 МЕС",
    },
    "fr": {
        "script": "en",
        "agency": "ESA",
        "LIVE": "DIRECT", "HISTORIC": "HISTORIQUE", "CURRENT": "ACTUEL",
        "SEARCH HISTORIC": "RECHERCHE", "REFRESH": "ACTUALISER",
        "FOLLOW": "SUIVI", "AUTO ON": "AUTO", "MANUAL": "MANUEL",
        "UNITS": "UNITÉS", "HOME SCREENS": "ÉCRANS", "TRACKING": "SUIVI",
        "UPCOMING WINDOW": "FENÊTRE",
        "1 WK": "1 SEM", "1 MO": "1 MOIS", "6 MO": "6 MOIS",
    },
    "ja": {
        "script": "cjk", "idx": 0,
        "agency": "宇宙航空",
        "LIVE": "ライブ", "HISTORIC": "履歴", "CURRENT": "現在",
        "SEARCH HISTORIC": "検索", "REFRESH": "更新",
        "FOLLOW": "追尾", "AUTO ON": "自動", "MANUAL": "手動",
        "UNITS": "単位", "HOME SCREENS": "画面", "TRACKING": "追跡",
        "UPCOMING WINDOW": "予定枠",
        "1 WK": "1週", "1 MO": "1月", "6 MO": "6月",
    },
    "hi": {
        "script": "hi",
        "agency": "इसरो",
        "LIVE": "लाइव", "HISTORIC": "इतिहास", "CURRENT": "वर्तमान",
        "SEARCH HISTORIC": "खोज", "REFRESH": "ताज़ा",
        "FOLLOW": "अनुसरण", "AUTO ON": "स्वतः", "MANUAL": "मैनुअल",
        "UNITS": "इकाई", "HOME SCREENS": "स्क्रीन", "TRACKING": "ट्रैक",
        "UPCOMING WINDOW": "आगामी",
        "1 WK": "1 सप्ताह", "1 MO": "1 माह", "6 MO": "6 माह",
    },
}

SKINS = [
    dict(id="spacex", name="SPACEX", lang=None, logo="spacex",
         bg="#050505", panel="#121212", accent="#FFFFFF", title="#FFFFFF",
         text="#F0F0F0", muted="#8A8A8A", idle="#0C0C0C", active="#1A1A1A",
         idle_s="#3A3A3A", active_s="#FFFFFF", t_idle="#8A8A8A", t_on="#FFFFFF",
         lamp="#00D26A", local="#BBBBBB"),
    dict(id="nasa", name="NASA", lang=None, logo="nasa",
         bg="#0A0E18", panel="#101828", accent="#0032A0", title="#8EB4F0",
         text="#E8EEF8", muted="#7A8AA0", idle="#101828", active="#1A2840",
         idle_s="#2A3A55", active_s="#0032A0", t_idle="#7A8AA0", t_on="#E8EEF8",
         lamp="#C8102E", local="#8EB4F0"),
    dict(id="chinese", name="CASC", lang="zh", logo="casc",
         bg="#0C0808", panel="#180E0E", accent="#DE2910", title="#DE2910",
         text="#F5E6C8", muted="#A08060", idle="#140A0A", active="#2A1010",
         idle_s="#5A2020", active_s="#DE2910", t_idle="#A08060", t_on="#F5E6C8",
         lamp="#FFD700", local="#FFD700"),
    dict(id="roscosmos", name="РОСКОСМОС", lang="ru", logo="ros",
         bg="#080A0C", panel="#12161C", accent="#E8C547", title="#E8C547",
         text="#E8E0D0", muted="#8A8580", idle="#101418", active="#1C242C",
         idle_s="#3A4048", active_s="#E8C547", t_idle="#8A8580", t_on="#F0E8D0",
         lamp="#C41E3A", local="#E8C547"),
    dict(id="blue", name="BLUE ORIGIN", lang=None, logo="blue",
         bg="#061018", panel="#0C1A28", accent="#3D9BE9", title="#3D9BE9",
         text="#E8F4FF", muted="#7A98A8", idle="#0A1824", active="#123044",
         idle_s="#2A5068", active_s="#3D9BE9", t_idle="#7A98A8", t_on="#E8F4FF",
         lamp="#C4A35A", local="#C4A35A"),
    dict(id="esa", name="ESA / ARIANESPACE", lang="fr", logo="esa",
         bg="#070B18", panel="#0E1630", accent="#FFD100", title="#FFD100",
         text="#F0F4FF", muted="#8A98C0", idle="#0C1428", active="#183060",
         idle_s="#003399", active_s="#FFD100", t_idle="#8A98C0", t_on="#FFFFFF",
         lamp="#FFD100", local="#FFD100"),
    dict(id="rocketlab", name="ROCKET LAB", lang=None, logo="rl",
         bg="#080808", panel="#141414", accent="#FF5A1F", title="#FF5A1F",
         text="#F2F2F2", muted="#8A8A8A", idle="#101010", active="#1C1C1C",
         idle_s="#3A3A3A", active_s="#FF5A1F", t_idle="#8A8A8A", t_on="#FFFFFF",
         lamp="#FF5A1F", local="#FF5A1F"),
    dict(id="isro", name="ISRO", lang="hi", logo="isro",
         bg="#0A0806", panel="#18140E", accent="#FF9933", title="#FF9933",
         text="#F5F0E6", muted="#A09070", idle="#14100C", active="#241810",
         idle_s="#4A3820", active_s="#FF9933", t_idle="#A09070", t_on="#F5F0E6",
         lamp="#138808", local="#FF9933"),
    dict(id="jaxa", name="JAXA", lang="ja", logo="jaxa",
         bg="#0C0808", panel="#1A0E0E", accent="#BC002D", title="#E85A78",
         text="#F8F0F0", muted="#A08080", idle="#140A0A", active="#281010",
         idle_s="#5A2028", active_s="#BC002D", t_idle="#A08080", t_on="#F8F0F0",
         lamp="#BC002D", local="#F0C0C8"),
    dict(id="generic", name="MISSION", lang=None, logo="generic",
         bg="#06080C", panel="#0E1620", accent="#00C8FF", title="#00C8FF",
         text="#D0E0F0", muted="#6A8090", idle="#0A1018", active="#122030",
         idle_s="#2A3A4A", active_s="#00C8FF", t_idle="#6A8090", t_on="#D0E0F0",
         lamp="#00C8FF", local="#00C8FF"),
]

def loc_font(lang, size, bold=False):
    if not lang:
        return font(EN if bold else ENR, size)
    spec = LANG[lang]
    if spec["script"] == "cjk":
        return font(CJKB if bold else CJK, size, index=spec["idx"])
    if spec["script"] == "hi":
        return font(HI, size)
    return font(EN if bold else ENR, size)

def wrap(draw, text, f, width):
    words = text.split()
    lines, cur = [], ""
    for w in words:
        trial = (cur + " " + w).strip()
        if draw.textlength(trial, font=f) <= width:
            cur = trial
        else:
            if cur:
                lines.append(cur)
            cur = w
    if cur:
        lines.append(cur)
    return lines

def screw(draw, x, y, skin):
    r = 5
    draw.ellipse((x-r, y-r, x+r, y+r), fill=hexc("#3A3A3A"), outline=hexc(skin["accent"]))
    draw.line((x-3, y, x+3, y), fill=hexc("#111111"), width=1)
    draw.line((x, y-3, x, y+3), fill=hexc("#111111"), width=1)

def ticks(draw, x0, y0, x1, y1, col):
    L = 10
    c = hexc(col)
    for (xa, ya, xb, yb) in (
        (x0, y0, x0+L, y0), (x0, y0, x0, y0+L),
        (x1, y0, x1-L, y0), (x1, y0, x1, y0+L),
        (x0, y1, x0+L, y1), (x0, y1, x0, y1-L),
        (x1, y1, x1-L, y1), (x1, y1, x1, y1-L),
    ):
        draw.line((xa, ya, xb, yb), fill=c, width=2)

def logo(draw, kind, cx, cy, s):
    if kind == "spacex":
        draw.line((cx-s, cy-s*0.85, cx+s, cy+s*0.85), fill=(255,255,255), width=max(2, int(s*0.28)))
        draw.line((cx+s, cy-s*0.85, cx-s, cy+s*0.85), fill=(255,255,255), width=max(2, int(s*0.28)))
    elif kind == "nasa":
        draw.ellipse((cx-s, cy-s, cx+s, cy+s), fill=(0, 50, 160), outline=(240,245,255))
        draw.arc((cx-s*0.72, cy-s*0.28, cx+s*0.72, cy+s*0.72), 200, 340, fill=(200, 16, 46), width=max(2, int(s*0.22)))
    elif kind == "casc":
        for i, rr in enumerate((s*0.45, s*0.72, s*1.0)):
            draw.ellipse((cx-rr, cy-rr, cx+rr, cy+rr), outline=(222, 41, 16))
        draw.polygon([(cx, cy-s*0.85), (cx+s*0.22, cy-s*0.2), (cx-s*0.22, cy-s*0.2)], fill=(255, 215, 0))
        draw.rectangle((cx-s*0.08, cy-s*0.2, cx+s*0.08, cy+s*0.7), fill=(222, 41, 16))
    elif kind == "ros":
        pts = []
        for i in range(10):
            ang = -math.pi/2 + i * math.pi/5
            rad = s if i % 2 == 0 else s*0.42
            pts.append((cx + rad*math.cos(ang), cy + rad*math.sin(ang)))
        draw.polygon(pts, fill=(196, 30, 58), outline=(232, 197, 71))
    elif kind == "blue":
        # feather: a narrow leaf, the mark Blue Origin uses, drawn simply
        draw.polygon([(cx, cy-s), (cx+s*0.45, cy+s*0.15), (cx, cy+s*0.85), (cx-s*0.45, cy+s*0.15)], fill=(61, 155, 233))
        draw.line((cx, cy-s*0.7, cx, cy+s*0.6), fill=(6, 16, 24), width=1)
    elif kind == "esa":
        draw.ellipse((cx-s, cy-s, cx+s, cy+s), fill=(0, 51, 153), outline=(200, 220, 255))
        f = font(EN, int(s*1.15))
        draw.text((cx, cy), "e", font=f, fill=(255,255,255), anchor="mm")
    elif kind == "rl":
        draw.ellipse((cx-s, cy-s, cx+s, cy+s), fill=(12,12,12), outline=(255, 90, 31))
        draw.polygon([(cx, cy-s*0.55), (cx+s*0.28, cy+s*0.45), (cx-s*0.28, cy+s*0.45)], fill=(255,255,255))
    elif kind == "isro":
        draw.arc((cx-s, cy-s*0.2, cx+s, cy+s*0.9), 200, 340, fill=(255, 153, 51), width=max(2, int(s*0.28)))
        draw.polygon([(cx, cy-s*0.9), (cx+s*0.28, cy+s*0.15), (cx-s*0.28, cy+s*0.15)], fill=(255, 153, 51))
        draw.ellipse((cx-s*0.16, cy+s*0.35, cx+s*0.16, cy+s*0.67), fill=(19, 136, 8))
    elif kind == "jaxa":
        draw.ellipse((cx-s, cy-s, cx+s, cy+s), fill=(188, 0, 45))
        draw.arc((cx-s*0.62, cy-s*0.62, cx+s*0.62, cy+s*0.62), 300, 200, fill=(255,255,255), width=max(2, int(s*0.18)))
        draw.ellipse((cx+s*0.15, cy-s*0.55, cx+s*0.42, cy-s*0.28), fill=(255,255,255))
    else:
        draw.polygon([(cx, cy-s), (cx+s*0.7, cy), (cx, cy+s), (cx-s*0.7, cy)], outline=(0, 200, 255))

def gold_box(draw, x, y, w, title, body):
    f_t = font(EN, 16)
    f_b = font(EN, 13)
    lines = wrap(draw, body, f_b, w - 24)
    h = 14 + 20 + 6 + len(lines) * 17 + 12
    draw.rounded_rectangle((x, y, x+w, y+h), radius=8, fill=hexc("#2A2208"), outline=hexc("#F2C14E"), width=2)
    draw.text((x+12, y+10), title, font=f_t, fill=hexc("#F2C14E"))
    yy = y + 34
    for ln in lines:
        draw.text((x+12, yy), ln, font=f_b, fill=hexc("#FFE8A0"))
        yy += 17
    return h

def button(draw, x, y, w, h, en, local, on, skin, lang):
    fill = hexc(skin["active"] if on else skin["idle"])
    stroke = hexc(skin["active_s"] if on else skin["idle_s"])
    draw.rounded_rectangle((x, y, x+w, y+h), radius=6, fill=fill, outline=stroke, width=2)
    # lamp
    lamp = hexc(skin["lamp"] if on else "#2A2A2A")
    draw.ellipse((x+12, y+h/2-5, x+22, y+h/2+5), fill=lamp)
    logo(draw, skin["logo"], x+40, y+h/2, 9)
    tc = hexc(skin["t_on"] if on else skin["t_idle"])
    show_local = bool(local) and local != en
    f_en = font(EN, 15 if show_local else 16)
    if show_local:
        draw.text((x+w/2+16, y+h*0.36), en, font=f_en, fill=tc, anchor="mm")
        f_l = loc_font(lang, 13)
        draw.text((x+w/2+16, y+h*0.70), local, font=f_l, fill=hexc(skin["local"] if on else skin["muted"]), anchor="mm")
    else:
        draw.text((x+w/2+16, y+h/2), en, font=f_en, fill=tc, anchor="mm")
    return h

def section_label(draw, x, y, en, local, skin, lang):
    f = font(EN, 13)
    draw.text((x, y), en, font=f, fill=hexc(skin["title"]))
    if local and local != en:
        tw = draw.textlength(en, font=f)
        f_l = loc_font(lang, 13)
        draw.text((x+tw+8, y), local, font=f_l, fill=hexc(skin["local"]))
    return 18

def render(skin):
    lang = skin["lang"]
    L = LANG.get(lang, {})
    im = Image.new("RGB", (W, 1700), hexc(skin["bg"]))
    d = ImageDraw.Draw(im)
    x = 16
    y = 18
    # header mark + name
    logo(d, skin["logo"], x+16, y+16, 14)
    f_title = font(EN, 20)
    d.text((x+40, y), "LIVE ROCKET TRACKER", font=f_title, fill=hexc("#F4EDE0"))
    y += 28
    f_name = font(EN, 14)
    d.text((x+40, y), skin["name"], font=f_name, fill=hexc(skin["title"]))
    agency = L.get("agency")
    if agency and agency not in skin["name"]:
        tw = d.textlength(skin["name"], font=f_name)
        f_a = loc_font(lang, 14, bold=True)
        d.text((x+44+tw, y), agency, font=f_a, fill=hexc(skin["local"]))
    y += 28
    h = gold_box(d, x, y, W-32, ENT_TITLE, ENT_BODY)
    y += h + 10
    f_sub = font(ENR, 12)
    d.text((x, y), "Command Center  |  next launch on your home screen", font=f_sub, fill=hexc(skin["muted"]))
    y += 22
    section_label(d, x, y, "TRACKING", L.get("TRACKING"), skin, lang)
    y += 18
    f_body = font(ENR, 12)
    d.text((x, y), "Current follows the next real launch. Historic is demos and past flights.", font=f_body, fill=hexc(skin["muted"]))
    y += 22

    # LIVE panel — historic is selected, so the upcoming-window row stays hidden
    live_top = y
    y += 12
    section_label(d, x+12, y, "LIVE", L.get("LIVE"), skin, lang)
    y += 22
    bh = 52 if lang else 40
    button(d, x+12, y, W-56, bh, "CURRENT", L.get("CURRENT"), False, skin, lang)
    y += bh + 12
    live_bot = y
    d.rounded_rectangle((x, live_top, W-16, live_bot), radius=8, outline=hexc(skin["accent"]), width=2)
    ticks(d, x, live_top, W-16, live_bot, skin["accent"])
    for sx, sy in ((x+8, live_top+8), (W-24, live_top+8), (x+8, live_bot-8), (W-24, live_bot-8)):
        screw(d, sx, sy, skin)
    y = live_bot + 12

    hist_top = y
    y += 12
    section_label(d, x+12, y, "HISTORIC", L.get("HISTORIC"), skin, lang)
    y += 22
    button(d, x+12, y, W-56, bh, "HISTORIC", L.get("HISTORIC"), True, skin, lang)
    y += bh + 10
    section_label(d, x+12, y, "SEARCH HISTORIC", L.get("SEARCH HISTORIC"), skin, lang)
    y += 20
    d.rounded_rectangle((x+12, y, W-28, y+36), radius=6, outline=hexc(skin["idle_s"]), width=1)
    d.text((x+22, y+10), "Falcon, RUD, Electron, SLC-40", font=font(ENR, 13), fill=hexc(skin["muted"]))
    y += 44
    d.rounded_rectangle((x+12, y, W-28, y+36), radius=6, outline=hexc(skin["idle_s"]), width=1)
    d.text((x+22, y+10), "Past flights", font=font(ENR, 13), fill=hexc(skin["text"]))
    y += 46
    hb = gold_box(d, x+12, y, W-56, HIST_TITLE, HIST_BODY)
    y += hb + 12
    hist_bot = y
    d.rounded_rectangle((x, hist_top, W-16, hist_bot), radius=8, outline=hexc(skin["accent"]), width=2)
    ticks(d, x, hist_top, W-16, hist_bot, skin["accent"])
    for sx, sy in ((x+8, hist_top+8), (W-24, hist_top+8), (x+8, hist_bot-8), (W-24, hist_bot-8)):
        screw(d, sx, sy, skin)
    y = hist_bot + 14

    rw = 240 if lang else 160
    button(d, x, y, rw, bh if lang else 40, "REFRESH", L.get("REFRESH"), False, skin, lang)
    d.text((x+rw+12, y+14), "Idle. Not fetched yet", font=font(ENR, 12), fill=hexc(skin["muted"]))
    y += 52
    section_label(d, x, y, "FOLLOW", L.get("FOLLOW"), skin, lang)
    y += 22
    gap = 10
    bw = (W - 32 - gap) // 2
    button(d, x, y, bw, bh, "AUTO ON", L.get("AUTO ON"), True, skin, lang)
    button(d, x+bw+gap, y, bw, bh, "MANUAL", L.get("MANUAL"), False, skin, lang)
    y += bh + 12
    section_label(d, x, y, "UNITS", L.get("UNITS"), skin, lang)
    y += 22
    button(d, x, y, bw, 40, "MPH (US)", None, True, skin, lang)
    button(d, x+bw+gap, y, bw, 40, "KM/H", None, False, skin, lang)
    y += 54
    section_label(d, x, y, "HOME SCREENS", L.get("HOME SCREENS"), skin, lang)
    y += 28
    crop = im.crop((0, 0, W, y))
    path = os.path.join(OUT, skin["id"] + ".png")
    crop.save(path, "PNG")
    return path, crop.size

if __name__ == "__main__":
    for s in SKINS:
        p, sz = render(s)
        print(p, sz)
