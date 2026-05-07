/**
 * Property-based tests for dashboardHelpers.js
 *
 * Property 10: Dashboard stat derivation — pending count
 * For any array of DraftSummaryDto objects, the "Pendientes de Revisión" count
 * must equal the number of items where status === 'PENDING'.
 * Validates: Requirements 7.6
 *
 * Property 11: Dashboard most-recent execution used for activities
 * For any non-empty array of ExecutionSummaryDto objects, getMostRecentExecution
 * must return the execution with the lexicographically greatest startedAt value.
 * Validates: Requirements 7.2
 */

import { describe, it, expect } from 'vitest'
import * as fc from 'fast-check'
import { derivePendingCount, getMostRecentExecution } from './dashboardHelpers'

// ─── Arbitraries ──────────────────────────────────────────────────────────────

const draftStatusArb = fc.constantFrom('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED')

const draftSummaryArb = fc.record({
  id: fc.string({ minLength: 1 }),
  channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
  status: draftStatusArb,
  createdAt: fc
    .integer({ min: new Date('2020-01-01T00:00:00.000Z').getTime(), max: new Date('2030-12-31T23:59:59.999Z').getTime() })
    .map((ts) => new Date(ts).toISOString()),
  summary: fc.string(),
})

// Use bounded dates to avoid Invalid Date edge cases from fast-check
// Generate timestamps as integers and convert to ISO strings — avoids Invalid Date during shrinking
const MIN_TS = new Date('2020-01-01T00:00:00.000Z').getTime()
const MAX_TS = new Date('2030-12-31T23:59:59.999Z').getTime()

const isoDateArb = fc
  .integer({ min: MIN_TS, max: MAX_TS })
  .map((ts) => new Date(ts).toISOString())

const executionSummaryArb = fc.record({
  id: fc.string({ minLength: 1 }),
  weekStart: isoDateArb.map((s) => s.slice(0, 10)),
  weekEnd: isoDateArb.map((s) => s.slice(0, 10)),
  status: fc.constantFrom('COMPLETED', 'RUNNING', 'FAILED'),
  startedAt: isoDateArb,
  completedAt: fc.option(isoDateArb, { nil: null }),
})

// ─── Property 10: Pending count derivation ────────────────────────────────────
// Feature: frontend-backend-integration, Property 10: Dashboard stat derivation — pending count
describe('Property 10: Dashboard stat derivation — pending count', () => {
  it('derivePendingCount returns the count of drafts with status PENDING for any input array', () => {
    fc.assert(
      fc.property(fc.array(draftSummaryArb), (drafts) => {
        const expected = drafts.filter((d) => d.status === 'PENDING').length
        expect(derivePendingCount(drafts)).toBe(expected)
      }),
      { numRuns: 100 }
    )
  })

  it('returns 0 for an empty array', () => {
    expect(derivePendingCount([])).toBe(0)
  })

  it('returns 0 when no drafts are PENDING', () => {
    const boundedIso = fc
      .integer({ min: new Date('2020-01-01T00:00:00.000Z').getTime(), max: new Date('2030-12-31T23:59:59.999Z').getTime() })
      .map((ts) => new Date(ts).toISOString())
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.string({ minLength: 1 }),
            channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
            status: fc.constantFrom('APPROVED', 'REJECTED', 'PUBLISHED'),
            createdAt: boundedIso,
            summary: fc.string(),
          })
        ),
        (drafts) => {
          expect(derivePendingCount(drafts)).toBe(0)
        }
      ),
      { numRuns: 50 }
    )
  })

  it('returns the full length when all drafts are PENDING', () => {
    const boundedIso = fc
      .integer({ min: new Date('2020-01-01T00:00:00.000Z').getTime(), max: new Date('2030-12-31T23:59:59.999Z').getTime() })
      .map((ts) => new Date(ts).toISOString())
    fc.assert(
      fc.property(
        fc.array(
          fc.record({
            id: fc.string({ minLength: 1 }),
            channel: fc.constantFrom('NEWSLETTER', 'LINKEDIN', 'TWITTER'),
            status: fc.constant('PENDING'),
            createdAt: boundedIso,
            summary: fc.string(),
          })
        ),
        (drafts) => {
          expect(derivePendingCount(drafts)).toBe(drafts.length)
        }
      ),
      { numRuns: 50 }
    )
  })
})

// ─── Property 11: Most-recent execution selection ─────────────────────────────
// Feature: frontend-backend-integration, Property 11: Dashboard most-recent execution used for activities
describe('Property 11: Dashboard most-recent execution used for activities', () => {
  it('getMostRecentExecution returns the execution with the greatest startedAt for any non-empty array', () => {
    fc.assert(
      fc.property(
        fc.array(executionSummaryArb, { minLength: 1 }),
        (executions) => {
          const result = getMostRecentExecution(executions)

          // The result must be one of the executions in the array
          expect(executions).toContainEqual(result)

          // No other execution should have a greater startedAt
          const maxStartedAt = executions.reduce(
            (max, e) => (e.startedAt > max ? e.startedAt : max),
            ''
          )
          expect(result.startedAt).toBe(maxStartedAt)
        }
      ),
      { numRuns: 100 }
    )
  })

  it('returns the single element when the array has exactly one execution', () => {
    fc.assert(
      fc.property(executionSummaryArb, (execution) => {
        expect(getMostRecentExecution([execution])).toEqual(execution)
      }),
      { numRuns: 50 }
    )
  })

  it('correctly identifies the most recent among executions with distinct startedAt values', () => {
    const executions = [
      { id: 'a', startedAt: '2025-01-01T00:00:00Z', weekStart: '', weekEnd: '', status: 'COMPLETED', completedAt: null },
      { id: 'b', startedAt: '2025-03-15T12:00:00Z', weekStart: '', weekEnd: '', status: 'COMPLETED', completedAt: null },
      { id: 'c', startedAt: '2025-02-10T08:00:00Z', weekStart: '', weekEnd: '', status: 'FAILED',    completedAt: null },
    ]
    expect(getMostRecentExecution(executions).id).toBe('b')
  })
})
