import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather"
private const val WEATHER_API_KEY = "1c1586e9bac23fc1b74246e48dd46b16"

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