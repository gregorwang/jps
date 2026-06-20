import type { ReactNode } from 'react'
import type { StructuredAiResult } from '../lib/types'

type StructuredAiResultViewProps = {
  result: StructuredAiResult
}

export function StructuredAiResultView({ result }: StructuredAiResultViewProps) {
  const hasSections = result.sections.length > 0

  return (
    <article className="ai-structured-result">
      <p className="eyebrow">{result.title}</p>
      <div className="ai-summary">
        <MarkdownText text={result.summary} />
      </div>
      {hasSections ? (
        <div className="ai-article-body">
          {result.sections.map((section) => (
            <section className="ai-article-section" key={section.title}>
              <h3>{section.title}</h3>
              <MarkdownText text={section.body || 'AI 返回中未单独拆出这一栏，请参考完整文本。'} />
            </section>
          ))}
        </div>
      ) : (
        <MarkdownText text={result.text} />
      )}
      {hasSections && result.text ? (
        <details>
          <summary>完整返回</summary>
          <div className="ai-full-text">
            <MarkdownText text={result.text} />
          </div>
        </details>
      ) : null}
    </article>
  )
}

function MarkdownText({ text }: { text: string }) {
  const blocks = parseBlocks(text)

  return (
    <div className="ai-markdown">
      {blocks.map((block, index) => {
        if (block.type === 'heading') {
          return <h3 key={index}>{renderInline(block.text)}</h3>
        }
        if (block.type === 'ordered-list') {
          return (
            <ol key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInline(item)}</li>
              ))}
            </ol>
          )
        }
        if (block.type === 'unordered-list') {
          return (
            <ul key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInline(item)}</li>
              ))}
            </ul>
          )
        }
        return <p key={index}>{renderInline(block.text)}</p>
      })}
    </div>
  )
}

type MarkdownBlock =
  | { type: 'heading'; text: string }
  | { type: 'paragraph'; text: string }
  | { type: 'ordered-list'; items: string[] }
  | { type: 'unordered-list'; items: string[] }

function parseBlocks(input: string): MarkdownBlock[] {
  const lines = input
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => line.trim())

  const blocks: MarkdownBlock[] = []
  let paragraph: string[] = []
  let list: { type: 'ordered-list' | 'unordered-list'; items: string[] } | null = null

  function flushParagraph() {
    if (paragraph.length === 0) return
    blocks.push({ type: 'paragraph', text: paragraph.join(' ') })
    paragraph = []
  }

  function flushList() {
    if (!list) return
    blocks.push(list)
    list = null
  }

  for (const line of lines) {
    if (!line) {
      flushParagraph()
      flushList()
      continue
    }

    const heading = line.match(/^(?:#{1,6}\s*)?(?:\d+[.、]\s*)?(?:\*\*)?(.+?)(?:\*\*)?\s*[：:]?\s*$/u)
    const isColumnHeading =
      Boolean(heading && line.length <= 28 && /^(?:#{1,6}\s+|\d+[.、]\s*).+/u.test(line)) ||
      /^(?:\*\*)?[\p{Script=Han}A-Za-z/（）()]+(?:\*\*)?\s*[：:]$/u.test(line)
    if (line.startsWith('#') || isColumnHeading) {
      flushParagraph()
      flushList()
      blocks.push({ type: 'heading', text: cleanMarkdownText(heading?.[1] ?? line) })
      continue
    }

    const ordered = line.match(/^\d+[.、]\s+(.+)$/u)
    if (ordered) {
      flushParagraph()
      if (!list || list.type !== 'ordered-list') {
        flushList()
        list = { type: 'ordered-list', items: [] }
      }
      list.items.push(ordered[1])
      continue
    }

    const unordered = line.match(/^[-*•]\s+(.+)$/u)
    if (unordered) {
      flushParagraph()
      if (!list || list.type !== 'unordered-list') {
        flushList()
        list = { type: 'unordered-list', items: [] }
      }
      list.items.push(unordered[1])
      continue
    }

    flushList()
    paragraph.push(line)
  }

  flushParagraph()
  flushList()
  return blocks.length > 0 ? blocks : [{ type: 'paragraph', text: input }]
}

function renderInline(input: string): ReactNode[] {
  const nodes: ReactNode[] = []
  const pattern = /(`([^`]+)`)|(\*\*([^*]+)\*\*)|(\*([^*]+)\*)/g
  let lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = pattern.exec(input))) {
    if (match.index > lastIndex) {
      nodes.push(cleanMarkdownText(input.slice(lastIndex, match.index)))
    }
    if (match[2]) {
      nodes.push(<code key={nodes.length}>{match[2]}</code>)
    } else if (match[4]) {
      nodes.push(<strong key={nodes.length}>{cleanMarkdownText(match[4])}</strong>)
    } else if (match[6]) {
      nodes.push(<em key={nodes.length}>{cleanMarkdownText(match[6])}</em>)
    }
    lastIndex = pattern.lastIndex
  }

  if (lastIndex < input.length) {
    nodes.push(cleanMarkdownText(input.slice(lastIndex)))
  }

  return nodes
}

function cleanMarkdownText(input: string) {
  return input
    .replace(/^#{1,6}\s*/u, '')
    .replace(/^\d+[.、]\s*/u, '')
    .replace(/^\s*[-*•]\s+/u, '')
    .replace(/\*\*/g, '')
    .trim()
}
