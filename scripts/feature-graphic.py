#!/usr/bin/env python3
"""Renders the 1024x500 Google Play feature graphic for each store language.

Usage: scripts/feature-graphic.py            (needs rsvg-convert and the Inter,
       Inter Display and Noto Sans CJK SC fonts)

The layout mirrors .github/social-preview.svg: launcher glyph, app name, tagline,
three sample counters and the home screenshot from
fastlane/metadata/android/<lang>/images/phoneScreenshots/01-home.png. Output goes to
fastlane/metadata/android/<lang>/images/featureGraphic.png.
"""
import base64, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FG = os.path.join(ROOT, "app/src/main/res/drawable/ic_launcher_foreground.xml")
META = os.path.join(ROOT, "fastlane/metadata/android")

LANGS = {
    "en-US": dict(
        title="EventSince", title_font="Inter Display", sub=None,
        tagline="How long has it been since ...", tagline_font="Inter Display",
        line="Live counters, reset history with heatmaps, goal reminders,",
        line2="home-screen widgets, local and WebDAV backup.", line_font="Inter",
        chips=[("1d 06:20:20", "Morning run", "#FFD193", "#1A1A1A"),
               ("1y 3m 14d", "Started learning piano", "#D87B00", "#FFFFFF"),
               ("3d 06:44:20", "Watered the plants", "#26B9E7", "#FFFFFF")],
        chip_font="Inter",
    ),
    "zh-CN": dict(
        title="事起", title_font="Noto Sans CJK SC", sub="EventSince",
        tagline="距离上一次，已经过去了多久？", tagline_font="Noto Sans CJK SC",
        line="实时计时、重置历史与热力图、目标提醒、",
        line2="桌面小组件、本地与 WebDAV 备份。", line_font="Noto Sans CJK SC",
        chips=[("1天 06:12:04", "晨跑", "#FFD193", "#1A1A1A"),
               ("1年 3月 14天", "开始学钢琴", "#D87B00", "#FFFFFF"),
               ("3天 06:36:04", "给植物浇水", "#26B9E7", "#FFFFFF")],
        chip_font="Noto Sans CJK SC",
    ),
}

def glyph_paths():
    return re.findall(r'android:pathData="([^"]+)"', open(FG).read())

def svg(lang, cfg):
    shot = os.path.join(META, lang, "images/phoneScreenshots/01-home.png")
    b64 = base64.b64encode(open(shot, "rb").read()).decode()
    paths = "".join(f'<path fill="#FFFFFF" d="{p}"/>' for p in glyph_paths())
    out = [f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1024" height="500" viewBox="0 0 1024 500">
<defs>
  <clipPath id="phone"><rect x="724" y="72" width="250" height="440" rx="24"/></clipPath>
  <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#000" flood-opacity="0.18"/></filter>
</defs>
<rect width="1024" height="500" fill="#F8F8FC"/>
<rect x="64" y="72" width="96" height="96" rx="22" fill="#000000"/>
<g transform="translate(64,72) scale({96/108}) translate(17.34,13.44) scale(0.78)">{paths}</g>
<text x="184" y="138" font-family="{cfg['title_font']}" font-weight="700" font-size="56" fill="#1C1B1F">{cfg['title']}</text>''']
    if cfg["sub"]:
        out.append(f'<text x="318" y="138" font-family="Inter Display" font-weight="500" font-size="30" fill="#5C5F66">{cfg["sub"]}</text>')
    out.append(f'''<text x="64" y="222" font-family="{cfg['tagline_font']}" font-weight="600" font-size="31" fill="#1C1B1F">{cfg['tagline']}</text>
<text x="64" y="258" font-family="{cfg['line_font']}" font-weight="400" font-size="19" fill="#5C5F66">{cfg['line']}</text>
<text x="64" y="286" font-family="{cfg['line_font']}" font-weight="400" font-size="19" fill="#5C5F66">{cfg['line2']}</text>''')
    y = 316
    for value, label, bg, fg in cfg["chips"]:
        w = 18 + int(sum(20 if ord(c) > 0x2E80 else 12.2 for c in value))
        out.append(f'<rect x="64" y="{y}" width="{w}" height="40" rx="11" fill="{bg}"/>'
                   f'<text x="{64 + w/2}" y="{y+27}" font-family="{cfg["chip_font"]}" font-weight="700" font-size="20" text-anchor="middle" fill="{fg}">{value}</text>'
                   f'<text x="{64 + w + 16}" y="{y+27}" font-family="{cfg["chip_font"]}" font-weight="500" font-size="22" fill="#1C1B1F">{label}</text>')
        y += 52
    out.append(f'''<g filter="url(#shadow)"><rect x="724" y="72" width="250" height="440" rx="24" fill="#FFFFFF"/></g>
<image x="724" y="72" width="250" height="444.4" clip-path="url(#phone)" preserveAspectRatio="xMinYMin" xlink:href="data:image/png;base64,{b64}"/>
</svg>
''')
    return "\n".join(out)

for lang, cfg in LANGS.items():
    src = os.path.join(META, lang, "images/featureGraphic.svg")
    open(src, "w").write(svg(lang, cfg))
    dst = os.path.join(META, lang, "images/featureGraphic.png")
    subprocess.run(["rsvg-convert", "-w", "1024", "-h", "500", src, "-o", dst], check=True)
    os.remove(src)
    print(dst)
