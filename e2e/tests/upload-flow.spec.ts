import { test, expect } from '@playwright/test'

/**
 * Verifies the full cross-platform upload flow:
 *   Android app (UploadE2ETest) → backend → web browse page
 *
 * Prerequisites (handled by run-e2e.sh):
 *   - Backend running on http://localhost:8080
 *   - Angular dev server running on http://localhost:4200
 *   - Android instrumented test has already completed successfully,
 *     meaning at least one reading has been uploaded to the backend.
 */
test('reading uploaded by the mobile app appears in the web browse page', async ({ page }) => {
    // 1. Navigate to the login page
    await page.goto('/login')

    // 2. Fill in credentials and submit
    //    The login form uses id="username" and id="password" (see login.html)
    await page.fill('#username', 'admin')
    await page.fill('#password', 'changeme')
    await page.click('button[type="submit"]')

    // 3. Wait for the auth guard to pass and the upload page to load
    await page.waitForURL('/', { timeout: 15_000 })

    // 4. Navigate to the browse page
    await page.goto('/browse')

    // 5. Wait for at least one reading card to render.
    //    The backend is in-memory: the reading uploaded by the Android test is present
    //    for the lifetime of this test run.
    await expect(page.locator('.card').first()).toBeVisible({ timeout: 15_000 })

    // 6. Assert the card contains an image thumbnail (proves the imagePath was stored)
    await expect(page.locator('.card__img').first()).toBeVisible()
})
