import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { Marked, type Tokens } from 'marked'
import { defineConfig, type Plugin } from 'vite'

const repoRoot = resolve(import.meta.dirname, '..')
const repoBlob = 'https://github.com/westonkd/duo-fold/blob/main/'
const sitePages: Record<string, string> = {
  'PRIVACY.md': '/privacy',
  'PERMISSIONS.md': '/permissions',
}

const marked = new Marked({
  walkTokens(token) {
    if (token.type !== 'link') return
    const link = token as Tokens.Link
    if (/^[a-z]+:|^#/i.test(link.href)) return
    link.href = sitePages[link.href] ?? repoBlob + link.href
  },
})

function renderMarkdown(file: string) {
  const html = marked.parse(readFileSync(resolve(repoRoot, file), 'utf8'), { async: false })
  return html.replaceAll('<table>', '<div class="table"><table>').replaceAll('</table>', '</table></div>')
}

function repoMarkdown(): Plugin {
  return {
    name: 'folio-repo-markdown',
    transformIndexHtml: {
      order: 'pre',
      handler: (html) => html.replace(/<!--\s*markdown:(\S+)\s*-->/g, (_, file: string) => renderMarkdown(file)),
    },
    configureServer(server) {
      const docs = Object.keys(sitePages).map((file) => resolve(repoRoot, file))
      server.watcher.add(docs)
      server.watcher.on('change', (path) => {
        if (docs.includes(path)) server.ws.send({ type: 'full-reload' })
      })
    },
  }
}

export default defineConfig({
  plugins: [repoMarkdown()],
  build: {
    rolldownOptions: {
      input: {
        index: resolve(import.meta.dirname, 'index.html'),
        privacy: resolve(import.meta.dirname, 'privacy.html'),
        permissions: resolve(import.meta.dirname, 'permissions.html'),
        notFound: resolve(import.meta.dirname, '404.html'),
      },
    },
  },
})
