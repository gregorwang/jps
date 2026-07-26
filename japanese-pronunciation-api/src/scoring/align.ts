import type { MoraEvidence } from "../contracts";
import { clamp } from "../utils/math";

export type AlignmentOperationType = "match" | "substitution" | "deletion" | "insertion";

export interface AlignmentOperation {
  type: AlignmentOperationType;
  cost: number;
  expected?: MoraEvidence;
  heard?: MoraEvidence;
}

export interface MoraAlignment {
  score: number;
  totalCost: number;
  substitutions: number;
  deletions: number;
  insertions: number;
  matches: number;
  operations: AlignmentOperation[];
}

const DELETION_COST = 1;
const INSERTION_COST = 0.35;

export function alignMorae(expected: readonly MoraEvidence[], heard: readonly MoraEvidence[]): MoraAlignment {
  const columns = heard.length + 1;
  const cells = (expected.length + 1) * columns;
  const costs = new Float64Array(cells);
  costs.fill(Number.POSITIVE_INFINITY);
  const directions = new Uint8Array(cells);
  const at = (row: number, column: number): number => row * columns + column;
  costs[0] = 0;

  for (let row = 1; row <= expected.length; row += 1) {
    costs[at(row, 0)] = row * DELETION_COST;
    directions[at(row, 0)] = 2;
  }
  for (let column = 1; column <= heard.length; column += 1) {
    costs[at(0, column)] = column * INSERTION_COST;
    directions[at(0, column)] = 3;
  }

  for (let row = 1; row <= expected.length; row += 1) {
    for (let column = 1; column <= heard.length; column += 1) {
      const expectedMora = expected[row - 1];
      const heardMora = heard[column - 1];
      if (!expectedMora || !heardMora) continue;
      const diagonalCost = (costs[at(row - 1, column - 1)] ?? Number.POSITIVE_INFINITY) + substitutionCost(expectedMora.value, heardMora.value);
      const deletionCost = (costs[at(row - 1, column)] ?? Number.POSITIVE_INFINITY) + DELETION_COST;
      const insertionCost = (costs[at(row, column - 1)] ?? Number.POSITIVE_INFINITY) + INSERTION_COST;

      if (diagonalCost <= deletionCost && diagonalCost <= insertionCost) {
        costs[at(row, column)] = diagonalCost;
        directions[at(row, column)] = 1;
      } else if (deletionCost <= insertionCost) {
        costs[at(row, column)] = deletionCost;
        directions[at(row, column)] = 2;
      } else {
        costs[at(row, column)] = insertionCost;
        directions[at(row, column)] = 3;
      }
    }
  }

  const operations: AlignmentOperation[] = [];
  let row = expected.length;
  let column = heard.length;
  while (row > 0 || column > 0) {
    const direction = directions[at(row, column)];
    if (direction === 1 && row > 0 && column > 0) {
      const expectedMora = expected[row - 1];
      const heardMora = heard[column - 1];
      if (expectedMora && heardMora) {
        const cost = substitutionCost(expectedMora.value, heardMora.value);
        operations.push({
          type: cost === 0 ? "match" : "substitution",
          cost,
          expected: expectedMora,
          heard: heardMora,
        });
      }
      row -= 1;
      column -= 1;
    } else if (direction === 2 && row > 0) {
      const expectedMora = expected[row - 1];
      if (expectedMora) operations.push({ type: "deletion", cost: DELETION_COST, expected: expectedMora });
      row -= 1;
    } else if (column > 0) {
      const heardMora = heard[column - 1];
      if (heardMora) operations.push({ type: "insertion", cost: INSERTION_COST, heard: heardMora });
      column -= 1;
    } else {
      break;
    }
  }
  operations.reverse();

  const totalCost = costs[at(expected.length, heard.length)] ?? Number.POSITIVE_INFINITY;
  return {
    score: clamp(100 * (1 - totalCost / Math.max(1, expected.length)), 0, 100),
    totalCost,
    substitutions: operations.filter((operation) => operation.type === "substitution").length,
    deletions: operations.filter((operation) => operation.type === "deletion").length,
    insertions: operations.filter((operation) => operation.type === "insertion").length,
    matches: operations.filter((operation) => operation.type === "match").length,
    operations,
  };
}

function substitutionCost(expected: string, heard: string): number {
  if (expected === heard) return 0;
  if (expected === "�" || heard === "�") return 1;
  const vowels = new Set(["あ", "い", "う", "え", "お"]);
  if ((expected === "ー" && vowels.has(heard)) || (heard === "ー" && vowels.has(expected))) return 0.35;
  return 1;
}
