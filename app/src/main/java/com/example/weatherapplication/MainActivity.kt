package com.example.weatherapplication

import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global
import android.widget.Toast
import androidx.activity.contextaware.withContextAvailable
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.weatherapplication.data.utils.RetrofitInstance
import com.example.weatherapplication.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import java.io.IOException
import retrofit2.Response
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getCurrentWeather()
    }

    private fun getCurrentWeather() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    "ha noi",
                    "metric",
                    applicationContext.getString(R.string.api_key)
                )
            } catch (e: IOException) {
                Toast.makeText(this@MainActivity, "app error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            } catch (e: retrofit2.HttpException) {
                Toast.makeText(this@MainActivity, "http error ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!

                    val iconId = data.list[0].weather[0].icon

                    val imgUrl = "http://openweathermap.org/img/wn/$iconId@4x.png"

                    Picasso.get().load(imgUrl).into(binding.imageWeather)

                    binding.tvSunrise.text =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.ENGLISH
                        ).format(data.city.sunrise * 1000)

                    binding.tvSunset.text =
                        SimpleDateFormat(
                            "hh:mm a",
                            Locale.ENGLISH
                        ).format(data.city.sunset * 1000)

                    binding.apply {
                        tvStatus.text = data.list[0].weather[0].description
                        tvWind.text = "${data.list[0].wind.speed} m/s"
                        tvCity.text = data.city.name
                        tvCountry.text = data.city.country
                        tvTemp.text = "${data.list[0].main.temp}°C"
                        tvFeelsLike.text = "Cảm giác như ${data.list[0].main.feels_like}°C"
                        tvMinMax.text = "Thấp nhất ${data.list[0].main.temp_min}°C, Cao nhất${data.list[0].main.temp_max}°C"
                        tvHumidity.text  = "Humidity: ${data.list[0].main.humidity}%"
                        tvRain.text = "Rain: ${data.list[0].rain?.`3h` ?: 0.0} mm"
                        tvVisibility.text = "Visibility: ${data.list[0].visibility / 1000} km"
//                        tvUpdatetime.text = "Last Updated: ${
//                            SimpleDateFormat(
//                                "hh:mm a",
//                                Locale.ENGLISH
//                            ).format(data.list[0].dt * 1000)
//                        }"

                    }
                }
            }
        }
    }
}
