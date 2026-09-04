package org.better.urn.data.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import org.better.urn.data.UserPreferences
import org.better.urn.ui.universitice.UniversiticeViewModel

@OptIn(ExperimentalEncodingApi::class)
class MoodleAuthTest {

    private val userPreferences = UserPreferences()

    @BeforeTest
    @AfterTest
    fun cleanup() {
        userPreferences.moodleToken = ""
        userPreferences.moodlePassport = null
    }

    // --- Phase 1: Initiator Tests ---

    @Test
    fun testGeneratePassportLengthAndCharset() {
        val passport = MoodleAuthInitiator.generatePassport()
        assertEquals(32, passport.length)
        val validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toSet()
        assertTrue(passport.all { it in validChars })
    }

    @Test
    fun testGeneratePassportRandomness() {
        val passport1 = MoodleAuthInitiator.generatePassport()
        val passport2 = MoodleAuthInitiator.generatePassport()
        assertTrue(passport1 != passport2)
    }

    @Test
    fun testCreateLaunchUrl() {
        val baseUrl = "https://universitice.univ-rouen.fr/"
        val passport = "12345678901234567890123456789012"
        val launchUrl = MoodleAuthInitiator.createLaunchUrl(baseUrl, passport)

        val expected = "https://universitice.univ-rouen.fr/admin/tool/mobile/launch.php?service=moodle_mobile_app&passport=12345678901234567890123456789012&urlscheme=betterurn"
        assertEquals(expected, launchUrl)
    }

    // --- Phase 2: Normalizer Tests ---

    @Test
    fun testNormalizeTokenQueryParam() {
        val input = "betterurn://?token=SGVsbG8X&other=123#fragment"
        val normalized = MoodleAuthNormalizer.normalize(input)
        assertEquals("SGVsbG8X", normalized)
    }

    @Test
    fun testNormalizeNakedUri() {
        val input = "betterurn://SGVsbG8="
        val normalized = MoodleAuthNormalizer.normalize(input)
        assertEquals("SGVsbG8=", normalized)
    }

    @Test
    fun testNormalizeBase64Sanitization() {
        val input = "betterurn://abc%3D%3Ddef%2Bghi%2Fjkl-mno_pqr"
        val normalized = MoodleAuthNormalizer.normalize(input)
        assertEquals("abc==def+ghi/jkl+mno/pqr", normalized)
    }

    @Test
    fun testNormalizeEmpty() {
        assertEquals("", MoodleAuthNormalizer.normalize("   "))
    }

    // --- Phase 3: Parser Tests ---

    @Test
    fun testParsePositionalRule() {
        val md5 = "8f14e45fceea167a5a36dedd4bea2543"
        val token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d"
        val privateToken = "privatestore123"
        val passport = "ABCDEF1234567890ABCDEF1234567890"

        val rawPayload = "$md5:::$token:::$privateToken:::$passport"
        val encodedPayload = Base64.encode(rawPayload.encodeToByteArray())

        val payload = MoodleAuthParser.parse(encodedPayload)
        assertNotNull(payload)
        assertEquals(token, payload.token)
        assertEquals(privateToken, payload.privateToken)
        assertEquals(passport, payload.passport)
        assertEquals(md5, payload.urlHash)
    }

    @Test
    fun testParsePositionalRuleMinimal() {
        val md5 = "8f14e45fceea167a5a36dedd4bea2543"
        val token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d"

        val rawPayload = "$md5:::$token"
        val encodedPayload = Base64.encode(rawPayload.encodeToByteArray())

        val payload = MoodleAuthParser.parse(encodedPayload)
        assertNotNull(payload)
        assertEquals(token, payload.token)
        assertNull(payload.privateToken)
        assertNull(payload.passport)
        assertEquals(md5, payload.urlHash)
    }

    @Test
    fun testParseKeyValueFallback() {
        val rawPayload = "wstoken=9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d:::passport=PASSPORT123"
        val encodedPayload = Base64.encode(rawPayload.encodeToByteArray())

        val payload = MoodleAuthParser.parse(encodedPayload)
        assertNotNull(payload)
        assertEquals("9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d", payload.token)
        assertEquals("PASSPORT123", payload.passport)
    }

    @Test
    fun testParseBareTokenFallback() {
        val rawToken = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d"
        val payload = MoodleAuthParser.parse(rawToken)
        assertNotNull(payload)
        assertEquals(rawToken, payload.token)
        assertNull(payload.passport)
    }

    @Test
    fun testParseBlankInput() {
        assertNull(MoodleAuthParser.parse(""))
    }

    // --- Phase 4: Security Validator Tests ---

    @Test
    fun testValidateMatchingPassport() {
        val passport = "12345678901234567890123456789012"
        val payload = MoodleAuthPayload(
            token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d",
            passport = passport
        )

        assertTrue(MoodleAuthValidator.validate(payload, passport))
    }

    @Test
    fun testValidateMismatchingPassport() {
        val storedPassport = "12345678901234567890123456789012"
        val payload = MoodleAuthPayload(
            token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d",
            passport = "WRONG_PASSPORT_00000000000000"
        )

        assertFalse(MoodleAuthValidator.validate(payload, storedPassport))
    }

    @Test
    fun testValidateMissingStoredPassportWithPayloadPassport() {
        val payload = MoodleAuthPayload(
            token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d",
            passport = "PAYLOAD_PASSPORT"
        )

        assertFalse(MoodleAuthValidator.validate(payload, null))
    }

    @Test
    fun testValidateNonAsciiToken() {
        val payload = MoodleAuthPayload(token = "token_with_é_accent")
        assertFalse(MoodleAuthValidator.validate(payload, null))
    }

    @Test
    fun testValidateJsonTokenRejection() {
        val payload = MoodleAuthPayload(token = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}")
        assertFalse(MoodleAuthValidator.validate(payload, null))
    }

    // --- Phase 5: ViewModel Integration Tests ---

    @Test
    fun testViewModelInitiateLogin() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val url = viewModel.initiateLogin("https://universitice.univ-rouen.fr")

        assertTrue(url.startsWith("https://universitice.univ-rouen.fr/admin/tool/mobile/launch.php"))
        assertNotNull(userPreferences.moodlePassport)
        assertEquals(32, userPreferences.moodlePassport!!.length)
    }

    @Test
    fun testViewModelHandleAuthInputSuccess() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)

        // 1. Initiate login
        viewModel.initiateLogin("https://universitice.univ-rouen.fr")
        val storedPassport = userPreferences.moodlePassport
        assertNotNull(storedPassport)

        // 2. Prepare encoded deep link response with matching passport
        val md5 = "8f14e45fceea167a5a36dedd4bea2543"
        val token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d"
        val rawPayload = "$md5:::$token:::priv123:::$storedPassport"
        val encodedPayload = Base64.encode(rawPayload.encodeToByteArray())
        val deepLink = "betterurn://?token=$encodedPayload"

        // 3. Handle auth input
        val success = viewModel.handleAuthInput(deepLink)

        assertTrue(success)
        assertEquals(token, userPreferences.moodleToken)
        assertNull(userPreferences.moodlePassport) // Passport cleared
    }

    @Test
    fun testViewModelHandleAuthInputRejectionOnPassportMismatch() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)

        // 1. Initiate login
        viewModel.initiateLogin("https://universitice.univ-rouen.fr")

        // 2. Deep link with invalid passport
        val md5 = "8f14e45fceea167a5a36dedd4bea2543"
        val token = "9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d"
        val rawPayload = "$md5:::$token:::priv123:::FAKE_PASSPORT"
        val encodedPayload = Base64.encode(rawPayload.encodeToByteArray())
        val deepLink = "betterurn://?token=$encodedPayload"

        // 3. Handle auth input
        val success = viewModel.handleAuthInput(deepLink)

        assertFalse(success)
        assertEquals("", userPreferences.moodleToken)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
