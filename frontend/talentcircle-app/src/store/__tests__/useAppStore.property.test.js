// Feature: frontend-backend-integration, Property 1: Token persistence after login
// Validates: Requirements 2.2

import { describe, it, beforeEach } from 'vitest'
import * as fc from 'fast-check'
import { expect } from 'vitest'
import { useAppStore } from '../useAppStore'

describe('useAppStore — Property 1: Token persistence after login', () => {
  beforeEach(() => {
    // Clear localStorage and reset store state before each test
    localStorage.clear()
    useAppStore.setState({
      isAuthenticated: false,
      currentUser: null,
      accessToken: null,
      refreshToken: null,
    })
  })

  it(
    'stores accessToken and refreshToken in localStorage for any valid LoginResponse',
    () => {
      fc.assert(
        fc.property(
          fc.record({
            accessToken: fc.string({ minLength: 1 }),
            refreshToken: fc.string({ minLength: 1 }),
            expiresIn: fc.string(),
            user: fc.record({
              id: fc.string(),
              email: fc.emailAddress(),
              fullName: fc.string(),
              role: fc.constantFrom('ADMIN', 'EDITOR'),
              active: fc.boolean(),
            }),
          }),
          (loginResponse) => {
            // Reset localStorage before each iteration to avoid state leakage
            localStorage.clear()
            useAppStore.setState({
              isAuthenticated: false,
              currentUser: null,
              accessToken: null,
              refreshToken: null,
            })

            // Act: call the login store action
            useAppStore.getState().login(loginResponse)

            // Assert: both tokens must be persisted to localStorage
            expect(localStorage.getItem('accessToken')).toBe(loginResponse.accessToken)
            expect(localStorage.getItem('refreshToken')).toBe(loginResponse.refreshToken)
          }
        ),
        { numRuns: 100 }
      )
    }
  )
})

// Feature: frontend-backend-integration, Property 3: Session restoration from localStorage
// Validates: Requirements 2.4

describe('useAppStore — Property 3: Session restoration from localStorage', () => {
  const initialState = {
    isAuthenticated: false,
    currentUser: null,
    accessToken: null,
    refreshToken: null,
  }

  beforeEach(() => {
    localStorage.clear()
    useAppStore.setState(initialState)
  })

  it(
    'sets isAuthenticated to true and restores token fields when both tokens are present in localStorage',
    () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          fc.string({ minLength: 1 }),
          (accessToken, refreshToken) => {
            // Arrange: reset store and seed localStorage with both tokens
            localStorage.clear()
            useAppStore.setState(initialState)
            localStorage.setItem('accessToken', accessToken)
            localStorage.setItem('refreshToken', refreshToken)

            // Act
            useAppStore.getState().initAuth()

            // Assert: session must be restored
            const state = useAppStore.getState()
            expect(state.isAuthenticated).toBe(true)
            expect(state.accessToken).toBe(accessToken)
            expect(state.refreshToken).toBe(refreshToken)
          }
        ),
        { numRuns: 100 }
      )
    }
  )

  it(
    'leaves isAuthenticated as false when only accessToken is present (no refreshToken)',
    () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          (accessToken) => {
            // Arrange: only accessToken in localStorage
            localStorage.clear()
            useAppStore.setState(initialState)
            localStorage.setItem('accessToken', accessToken)
            // refreshToken intentionally absent

            // Act
            useAppStore.getState().initAuth()

            // Assert: session must NOT be restored
            expect(useAppStore.getState().isAuthenticated).toBe(false)
          }
        ),
        { numRuns: 100 }
      )
    }
  )

  it(
    'leaves isAuthenticated as false when only refreshToken is present (no accessToken)',
    () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          (refreshToken) => {
            // Arrange: only refreshToken in localStorage
            localStorage.clear()
            useAppStore.setState(initialState)
            localStorage.setItem('refreshToken', refreshToken)
            // accessToken intentionally absent

            // Act
            useAppStore.getState().initAuth()

            // Assert: session must NOT be restored
            expect(useAppStore.getState().isAuthenticated).toBe(false)
          }
        ),
        { numRuns: 100 }
      )
    }
  )

  it(
    'leaves isAuthenticated as false when neither token is present in localStorage',
    () => {
      // Arrange: localStorage is empty (cleared in beforeEach)
      useAppStore.setState(initialState)

      // Act
      useAppStore.getState().initAuth()

      // Assert
      expect(useAppStore.getState().isAuthenticated).toBe(false)
    }
  )
})

// Feature: frontend-backend-integration, Property 2: Token absence after logout
// Validates: Requirements 2.5

describe('useAppStore — Property 2: Token absence after logout', () => {
  beforeEach(() => {
    // Clear localStorage and reset store state before each test
    localStorage.clear()
    useAppStore.setState({
      isAuthenticated: false,
      currentUser: null,
      accessToken: null,
      refreshToken: null,
    })
  })

  it(
    'clears accessToken and refreshToken from localStorage and sets isAuthenticated to false after logout, for any token values',
    () => {
      fc.assert(
        fc.property(
          fc.string({ minLength: 1 }),
          fc.string({ minLength: 1 }),
          (accessToken, refreshToken) => {
            // Arrange: set up an authenticated session by calling login first
            localStorage.clear()
            useAppStore.setState({
              isAuthenticated: false,
              currentUser: null,
              accessToken: null,
              refreshToken: null,
            })

            useAppStore.getState().login({
              accessToken,
              refreshToken,
              expiresIn: '28800',
              user: { id: '1', email: 'test@example.com', fullName: 'Test User', role: 'EDITOR', active: true },
            })

            // Act: call logout
            useAppStore.getState().logout()

            // Assert: tokens must be absent from localStorage
            expect(localStorage.getItem('accessToken')).toBeNull()
            expect(localStorage.getItem('refreshToken')).toBeNull()
            // Assert: isAuthenticated must be false
            expect(useAppStore.getState().isAuthenticated).toBe(false)
          }
        ),
        { numRuns: 100 }
      )
    }
  )
})

// Feature: frontend-backend-integration, Property 4: User object round-trip through store
// Validates: Requirements 2.7

describe('useAppStore — Property 4: User object round-trip through store', () => {
  beforeEach(() => {
    // Clear localStorage and reset store state before each test
    localStorage.clear()
    useAppStore.setState({
      isAuthenticated: false,
      currentUser: null,
      accessToken: null,
      refreshToken: null,
    })
  })

  it(
    'currentUser deeply equals response.user for any UserDto received in a LoginResponse',
    () => {
      fc.assert(
        fc.property(
          fc.record({
            id: fc.string(),
            email: fc.emailAddress(),
            fullName: fc.string(),
            role: fc.constantFrom('ADMIN', 'EDITOR'),
            active: fc.boolean(),
          }),
          (user) => {
            // Arrange: reset state before each iteration
            localStorage.clear()
            useAppStore.setState({
              isAuthenticated: false,
              currentUser: null,
              accessToken: null,
              refreshToken: null,
            })

            const loginResponse = {
              accessToken: 'test-access-token',
              refreshToken: 'test-refresh-token',
              expiresIn: '28800',
              user,
            }

            // Act: call the login store action
            useAppStore.getState().login(loginResponse)

            // Assert: currentUser must deeply equal the user from the response
            expect(useAppStore.getState().currentUser).toEqual(loginResponse.user)
          }
        ),
        { numRuns: 100 }
      )
    }
  )
})
