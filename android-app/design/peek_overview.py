"""课前一眼自查：读 App 里的 kyoka_books.json / katsuyou_lessons.json，按书列出有问题的课（只在本地出清单，不往画布上画）。

查的是：规则还是一整段（没有拆成 beats / 没换成 glance）、上排放不下、例句左栏太长、没有例句。
用法：python design/peek_overview.py
"""
import json, pathlib

APP = pathlib.Path(__file__).resolve().parents[1] / 'app/src/main/assets'


def flags(p):
    if p.get('glance') or p.get('beats'):
        return []
    out = []
    n = len(p['rule'])
    if n > 60:
        out.append(f'规则 {n} 字（一眼 ≤ 40）')
    top = sum(len(t) for t in p['ends']) * 22 + len(p['ends']) * 18 + sum(len(t) for t, _ in p['result']) * 30 + 40
    if top > 350:
        out.append('上排可能放不下')
    if any(len(x['base']) > 5 for x in p['examples']):
        out.append('例句左栏偏长')
    if not p['examples']:
        out.append('没有例句')
    return out


def report(name, lessons):
    bad = [(L['point'], f) for L in lessons if L.get('peek') for f in [flags(L['peek'])] if f]
    print(f'{name}: {len(bad)} / {sum(1 for L in lessons if L.get("peek"))}')
    for pt, f in bad:
        print(f'   {pt}  ' + ' · '.join(f))


if __name__ == '__main__':
    for b in json.loads((APP / 'kyoka_books.json').read_text(encoding='utf-8'))['books']:
        report(f"{b['volume']} {b['title']}", b['lessons'])
    for b in json.loads((APP / 'katsuyou_lessons.json').read_text(encoding='utf-8'))['books']:
        report(f"VOL.{b['group']} {b['play']}", b['lessons'])
