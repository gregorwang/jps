import { readFile, writeFile } from "node:fs/promises";

const replacements = new Map([
  [
    "source 只截「背格好だけでも分かれば」，但真实证据还包括上一行「せめて」与后面的「話も違う」；不能孤立教学。",
    "不能只看截取短语；「せめて」在上一话轮、后件「話も違う」在同一行末，共同界定最低条件。",
  ],
  [
    "source 单列「〜前に」会遮蔽被动方向；本句不是“在吹走别人前”，而是“在自己被吹走前”。",
    "只教「〜前に」会遮蔽被动方向；本句不是“在吹走别人前”，而是“在自己被吹走前”。",
  ],
  [
    "source 功能只写“接近于”，若不保留「ほう」的二选比较，就会漏掉与前项「魔法」之间的对照。",
    "若只记成普通“接近于”，就会漏掉「ほう」所建立的二选比较，以及它与前项「魔法」的对照。",
  ],
  [
    "source meaning 只保留“很帅”，漏掉「けど」尚未闭合；学习时必须连读下一行的负面续接。",
    "若释义只保留“很帅”，就会漏掉「けど」尚未闭合；学习时必须连读下一行的负面续接。",
  ],
  [
    "source 单行只是「嫉妬の魔女と」，不能独立译成“大家都称她”；完整主语、宾语和动词来自上一行。",
    "孤立的「嫉妬の魔女と」不能独立译成“大家都称她”；完整主语、宾语和动词来自上一话轮。",
  ],
  [
    "不能因「食い」译成进食，也不能把 source 的单词脱离上一行；被阻止的是即将发生的危机。",
    "不能因「食い」译成进食，也不能把这个单词脱离上一话轮；被阻止的是即将发生的危机。",
  ],
  [
    "line 438「死にたくねえ」只关乎自己，line 439 增加「させ」便转向他人。",
    "前一句「死にたくねえな」只关乎自己，当前句增加「させ」便转向他人。",
  ],
  [
    "本行只突出“看见成功”，line 454 才说「鎖の音の正体」。",
    "本行只突出“看见成功”，下一句才说「鎖の音の正体」。",
  ],
  [
    "source meaning 把“锁链声音的真身”补入本行，但日文对象实际位于下一行；不能说本行逐字包含该信息。",
    "若把“锁链声音的真身”直接补入本句就会越过字幕边界；该对象实际位于下一话轮。",
  ],
]);

let replacementCount = 0;
const replaceStrings = (value) => {
  if (typeof value === "string" && replacements.has(value)) {
    replacementCount += 1;
    return replacements.get(value);
  }
  if (Array.isArray(value)) return value.map(replaceStrings);
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, child]) => [key, replaceStrings(child)]),
    );
  }
  return value;
};

for (const path of [
  "regen/batch11_grammar_sentence_linguistic.json",
  "regen/batch11_sentence_linguistic_overlay.json",
]) {
  const rows = JSON.parse(await readFile(path, "utf8"));
  await writeFile(path, `${JSON.stringify(replaceStrings(rows), null, 2)}\n`, "utf8");
}

if (replacementCount !== replacements.size) {
  throw new Error(
    `Expected ${replacements.size} replacements, applied ${replacementCount}`,
  );
}

console.log(JSON.stringify({ replacementCount }));
