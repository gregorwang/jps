import type { MoraEvidence } from "../contracts";
import { clamp } from "../utils/math";

export function modelAgreement(left: readonly MoraEvidence[], right: readonly MoraEvidence[]): number {
  const leftValues = left.map((item) => item.value);
  const rightValues = right.map((item) => item.value);
  if (leftValues.length === 0 && rightValues.length === 0) return 0;
  const distance = editDistance(leftValues, rightValues);
  return clamp(1 - distance / Math.max(1, leftValues.length, rightValues.length), 0, 1);
}

function editDistance(left: readonly string[], right: readonly string[]): number {
  let previous = Array.from({ length: right.length + 1 }, (_, index) => index);
  for (let leftIndex = 1; leftIndex <= left.length; leftIndex += 1) {
    const current = Array.from<number>({ length: right.length + 1 }).fill(0);
    current[0] = leftIndex;
    for (let rightIndex = 1; rightIndex <= right.length; rightIndex += 1) {
      const substitution = (previous[rightIndex - 1] ?? 0) + (left[leftIndex - 1] === right[rightIndex - 1] ? 0 : 1);
      const deletion = (previous[rightIndex] ?? 0) + 1;
      const insertion = (current[rightIndex - 1] ?? 0) + 1;
      current[rightIndex] = Math.min(substitution, deletion, insertion);
    }
    previous = current;
  }
  return previous[right.length] ?? 0;
}
