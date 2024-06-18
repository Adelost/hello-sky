import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T> callAsync(
    coroutineScope: CoroutineScope,
    suspendFunction: suspend () -> Result<T>,
    onSuccess: (T) -> Unit,
    onFailure: (String) -> Unit
) {
    coroutineScope.launch {
        val result = suspendFunction()
        result.onSuccess { data ->
            onSuccess(data)
        }.onFailure { exception ->
            onFailure(exception.message ?: "Unknown error")
        }
    }
}