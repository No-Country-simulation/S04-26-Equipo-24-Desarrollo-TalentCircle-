/**
 * Pure helper functions for Dashboard stat derivation.
 * Exported separately so they can be unit/property tested in isolation.
 */

/**
 * Count drafts with status === 'PENDING'.
 *
 * @param {import('../types/api').DraftSummaryDto[]} drafts
 * @returns {number}
 */
export function derivePendingCount(drafts) {
  return drafts.filter((d) => d.status === 'PENDING').length
}

/**
 * Return the execution with the lexicographically greatest `startedAt` ISO string.
 * ISO-8601 strings sort correctly as plain strings, so a simple string comparison works.
 *
 * @param {import('../types/api').ExecutionSummaryDto[]} executions - Must be non-empty.
 * @returns {import('../types/api').ExecutionSummaryDto}
 */
export function getMostRecentExecution(executions) {
  return executions.reduce((latest, current) =>
    current.startedAt > latest.startedAt ? current : latest
  )
}
