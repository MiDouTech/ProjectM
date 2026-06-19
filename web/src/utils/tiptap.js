// Tiptap JSON ↔ 导出工具（HTML / Markdown）。覆盖 StarterKit 常见节点；
// 复杂节点（表格等）暂按文本降级，满足 P2 导出需求。

function escapeHtml(s = '') {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function textWithMarks(node, fmt) {
  let t = fmt === 'html' ? escapeHtml(node.text || '') : (node.text || '')
  for (const m of node.marks || []) {
    if (fmt === 'html') {
      if (m.type === 'bold') t = `<strong>${t}</strong>`
      else if (m.type === 'italic') t = `<em>${t}</em>`
      else if (m.type === 'strike') t = `<s>${t}</s>`
      else if (m.type === 'code') t = `<code>${t}</code>`
    } else {
      if (m.type === 'bold') t = `**${t}**`
      else if (m.type === 'italic') t = `*${t}*`
      else if (m.type === 'strike') t = `~~${t}~~`
      else if (m.type === 'code') t = `\`${t}\``
    }
  }
  return t
}

function inline(node, fmt) {
  if (!node) return ''
  if (node.type === 'text') return textWithMarks(node, fmt)
  if (node.type === 'hardBreak') return fmt === 'html' ? '<br/>' : '  \n'
  return (node.content || []).map((n) => inline(n, fmt)).join('')
}

export function toHtml(doc) {
  if (!doc) return ''
  const walk = (nodes = []) => nodes.map(block).join('\n')
  const block = (n) => {
    switch (n.type) {
      case 'paragraph': return `<p>${inline(n, 'html')}</p>`
      case 'heading': return `<h${n.attrs?.level || 1}>${inline(n, 'html')}</h${n.attrs?.level || 1}>`
      case 'blockquote': return `<blockquote>${walk(n.content)}</blockquote>`
      case 'bulletList': return `<ul>${walk(n.content)}</ul>`
      case 'orderedList': return `<ol>${walk(n.content)}</ol>`
      case 'listItem': return `<li>${walk(n.content)}</li>`
      case 'codeBlock': return `<pre><code>${escapeHtml((n.content || []).map((c) => c.text || '').join(''))}</code></pre>`
      case 'horizontalRule': return '<hr/>'
      default: return inline(n, 'html')
    }
  }
  return walk(doc.content)
}

export function toMarkdown(doc) {
  if (!doc) return ''
  const block = (n, depth = 0) => {
    switch (n.type) {
      case 'paragraph': return inline(n, 'md') + '\n'
      case 'heading': return `${'#'.repeat(n.attrs?.level || 1)} ${inline(n, 'md')}\n`
      case 'blockquote': return (n.content || []).map((c) => `> ${block(c, depth).trim()}`).join('\n') + '\n'
      case 'bulletList': return (n.content || []).map((li) => `${'  '.repeat(depth)}- ${block(li, depth + 1).trim()}`).join('\n') + '\n'
      case 'orderedList': return (n.content || []).map((li, i) => `${'  '.repeat(depth)}${i + 1}. ${block(li, depth + 1).trim()}`).join('\n') + '\n'
      case 'listItem': return (n.content || []).map((c) => block(c, depth)).join('')
      case 'codeBlock': return '```\n' + (n.content || []).map((c) => c.text || '').join('') + '\n```\n'
      case 'horizontalRule': return '---\n'
      default: return inline(n, 'md') + '\n'
    }
  }
  return (doc.content || []).map((n) => block(n)).join('\n')
}

// 触发浏览器下载文本文件
export function downloadText(filename, text, mime = 'text/plain') {
  const blob = new Blob([text], { type: `${mime};charset=utf-8` })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
