#!/usr/bin/env node

import fs from 'node:fs'
import path from 'node:path'
import { pathToFileURL } from 'node:url'

const REQUIRED_SECTIONS = [
  'Message à faire passer',
  'Déroulé oral',
  'Insister sur',
  'Transition',
]

export function validatePresenterNote(note) {
  const errors = []

  if (/AUTO-GENERATED:[^\r\n]*:(?:START|END)/.test(note)) {
    errors.push('contains an AUTO-GENERATED marker')
  }

  const headingPattern =
    /^\s*\*\*(Message à faire passer|Déroulé oral|Insister sur|Transition)\*\*\s*:?\s*$/gm
  const headings = [...note.matchAll(headingPattern)].map((match) => ({
    label: match[1],
    index: match.index,
    contentStart: match.index + match[0].length,
  }))

  for (const section of REQUIRED_SECTIONS) {
    const occurrences = headings.filter((heading) => heading.label === section)

    if (occurrences.length === 0) {
      errors.push(`is missing the "${section}" section`)
    } else if (occurrences.length > 1) {
      errors.push(`duplicates the "${section}" section`)
    }
  }

  if (
    headings.length === REQUIRED_SECTIONS.length &&
    !headings.every(
      (heading, index) => heading.label === REQUIRED_SECTIONS[index],
    )
  ) {
    errors.push('has presenter-note sections in the wrong order')
  }

  for (const [index, heading] of headings.entries()) {
    const contentEnd = headings[index + 1]?.index ?? note.length
    const content = note.slice(heading.contentStart, contentEnd).trim()

    if (!content) {
      errors.push(`has an empty "${heading.label}" section`)
    }
  }

  return errors
}

async function main() {
  const entryPath = path.resolve(process.argv[2] ?? 'slidev/slides.md')

  if (!fs.existsSync(entryPath)) {
    console.error(`Slidev entry not found: ${entryPath}`)
    process.exit(1)
  }

  const slidevRoot = path.dirname(entryPath)
  const parserPath = path.join(
    slidevRoot,
    'node_modules/@slidev/parser/dist/core.mjs',
  )

  if (!fs.existsSync(parserPath)) {
    console.error(
      `Slidev parser not found. Run the package installation in ${slidevRoot} first.`,
    )
    process.exit(1)
  }

  const { parseSync } = await import(pathToFileURL(parserPath).href)
  const visitedFiles = new Set()
  const slides = []

  function visit(filePath) {
    const resolvedPath = path.resolve(filePath)

    if (visitedFiles.has(resolvedPath)) return
    visitedFiles.add(resolvedPath)

    if (!fs.existsSync(resolvedPath)) {
      console.error(`Imported slide file not found: ${resolvedPath}`)
      process.exitCode = 1
      return
    }

    const source = fs.readFileSync(resolvedPath, 'utf8')
    const parsed = parseSync(source, resolvedPath)

    for (const slide of parsed.slides) {
      const importedSource = slide.frontmatter?.src

      if (typeof importedSource === 'string' && importedSource.trim()) {
        visit(path.resolve(path.dirname(resolvedPath), importedSource.trim()))
        continue
      }

      slides.push({
        filePath: resolvedPath,
        index: slide.index + 1,
        title: slide.title || '(sans titre)',
        note: slide.note?.trim(),
      })
    }
  }

  visit(entryPath)

  const errors = []
  const warnings = []

  for (const slide of slides) {
    const location = `${path.relative(process.cwd(), slide.filePath)}:${slide.index}`

    if (!slide.note) {
      errors.push(`${location} "${slide.title}" has no presenter note`)
      continue
    }

    for (const error of validatePresenterNote(slide.note)) {
      errors.push(`${location} "${slide.title}" ${error}`)
    }

    const wordCount = slide.note.match(/\S+/g)?.length ?? 0

    if (wordCount < 60) {
      warnings.push(
        `${location} "${slide.title}" has a short note (${wordCount} words)`,
      )
    } else if (wordCount > 180) {
      warnings.push(
        `${location} "${slide.title}" has a long note (${wordCount} words)`,
      )
    }
  }

  for (const warning of warnings) console.warn(`WARN: ${warning}`)
  for (const error of errors) console.error(`ERROR: ${error}`)

  if (errors.length || process.exitCode) {
    console.error(`Presenter notes invalid: ${errors.length} error(s)`)
    process.exit(1)
  }

  console.log(`Presenter notes valid: ${slides.length}/${slides.length} slides`)
}

const isMain =
  process.argv[1] &&
  pathToFileURL(path.resolve(process.argv[1])).href === import.meta.url

if (isMain) await main()
