package com.boxbox.app.cache

import android.content.Context

data class CachedResult<T>(val data: T, val isStale: Boolean, val savedAtMillis: Long?)

object CachedApi {
    const val DEFAULT_TTL_MILLIS = 60 * 60 * 1000L // 1시간: 신선한 캐시가 있으면 네트워크 호출 생략

    /**
     * 캐시가 TTL 이내면 캐시만 쓰고 네트워크 호출 없음.
     * 캐시가 없거나 오래됐으면: 온라인이면 새로 받아서 캐시 갱신, 오프라인/실패면 오래된 캐시라도 반환.
     * 캐시도 없고 받을 수도 없으면 예외를 던짐.
     * forceRefresh=true면 TTL을 무시하고 온라인일 때 항상 네트워크로 새로 받는다 (당겨서 새로고침용).
     */
    suspend fun <T> fetch(
        context: Context,
        key: String,
        clazz: Class<T>,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
        forceRefresh: Boolean = false,
        networkCall: suspend () -> T
    ): CachedResult<T> {
        val age = FileCache.ageMillis(context, key)
        if (!forceRefresh && age != null && age < ttlMillis) {
            FileCache.load(context, key, clazz)?.let {
                return CachedResult(it, isStale = false, savedAtMillis = FileCache.savedAtMillis(context, key))
            }
        }

        if (NetworkUtils.isOnline(context)) {
            try {
                val fresh = networkCall()
                FileCache.save(context, key, fresh)
                return CachedResult(fresh, isStale = false, savedAtMillis = System.currentTimeMillis())
            } catch (_: Exception) {
                // 네트워크는 연결돼 있다는데 요청이 실패함 -> 캐시로 폴백
            }
        }

        val cached = FileCache.load(context, key, clazz)
        if (cached != null) {
            return CachedResult(cached, isStale = true, savedAtMillis = FileCache.savedAtMillis(context, key))
        }
        throw IllegalStateException("No cached data available and network request failed for key=$key")
    }
}
