package org.better.urn.data.izly

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.better.urn.data.SecureStorage
import kotlin.time.Instant

interface IzlyRepository {
    suspend fun hasValidSession(): Boolean
    suspend fun login(phone: String, pin: String): Result<Boolean>
    suspend fun tokenize(smsLink: String): Result<Unit>
    suspend fun getBalance(): Result<Float>
    suspend fun getHistory(): Result<List<IzlyOperation>>
    suspend fun logout()
    fun getSavedPhone(): String?
    fun savePhone(phone: String)
}

fun IzlyRepository(httpClient: HttpClient = HttpClient(CIO)): IzlyRepository = RealIzlyRepository(httpClient)

@Serializable
private data class IzlyOperationsResponse(
    @SerialName("GetHomePageOperationsResult") val getHomePageOperationsResult: OperationsResultNode? = null,
    @SerialName("ErrorMessage") val errorMessage: String? = null,
    @SerialName("Code") val code: Int? = null,
)

@Serializable
private data class OperationsResultNode(
    @SerialName("UP") val up: UpNode? = null,
    @SerialName("Result") val result: List<IzlyOperationNode> = emptyList(),
)

@Serializable
private data class IzlyOperationNode(
    @SerialName("Id") val id: Long,
    @SerialName("Amount") val amount: Float,
    @SerialName("Date") val date: String,
    @SerialName("IsCredit") val isCredit: Boolean,
    @SerialName("Message") val message: String? = null,
    @SerialName("OperationType") val operationType: Int? = null,
)

@Serializable
private data class UpNode(
    @SerialName("BAL") val balance: Float? = null,
)

class RealIzlyRepository(
    private val httpClient: HttpClient,
) : IzlyRepository {

    companion object {
        private const val SOAP_ENDPOINT = "https://soap.izly.fr/Service.asmx"
        private const val REST_BASE_URL = "https://rest.izly.fr/Service/PublicService.svc/rest"
        private const val CLIENT_VERSION = "7.0"
        private const val CLIENT_TYPE = "PART"
        private const val CHANNEL = "AIZ"
        private const val MODEL = "A"
        private const val USER_AGENT = "ksoap2-android/2.6.0+"

        private const val KEY_TOKEN = "izly_access_token"
        private const val KEY_SID = "izly_session_id"
        private const val KEY_PHONE = "izly_phone"
    }

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private var cachedAccessToken: String?
        get() = SecureStorage.getSecureString(KEY_TOKEN)
        set(value) {
            if (value == null) SecureStorage.removeSecureString(KEY_TOKEN)
            else SecureStorage.saveSecureString(KEY_TOKEN, value)
        }

    private var cachedSessionId: String?
        get() = SecureStorage.getSecureString(KEY_SID)
        set(value) {
            if (value == null) SecureStorage.removeSecureString(KEY_SID)
            else SecureStorage.saveSecureString(KEY_SID, value)
        }

    private var currentUserPhone: String?
        get() = SecureStorage.getSecureString(KEY_PHONE)
        set(value) {
            if (value == null) SecureStorage.removeSecureString(KEY_PHONE)
            else SecureStorage.saveSecureString(KEY_PHONE, value)
        }

    override suspend fun hasValidSession(): Boolean {
        return (cachedAccessToken != null) && (cachedSessionId != null) && (currentUserPhone != null)
    }

    override suspend fun login(phone: String, pin: String): Result<Boolean> {
        return runCatching {
            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <v:Envelope xmlns:i="http://www.w3.org/2001/XMLSchema-instance" xmlns:d="http://www.w3.org/2001/XMLSchema" xmlns:c="http://schemas.xmlsoap.org/soap/encoding/" xmlns:v="http://schemas.xmlsoap.org/soap/envelope/">
                  <v:Header/>
                  <v:Body>
                    <Logon xmlns="Service" id="o0" c:root="1">
                      <version i:type="d:string">$CLIENT_VERSION</version>
                      <channel i:type="d:string">$CHANNEL</channel>
                      <format i:type="d:string">T</format>
                      <model i:type="d:string">$MODEL</model>
                      <language i:type="d:string">fr</language>
                      <user i:type="d:string">$phone</user>
                      <password i:type="d:string">$pin</password>
                      <smoneyClientType i:type="d:string">$CLIENT_TYPE</smoneyClientType>
                      <rooted i:type="d:string">0</rooted>
                    </Logon>
                  </v:Body>
                </v:Envelope>
            """.trimIndent()

            val response = httpClient.post(SOAP_ENDPOINT) {
                headers {
                    append("clientVersion", CLIENT_VERSION)
                    append("smoneyClientType", CLIENT_TYPE)
                    append("SOAPAction", "Service/Logon")
                    append("User-Agent", USER_AGENT)
                    append(HttpHeaders.Connection, "close")
                }
                contentType(ContentType.parse("text/xml;charset=utf-8"))
                setBody(soapBody)
            }

            val unescapedText = response.bodyAsText().replace("&lt;", "<").replace("&gt;", ">")

            if (!response.status.isSuccess() || unescapedText.contains("<Error")) {
                throw IllegalArgumentException("Authentification refusée.")
            }

            currentUserPhone = phone
            true
        }
    }

    override fun getSavedPhone(): String? = currentUserPhone

    override fun savePhone(phone: String) {
        if (phone.isNotBlank()) {
            currentUserPhone = phone
        }
    }

    override suspend fun tokenize(smsLink: String): Result<Unit> {
        return runCatching {
            val cleanLink = smsLink.trim().substringBefore("?").trimEnd('/')

            val realUrl: String = if (cleanLink.startsWith("izly:", ignoreCase = true)) {
                cleanLink
            } else if (cleanLink.startsWith("http://", ignoreCase = true) || cleanLink.startsWith("https://", ignoreCase = true)) {
                val redirectedUrl = HttpClient {
                    followRedirects = false
                }.use { client ->
                    client.get(cleanLink) {
                        headers {
                            append(HttpHeaders.UserAgent, USER_AGENT)
                        }
                    }.headers[HttpHeaders.Location]
                }
                redirectedUrl ?: cleanLink
            } else {
                cleanLink
            }

            val pathSegments = realUrl.trimEnd('/').split("/")
            if (pathSegments.size < 2) {
                throw IllegalArgumentException("Lien expiré ou invalide.")
            }
            val activationCode = pathSegments.last()
            val phone = pathSegments[pathSegments.size - 2]

            val soapBody = """
                <?xml version="1.0" encoding="utf-8"?>
                <v:Envelope xmlns:i="http://www.w3.org/2001/XMLSchema-instance" xmlns:d="http://www.w3.org/2001/XMLSchema" xmlns:c="http://schemas.xmlsoap.org/soap/encoding/" xmlns:v="http://schemas.xmlsoap.org/soap/envelope/">
                  <v:Header/>
                  <v:Body>
                    <Logon xmlns="Service" id="o0" c:root="1">
                      <version i:type="d:string">7.0</version>
                      <channel i:type="d:string">AIZ</channel>
                      <format i:type="d:string">T</format>
                      <model i:type="d:string">A</model>
                      <language i:type="d:string">fr</language>
                      <user i:type="d:string">$phone</user>
                      <password i:null="true" />
                      <smoneyClientType i:type="d:string">PART</smoneyClientType>
                      <rooted i:type="d:string">0</rooted>
                      <actCode i:type="d:string">$activationCode</actCode>
                    </Logon>
                  </v:Body>
                </v:Envelope>
            """.trimIndent()

            val response = httpClient.post(SOAP_ENDPOINT) {
                headers {
                    append("clientVersion", "7.0")
                    append("smoneyClientType", "PART")
                    append("SOAPAction", "Service/Logon")
                    append("User-Agent", USER_AGENT)
                    append(HttpHeaders.Connection, "close")
                }
                contentType(ContentType.parse("text/xml;charset=utf-8"))
                setBody(soapBody)
            }

            val unescapedText = response.bodyAsText().replace("&lt;", "<").replace("&gt;", ">")

            if (!response.status.isSuccess() || unescapedText.contains("<Error") || unescapedText.contains("<Code>")) {
                val errorMsg = unescapedText.substringAfter("<Msg>", "").substringBefore("</Msg>", "").ifBlank { "Activation refusée" }
                throw IllegalArgumentException("Activation refusée : $errorMsg")
            }

            val tokenMatch = "<ACCESS_TOKEN>(.*?)</ACCESS_TOKEN>".toRegex().find(unescapedText)
            val sidMatch = "<SID>(.*?)</SID>".toRegex().find(unescapedText)

            if ((tokenMatch == null) || (sidMatch == null)) {
                throw IllegalStateException("Jetons introuvables.")
            }

            cachedAccessToken = tokenMatch.groupValues[1]
            cachedSessionId = sidMatch.groupValues[1]
            currentUserPhone = phone
        }
    }

    override suspend fun getBalance(): Result<Float> {
        val token = cachedAccessToken ?: return Result.failure(IllegalStateException("Non autorisé"))
        val sessionId = cachedSessionId ?: return Result.failure(IllegalStateException("Non autorisé"))
        val phone = currentUserPhone ?: return Result.failure(IllegalStateException("Non autorisé"))

        return runCatching {
            val response = httpClient.get("$REST_BASE_URL/GetHomePageOperations") {
                url {
                    parameters.append("transactionGroup", "2")
                    parameters.append("top", "1")
                }
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append("channel", CHANNEL)
                    append("clientVersion", CLIENT_VERSION)
                    append("format", "T")
                    append("language", "fr")
                    append("model", MODEL)
                    append("sessionId", sessionId)
                    append("smoneyClientType", CLIENT_TYPE)
                    append("userId", phone)
                    append("version", "2.0")
                }
            }

            if (!response.status.isSuccess()) throw IllegalStateException("Erreur réseau")

            val apiResponse = jsonParser.decodeFromString<IzlyOperationsResponse>(response.bodyAsText())
            apiResponse.errorMessage?.let { throw IllegalStateException(it) }

            apiResponse.getHomePageOperationsResult?.up?.balance
                ?: throw IllegalStateException("Solde introuvable")
        }
    }

    override suspend fun getHistory(): Result<List<IzlyOperation>> {
        val token = cachedAccessToken ?: return Result.failure(IllegalStateException("Non autorisé"))
        val sessionId = cachedSessionId ?: return Result.failure(IllegalStateException("Non autorisé"))
        val phone = currentUserPhone ?: return Result.failure(IllegalStateException("Non autorisé"))

        return runCatching {
            val response = httpClient.get("$REST_BASE_URL/GetHomePageOperations") {
                url {
                    parameters.append("transactionGroup", "2")
                    parameters.append("top", "15")
                }
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append("channel", CHANNEL)
                    append("clientVersion", CLIENT_VERSION)
                    append("format", "T")
                    append("language", "fr")
                    append("model", MODEL)
                    append("sessionId", sessionId)
                    append("smoneyClientType", CLIENT_TYPE)
                    append("userId", phone)
                    append("version", "2.0")
                }
            }

            if (!response.status.isSuccess()) {
                throw IllegalStateException("API error: HTTP ${response.status.value}")
            }

            val apiResponse = jsonParser.decodeFromString<IzlyOperationsResponse>(response.bodyAsText())
            apiResponse.errorMessage?.let { throw IllegalStateException(it) }

            val operationsNode = apiResponse.getHomePageOperationsResult?.result
                ?: throw IllegalStateException("Payload invalide")

            operationsNode.map { node ->
                val title = node.message?.takeIf { it.isNotBlank() } ?: when {
                    node.operationType == 2 -> "Paiement Resto U"
                    node.isCredit || (node.operationType == 0) || (node.operationType == 1) -> "Rechargement Izly"
                    else -> "Paiement Izly"
                }

                IzlyOperation(
                    id = node.id.toString(),
                    amount = node.amount,
                    date = parseWcfDate(node.date),
                    type = title,
                    isCredit = node.isCredit,
                )
            }
        }
    }

    override suspend fun logout() {
        cachedAccessToken = null
        cachedSessionId = null
        currentUserPhone = null
    }

    private fun parseWcfDate(wcfDate: String): String {
        return try {
            val timestampStr = wcfDate
                .substringAfter("/Date(")
                .substringBefore("+")
                .substringBefore("-")
                .substringBefore(")/")
            val epochMillis = timestampStr.toLong()
            val dateTime = Instant.fromEpochMilliseconds(epochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val day = dateTime.day.toString().padStart(2, '0')
            val month = dateTime.month.number.toString().padStart(2, '0')
            val hours = dateTime.hour.toString().padStart(2, '0')
            val minutes = dateTime.minute.toString().padStart(2, '0')

            "$day/$month/${dateTime.year} - $hours:$minutes"
        } catch (_: Exception) {
            wcfDate
        }
    }
}
