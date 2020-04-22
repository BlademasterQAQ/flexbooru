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

package onlymash.flexbooru.data.api

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import onlymash.flexbooru.common.Keys.HEADER_USER_AGENT
import onlymash.flexbooru.common.Settings
import onlymash.flexbooru.data.model.app.OrderResponse
import onlymash.flexbooru.extension.NetResult
import onlymash.flexbooru.extension.userAgent
import onlymash.flexbooru.util.Logger
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface OrderApi {

    companion object {
        private const val BASE_URL = "https://flexbooru-pay.fiepi.com"
        operator fun invoke(): OrderApi {

            val logger = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    Logger.d("OrderApi", message)
                }
            }).apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val interceptor = Interceptor { chain ->
                val requests =  chain.request().newBuilder()
                    .removeHeader(HEADER_USER_AGENT)
                    .addHeader(HEADER_USER_AGENT, userAgent)
                    .build()
                chain.proceed(requests)
            }

            val client = OkHttpClient.Builder().apply {
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(10, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(interceptor)
                    .addInterceptor(logger)
            }
                .build()

            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(Json(JsonConfiguration(ignoreUnknownKeys = true))
                    .asConverterFactory(contentType))
                .build()
                .create(OrderApi::class.java)
        }

        suspend fun orderChecker(orderId: String, deviceId: String) {
            withContext(Dispatchers.IO) {
                try {
                    val response = OrderApi().checker(orderId, deviceId)
                    val data = response.body()
                    if (response.isSuccessful && data != null) {
                        if (data.success) {
                            Settings.isOrderSuccess = data.activated
                        } else {
                            Settings.isOrderSuccess = false
                            Settings.orderId = ""
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        suspend fun orderRegister(orderId: String, deviceId: String): NetResult<OrderResponse> {
            return withContext(Dispatchers.IO) {
                try {
                    val response = OrderApi().register(orderId, deviceId)
                    val data = response.body()
                    if (response.isSuccessful && data != null) {
                        NetResult.Success(data)
                    } else {
                        NetResult.Error("code: ${response.code()}")
                    }
                } catch (e: Exception) {
                    NetResult.Error(e.toString())
                }
            }
        }
    }

    @GET("/order/checker.json")
    suspend fun checker(
        @Query("order_id") orderId: String,
        @Query("device_id") deviceId: String): Response<OrderResponse>

    @GET("/order/register.json")
    suspend fun register(
        @Query("order_id") orderId: String,
        @Query("device_id") deviceId: String): Response<OrderResponse>
}