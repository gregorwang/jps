"""Snapshot every read-only content endpoint the App calls into app/src/main/assets/content/.

The App (RemoteLabClient.get) serves these paths from the pack and never touches the network
for them; anything missing still goes to the worker. Re-run before a release whenever the
Supabase content changed:  python scripts/build-content-pack.py
"""
import hashlib, json, os, sys, urllib.parse, urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = "https://anime-japanese-lab.ishallnotwant123.workers.dev"
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130 Safari/537.36"
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "content")
CURRICULUM, PAGE = "foundation-v1", 100


def fetch(path):
    req = urllib.request.Request(BASE + path, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8")


def q(params):  # same encoding as java.net.URLEncoder
    return "&".join(f"{urllib.parse.quote_plus(k)}={urllib.parse.quote_plus(v)}" for k, v in params)


pack = {}


def take(path):
    body = fetch(path)
    pack[path] = body
    return body


def paged(endpoint, extra=()):
    items, cursor = [], None
    while True:
        params = [("curriculumVersion", CURRICULUM)] + ([("cursor", cursor)] if cursor else []) + [("limit", str(PAGE))] + list(extra)
        root = json.loads(take(f"{endpoint}?{q(params)}"))
        items += root["items"]
        if not root["page"]["hasMore"]:
            return items
        cursor = root["page"]["nextCursor"]


def main():
    works = json.loads(take("/api/works"))
    take("/api/conjugation-drill/items")
    take("/api/linguistic-exercises")
    paged("/api/linguistics/foundation/topics")
    packs = paged("/api/linguistics/foundation/packs")
    for p in packs:
        paged("/api/linguistics/foundation/questions", [("packId", p["id"])])

    episode_paths = []
    for w in works:
        slug = w.get("slug") or w.get("workSlug")
        enc = urllib.parse.quote(slug, safe="")
        for e in json.loads(take(f"/api/works/{enc}/episodes")):
            base = f"/api/works/{enc}/episodes/{e['episode']}"
            episode_paths += [f"{base}/vocab", f"{base}/grammar", f"{base}/sentences",
                              f"{base}/exercises?limit=600", f"{base}/plan", f"{base}/subtitles"]
    with ThreadPoolExecutor(8) as pool:
        for path, body in zip(episode_paths, pool.map(fetch, episode_paths)):
            pack[path] = body

    os.makedirs(OUT, exist_ok=True)
    for f in os.listdir(OUT):
        os.remove(os.path.join(OUT, f))
    total = 0
    for path, body in pack.items():
        name = hashlib.sha1(path.encode("utf-8")).hexdigest() + ".json"
        with open(os.path.join(OUT, name), "w", encoding="utf-8", newline="") as fh:
            fh.write(body)
        total += len(body.encode("utf-8"))
    with open(os.path.join(OUT, "index.txt"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(sorted(pack)) + "\n")
    print(f"{len(pack)} paths, {total / 1e6:.1f} MB raw")


if __name__ == "__main__":
    sys.exit(main())
