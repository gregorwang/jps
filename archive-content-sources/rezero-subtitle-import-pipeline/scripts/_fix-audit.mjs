import { readFileSync, writeFileSync } from "node:fs"
const p = "scripts/audit-rezero-zh.mjs"
let t = readFileSync(p)
if (t[1] === 0) t = Buffer.from(t.toString("utf16le").replace(/^\uFEFF/, ""), "utf8")
else t = t.toString("utf8")
t = t.replace(/function findAss[\s\S]*?function canonicalZh/, "function canonicalZh")
t = t.replace("import { readFileSync, writeFileSync } from 'node:fs'", "import { readFileSync, writeFileSync, readdirSync } from 'node:fs'")
t = t.replace("  const { readdirSync } = await import('node:fs')\n  const jaLines", "  const jaLines")
writeFileSync(p, t, "utf8")
console.log("fixed")
