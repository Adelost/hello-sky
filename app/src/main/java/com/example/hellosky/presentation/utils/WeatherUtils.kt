import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val WEATHER_API_URL = ""
private const val WEATHER_API_KEY = ""

suspend fun fetchWeatherData(latitude: Double, longitude: Double): Result<JSONObject> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("$WEATHER_API_URL?lat=$latitude&lon=$longitude&appid=$WEATHER_API_KEY")
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.inputStream.bufferedReader().use {
                val response = it.readText()
                Result.success(JSONObject(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
