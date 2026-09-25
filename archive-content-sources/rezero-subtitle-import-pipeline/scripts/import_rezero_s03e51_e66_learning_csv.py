#!/usr/bin/env python3
"""Thin launcher: delegates to Node importer on Windows where .py encoding is unreliable."""
import os, subprocess, sys
from pathlib import Path
root = Path(__file__).resolve().parent
env = os.environ.copy()
env.setdefault("DRY_RUN", "1")
rc = subprocess.call(["node", str(root / "import-rezero-s03e51-e66.mjs")], cwd=root.parent, env=env)
sys.exit(rc)