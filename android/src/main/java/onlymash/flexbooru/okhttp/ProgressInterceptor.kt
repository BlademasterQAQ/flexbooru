/*
 * Copyright (C) 2020. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.okhttp

import okhttp3.Interceptor
import okhttp3.Response

import java.io.IOException
import java.util.HashMap

class ProgressInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val url = request.url.toString()
        val body = response.body ?: return response
        return response.newBuilder().body(ProgressResponseBody(url, body)).build()
    }

    companion object {

        val LISTENER_MAP: MutableMap<String, (Int) -> Unit> = HashMap()

        //注册下载监听
        fun addListener(url: String, callback: (Int) -> Unit) {
            LISTENER_MAP[url] = callback
        }

        //取消注册下载监听
        fun removeListener(url: String) {
            LISTENER_MAP.remove(url)
        }

        fun bindUrlWithInterval(url: String, interval: Long, callback: (Int) -> Unit) {
            var startTime = 0L
            var elapsedTime = interval
            addListener(url) { progress ->
                if (elapsedTime >= interval) {
                    callback.invoke(progress)
                    startTime = System.currentTimeMillis()
                    elapsedTime = 0L
                } else {
                    elapsedTime = System.currentTimeMillis() - startTime
                }
            }
        }
    }
}