import { readFile } from "node:fs/promises";
import { parseDocument } from "yaml";

const source = await readFile(new URL("../openapi.yaml", import.meta.url), "utf8");
const document = parseDocument(source, { prettyErrors: true, strict: true });
if (document.errors.length > 0) {
  for (const error of document.errors) console.error(error.message);
  process.exitCode = 1;
} else {
  const value = document.toJS();
  if (value?.openapi !== "3.1.0" || !value?.paths?.["/v1/pronunciation/evaluate"]) {
    throw new Error("OpenAPI document is missing its version or evaluation path");
  }
  console.log("OpenAPI YAML parsed successfully.");
}
