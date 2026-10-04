"""课前一眼 · 全书总览：把 kyoka_books.json / katsuyou_lessons.json 里每课的 peek 照 App 的版式画成画布画板。
一本书一块画板，每课一个 390×844 手机屏；屏上方标出自查发现的问题（规则太长、上排太挤……）。
用法：python gen_peeks.py <canvas 根目录>   （会写 project/PeekAll_*.dc.html 并更新 project/canvas.json）
"""
import html, json, math, pathlib, re, sys
from datetime import datetime, timezone

APP = pathlib.Path(r'C:\Users\汪家俊\jps\android-app\app\src\main\assets')
ROOT = pathlib.Path(sys.argv[1])
PROJ = ROOT / 'project'

LIGHT = ('--bg:#F7F7F4;--surface:#FFFFFF;--sunken:#EFEEE9;--line:#E4E3DD;--line2:#D3D2CB;--ink:#1B1B19;--ink2:#55544F;--ink3:#6E6D67;'
         '--work:#6A4FC4;--tone:rgba(106,79,196,0.22);--info:#2F6A9E;--bad:#B8432F;--ok:#2C7A55')
SEG = {'insert': 'var(--ok)', 'gem': 'var(--ok)', 'voiced': 'var(--work)', 'vowel': 'var(--info)', 'gone': 'var(--ink3)', 'bad': 'var(--bad)'}

CSS = '''body{margin:0;background:#E9E8E3}
.ph{width:390px;height:844px;background:var(--bg);color:var(--ink);display:flex;flex-direction:column;overflow:hidden;border:1px solid var(--line2);box-sizing:border-box;font-family:'Noto Sans SC',sans-serif}
.hd{height:64px;display:flex;align-items:center;gap:6px;padding:0 12px 0 6px;flex-shrink:0}
.x{width:44px;height:44px;display:flex;align-items:center;justify-content:center;color:var(--ink)}
.eb{font:400 11px/16px 'IBM Plex Mono',monospace;letter-spacing:.04em;color:var(--ink3);white-space:nowrap;overflow:hidden}
.tt{font:700 17px/22px 'Noto Serif JP',serif;white-space:nowrap;overflow:hidden}
.pl{margin:0 20px;height:2px;background:var(--line);flex-shrink:0}
.mn{flex:1;min-height:0;overflow:hidden;padding:24px 20px 32px;display:flex;flex-direction:column}
.lb{font:400 11px/16px 'IBM Plex Mono',monospace;letter-spacing:.06em;color:var(--ink3)}
.pn{position:relative;border:1.5px solid var(--ink);border-radius:4px;background:var(--surface);overflow:hidden}
.tn{position:absolute;width:170px;height:70px;transform:rotate(-12deg);background-image:radial-gradient(var(--tone) 1.2px,transparent 1.6px);background-size:7px 7px}
.tile{min-width:40px;height:44px;padding:0 6px;box-sizing:border-box;border:1.5px solid var(--ink);border-radius:4px;display:flex;align-items:center;justify-content:center;font:700 22px 'Noto Serif JP',serif;white-space:nowrap}
.jp{font-family:'Noto Serif JP',serif}
.ro{font:400 10px/15px 'IBM Plex Mono',monospace;letter-spacing:.02em;color:var(--ink3);white-space:nowrap}
.q{font-family:'Noto Serif JP',serif;font-weight:600;color:var(--ink)}
.bt{margin:0 20px 20px;height:64px;border-radius:10px;background:var(--ink);color:var(--bg);display:flex;align-items:center;padding:0 20px;flex-shrink:0;justify-content:space-between}
.flag{font:500 12px/18px 'Noto Sans SC',sans-serif;color:var(--bad);min-height:18px}
.ok{color:var(--ok)}
.hr{height:1px;background:var(--line)}'''

X_SVG = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18"></path></svg>'
ARROW = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12h14M13 6l6 6-6 6"></path></svg>'


def e(s):
    return html.escape(s, quote=True)


def note(s):
    """NoteText：「」里衬线加粗，**…** 加粗，→ 作品色。"""
    out, i = [], 0
    while i < len(s):
        if s.startswith('**', i) and s.find('**', i + 2) > i + 2:
            j = s.find('**', i + 2)
            out.append(f'<b style="color: var(--ink)">{e(s[i + 2:j])}</b>')
            i = j + 2
        elif s[i] in '「『' and s.find('」' if s[i] == '「' else '』', i + 1) > i:
            c = '」' if s[i] == '「' else '』'
            j = s.find(c, i + 1)
            out.append(f'{s[i]}<span class="q" lang="ja">{e(s[i + 1:j])}</span>{c}')
            i = j + 1
        elif s[i] in '→←':
            out.append(f'<span style="color: var(--work); font-weight: 600">{s[i]}</span>')
            i += 1
        else:
            out.append(e(s[i]))
            i += 1
    return ''.join(out)


def segs(lst, size):
    parts = []
    for t, k in lst:
        c = SEG.get(k)
        parts.append(f'<span style="color: {c}">{e(t)}</span>' if c and k != 'already' else e(t))
    return f'<span class="jp" lang="ja" style="font-weight: 700; font-size: {size}px; line-height: {round(size * 1.3)}px">{"".join(parts)}</span>'


def phone(eyebrow, title, body, go, count):
    return (f'<div class="ph"><div class="hd"><span class="x">{X_SVG}</span><div style="flex-grow: 1; min-width: 0; display: flex; flex-direction: column">'
            f'<span class="eb">{e(eyebrow)}</span><span class="tt" lang="ja">{e(title)}</span></div></div><div class="pl"></div>'
            f'<div class="mn">{body}</div>'
            f'<div class="bt"><div style="display: flex; flex-direction: column"><span style="font-size: 16px; font-weight: 600">{e(go)}</span>'
            f'<span style="font: 400 11px \'IBM Plex Mono\', monospace; opacity: .7">{count} 个</span></div>{ARROW}</div></div>')


def old_peek(p):
    tiles = ''.join(f'<span class="tile" lang="ja">{e(t)}</span>' for t in p['ends'])
    exs = []
    for x in p['examples']:
        exs.append('<div style="display: flex; align-items: flex-end; gap: 12px; min-height: 60px; padding: 8px 0; box-sizing: border-box">'
                   f'<span class="jp" lang="ja" style="font-size: 17px; line-height: 24px; color: var(--ink3); min-width: 64px; white-space: nowrap">{e(x["base"])}</span>'
                   '<span style="color: var(--ink3); font: 11px/24px \'IBM Plex Mono\', monospace">→</span>'
                   f'<span style="display: flex; flex-direction: column"><span class="ro" style="font-size: 11px; line-height: 16px">{e(x["ro"])}</span>'
                   f'{segs([[x["stem"], "same"]] + x["tail"], 19)}</span></div><div class="hr"></div>')
    return ('<span class="lb">課前の一眼</span><div style="height: 20px"></div>'
            '<div class="pn" style="padding: 14px 0; display: flex; flex-direction: column; align-items: center">'
            '<span class="tn" style="right: -36px; top: -20px"></span>'
            f'<div style="position: relative; display: flex; align-items: center; gap: 12px; padding-top: 8px; white-space: nowrap">'
            f'<span style="display: flex; gap: 6px">{tiles}</span><span style="color: var(--ink3)">→</span>{segs(p["result"], 30)}</div>'
            f'<p style="position: relative; margin: 16px 16px 8px; font-size: 15px; line-height: 25px; letter-spacing: .02em; color: var(--ink)">{note(p["rule"])}</p></div>'
            f'<div style="height: 20px"></div><div>{"".join(exs)}</div>')


def gl_line(lst, size, quiet=False, center=False):
    ro = 11 if size >= 22 else 10
    cells = []
    for t, r, k in lst:
        col = {'insert': 'var(--ok)', 'mark': 'var(--work)', 'gone': 'var(--ink3)'}.get(k, 'var(--ink2)' if quiet else 'var(--ink)')
        und = k in ('insert', 'mark')
        w = 500 if quiet else (900 if und else 700)
        deco = f'border-bottom: 2px solid {col};' if und else ('text-decoration: line-through;' if k == 'gone' else '')
        bg = 'background: var(--sunken); border-radius: 2px; padding: 0 3px;' if k == 'scope' else ''
        cells.append(f'<span style="display: flex; flex-direction: column; align-items: center; {bg}">'
                     f'<span class="ro" style="font-size: {ro}px; line-height: {ro + 5}px; color: {col if und else "var(--ink3)"}">{e(r)}</span>'
                     f'<span class="jp" lang="ja" style="font-size: {size}px; line-height: {round(size * 1.4)}px; font-weight: {w}; color: {col}; {deco} letter-spacing: {".03em" if size <= 20 else "0"}">{e(t)}</span></span>')
    jc = 'center' if center else 'flex-start'
    return f'<div style="display: flex; flex-wrap: wrap; align-items: flex-end; justify-content: {jc}; gap: 8px 3px">{"".join(cells)}</div>'


def hero_size(lst):
    n = sum(len(t) for t, _, _ in lst)
    return 32 if n <= 6 else 26 if n <= 10 else 22 if n <= 16 else 20


def glance(p):
    g = p['glance']
    h = g['hero']
    if h['to']:
        inner = (gl_line(h['from'], max(17, round(hero_size(h['from']) * .72)), quiet=True, center=True)
                 + '<span style="color: var(--ink3); font: 16px \'IBM Plex Mono\', monospace; padding: 8px 0">↓</span>'
                 + gl_line(h['to'], hero_size(h['to']), center=True))
    else:
        inner = gl_line(h['from'], hero_size(h['from']), center=True)
    if h['zh']:
        inner += f'<p style="margin: 16px 0 0; font-size: 14px; line-height: 22px; letter-spacing: .02em; color: var(--ink2); text-align: center">{note(h["zh"])}</p>'
    beats = ''.join(f'<div style="display: flex; gap: 12px"><span style="font: 12px/25px \'IBM Plex Mono\', monospace; color: var(--work); width: 12px; flex-shrink: 0">{i + 1}</span>'
                    f'<span style="font-size: 15px; line-height: 25px; letter-spacing: .02em; color: var(--ink2)">{note(b)}</span></div>' for i, b in enumerate(g['beats']))
    pairs = ''
    if g['pairs']:
        n = len(g['pairs'])
        pairs = f'<div style="height: 32px"></div><span class="lb">再看{" " + str(n) + " 个" if n > 1 else "一个"}</span><div style="height: 4px"></div>'
        for q in g['pairs']:
            row = gl_line(q['from'], 18, quiet=bool(q['to']))
            if q['to']:
                row += ('<div style="display: flex; align-items: flex-end"><span style="color: var(--ink3); font: 14px \'IBM Plex Mono\', monospace; padding: 0 8px 6px 0">→</span>'
                        + gl_line(q['to'], 20) + '</div>')
            if q['zh']:
                row += f'<span style="font-size: 13px; line-height: 20px; color: var(--ink3)">{note(q["zh"])}</span>'
            pairs += f'<div style="display: flex; flex-direction: column; gap: 8px; padding: 16px 0">{row}</div><div class="hr"></div>'
    return ('<span class="lb">課前の一眼 · 10 秒</span><div style="height: 16px"></div>'
            '<div class="pn"><span class="tn" style="left: -28px; bottom: -26px; width: 150px; height: 50px"></span>'
            f'<div style="position: relative; padding: 24px 16px; display: flex; flex-direction: column; align-items: center">{inner}</div></div>'
            f'<div style="height: 32px"></div><div style="display: flex; flex-direction: column; gap: 12px">{beats}</div>{pairs}')


def flags(p):
    if p.get('glance'):
        return []
    out = []
    n = len(p['rule'])
    if n > 60:
        out.append(f'规则 {n} 字（一眼 ≤ 40）')
    top = sum(len(t) for t in p['ends']) * 22 + len(p['ends']) * 18 + sum(len(t) for t, _ in p['result']) * 30 + 40
    if top > 330:
        out.append('上排放不下')
    if any(len(x['base']) > 5 for x in p['examples']):
        out.append('例句左栏偏长')
    if not p['examples']:
        out.append('没有例句')
    return out


def first_count(L):
    st = L['steps'][0]
    return (st.get('stack') and len(st['stack']['goals'])) or (st.get('connect') and len(st['connect']['left'])) or len(st.get('items', []))


def board(name, title, sub, screens):
    cols = min(6, max(1, len(screens)))
    rows = math.ceil(len(screens) / cols)
    w = 48 * 2 + cols * 390 + (cols - 1) * 32
    h = 48 + 56 + 24 + rows * (844 + 30) + (rows - 1) * 40 + 48
    nflag = sum(1 for _, f in screens if f)
    cells = ''.join(f'<div style="display: flex; flex-direction: column; gap: 12px"><span class="flag">{e(" · ".join(f)) if f else "<span class=ok>✓</span>"}</span>{s}</div>'
                    for s, f in screens)
    src = f'''<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<title>{e(title)}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@400;500&amp;family=Noto+Sans+SC:wght@400;500;600;700&amp;family=Noto+Serif+JP:wght@400;500;600;700;900&amp;display=swap" rel="stylesheet">
<style>
{CSS}
</style>
</helmet>
<div style="width: {w}px; height: {h}px; box-sizing: border-box; padding: 48px; background: #E9E8E3; font-family: 'Noto Sans SC', sans-serif; color: #1B1B19; {LIGHT}">
<div style="height: 56px; display: flex; align-items: baseline; gap: 16px; border-bottom: 1.5px solid var(--ink)"><span style="font-size: 28px; line-height: 40px; font-weight: 700">{e(title)}</span><span style="font: 13px 'IBM Plex Mono', monospace; color: var(--ink3)">{e(sub)} · 有问题的 {nflag} / {len(screens)}</span></div>
<div style="height: 24px"></div>
<div style="display: grid; grid-template-columns: repeat({cols}, 390px); gap: 40px 32px">{cells}</div>
</div>
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{{"$preview":{{"width":{w},"height":{h}}}}}'>
class Component extends DCLogic {{
  renderVals() {{ return {{}}; }}
}}
</script>
</body>
</html>
'''
    (PROJ / name).write_text(src, encoding='utf-8')
    return name, w, h, f'{title} · 有问题的 {nflag} / {len(screens)}'


def main():
    made = []
    ky = json.loads((APP / 'kyoka_books.json').read_text(encoding='utf-8'))
    for b in ky['books']:
        scr = []
        for i, L in enumerate(b['lessons']):
            p = L.get('peek')
            if not p:
                continue
            body = glance(p) if p.get('glance') else old_peek(p)
            scr.append((phone(f"{b['volume']} {b['title']} · 第 {i + 1} 課", L['title'], body, p.get('go') or '拼起来试试', first_count(L)), flags(p)))
        made.append(board(f"PeekAll_{b['id']}.dc.html", f"{b['volume']} {b['title']}", '课前一眼 · 真实数据', scr))
    kt = json.loads((APP / 'katsuyou_lessons.json').read_text(encoding='utf-8'))
    conj = {x['point_id']: x for x in json.loads((APP / 'conjugation_lessons.json').read_text(encoding='utf-8'))['lessons']}
    for b in kt['books']:
        scr = []
        for i, L in enumerate(b['lessons']):
            p = L.get('peek')
            if not p:
                continue
            t = conj.get(L['point'], {}).get('formula', L['point']).split('（')[0][:14]
            scr.append((phone(f"VOL.{b['group']} {b['play']} · 第 {i + 1} 課", t, glance(p) if p.get('glance') else old_peek(p), p.get('go') or '拼起来试试', first_count(L)), flags(p)))
        made.append(board(f"PeekAll_{b['group']}.dc.html", f"VOL.{b['group']} {b['play']}", '课前一眼 · 真实数据', scr))

    idx = json.loads((PROJ / 'canvas.json').read_text(encoding='utf-8'))
    if not any(pg['id'] == 'peeks' for pg in idx['pages']):
        idx['pages'].append({'id': 'peeks', 'name': '自習 · 课前一眼 全书总览（真实数据）'})
    y = 0
    for name, w, h, title in made:
        idx['boards'][name] = {'x': 0, 'y': y, 'w': w, 'h': h, 'page': 'peeks', 'title': title}
        if name not in idx['order']:
            idx['order'].append(name)
        y += h + 120
    idx['launch'] = {'view': 'canvas', 'page': 'peeks'}
    (PROJ / 'canvas.json').write_text(json.dumps(idx, ensure_ascii=False, indent=1), encoding='utf-8')
    for name, w, h, title in made:
        print(name, w, h, title, (PROJ / name).stat().st_size)


main()
