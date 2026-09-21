import assert from 'node:assert/strict'
import test from 'node:test'

import { validatePresenterNote } from './validate_presenter_notes.mjs'

const validNote = `
**Message à faire passer**

Une conclusion essentielle.

**Déroulé oral**

Une explication naturelle à lire ou à reformuler.

**Insister sur**

Une distinction importante.

**Transition**

Un lien causal vers la suite.
`

test('accepts a note that follows the presenter-note contract', () => {
  assert.deepEqual(validatePresenterNote(validNote), [])
})

test('rejects a note with a missing section', () => {
  const note = validNote.replace(
    /\*\*Insister sur\*\*[\s\S]*?(?=\*\*Transition\*\*)/,
    '',
  )

  assert.ok(
    validatePresenterNote(note).includes(
      'is missing the "Insister sur" section',
    ),
  )
})

test('rejects a note with an empty section', () => {
  const note = validNote.replace('Une distinction importante.', '')

  assert.ok(
    validatePresenterNote(note).includes('has an empty "Insister sur" section'),
  )
})

test('rejects sections in the wrong order', () => {
  const note = validNote
    .replace('**Déroulé oral**', '**TEMPORARY**')
    .replace('**Insister sur**', '**Déroulé oral**')
    .replace('**TEMPORARY**', '**Insister sur**')

  assert.ok(
    validatePresenterNote(note).includes(
      'has presenter-note sections in the wrong order',
    ),
  )
})

test('rejects a duplicated section', () => {
  const note = `${validNote}\n**Transition**\n\nUne autre transition.`

  assert.ok(
    validatePresenterNote(note).includes('duplicates the "Transition" section'),
  )
})

test('rejects unstructured text', () => {
  const errors = validatePresenterNote(
    'Cette note contient assez de texte, mais aucune rubrique obligatoire.',
  )

  assert.equal(errors.filter((error) => error.includes('is missing')).length, 4)
})

test('rejects an AUTO-GENERATED marker mixed with a real note', () => {
  const note = `${validNote}\nAUTO-GENERATED:W001:END`

  assert.ok(
    validatePresenterNote(note).includes('contains an AUTO-GENERATED marker'),
  )
})
