package com.example.weatherapplication

import android.annotation.SuppressLint
import android.util.Log
import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global
//import android.widget.SearchView
import android.widget.Toast
import androidx.activity.contextaware.withContextAvailable
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.SearchView
import com.example.weatherapplication.data.utils.RetrofitInstance
import com.example.weatherapplication.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
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

    private var city: String = "hanoi"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                city = query.toString()
                getCurrentWeather(city)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        getCurrentWeather(city)


    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    private fun getCurrentWeather(city: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.api.getCurrentWeather(
                    city,
                    "metric",
                    "vi",
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

            Log.d("API_RESPONSE", response.raw().toString())

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!

                    if (data.weather != null && data.weather.isNotEmpty()) {
                        val iconId = data.weather[0].icon

                        val imgUrl = "https://openweathermap.org/img/wn/$iconId@4x.png"

                        Picasso.get().load(imgUrl).into(binding.imageWeather)

                        binding.tvSunrise.text =
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.ENGLISH
                            ).format(data.sys.sunrise * 1000)

                        binding.tvSunset.text =
                            SimpleDateFormat(
                                "hh:mm a",
                                Locale.ENGLISH
                            ).format(data.sys.sunset * 1000)

                        binding.apply {
                            tvStatus.text = data.weather[0].description
                            tvWind.text = "${data.wind.speed} m/s"
                            tvCity.text = data.name
                            tvCountry.text = data.sys.country
                            tvTemp.text = "${data.main.temp.toInt()}°C"
                            tvFeelsLike.text = "Cảm giác ${data.main.feels_like.toInt()}°C"
                            tvMinMax.text = "${data.main.temp_min.toInt()}°C/${data.main.temp_max.toInt()}°C"
                            tvHumidity.text  = "${data.main.humidity}%"
                            tvPressure.text = "${data.main.pressure} hPa"
                            tvVisibility.text = "${data.visibility / 1000} km"
                        }
                    } else {
                        // Handle the case where data.weather is null or empty
                        Toast.makeText(this@MainActivity, "Weather data is not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
