package com.example.weatherapplication

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.MatrixCursor
import android.util.Log
import android.net.http.HttpException
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.Settings.Global
import android.widget.Toast
import androidx.activity.contextaware.withContextAvailable
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
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
import java.util.TimeZone

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding;

    // Khởi tạo thành phố mặc định là Hanoi
    private var city: String = "hanoi"
    // Khởi tạo danh sách thành phố
    private val listCity = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy danh sách thành phố
        getlistCity()

        // Khởi tạo adapter cho SearchView
        val from = arrayOf("cityName")
        val to = intArrayOf(android.R.id.text1)
        val adapter = SimpleCursorAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            null,
            from,
            to,
            CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
        )
        binding.searchView.suggestionsAdapter = adapter

        // Xử lý sự kiện khi người dùng nhập văn bản vào SearchView
        binding.searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Khi người dùng gửi truy vấn, cập nhật thành phố và lấy thông tin thời tiết
                city = query.toString()
                getCurrentWeather(city)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Khi văn bản truy vấn thay đổi, cập nhật danh sách gợi ý
                val data = listCity.filter { it.contains(newText.toString(), true) }

                val matrixCursor = MatrixCursor(arrayOf(BaseColumns._ID, "cityName"))
                data.forEachIndexed { index: Int, suggestion: String ->
                    matrixCursor.addRow(arrayOf(index, suggestion))
                }

                binding.searchView.suggestionsAdapter.changeCursor(matrixCursor)

                return true
            }
        })

        // Xử lý sự kiện khi người dùng nhấp vào một gợi ý
        binding.searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            @SuppressLint("Range")
            override fun onSuggestionClick(position: Int): Boolean {
                // Khi người dùng nhấp vào một gợi ý, đặt văn bản truy vấn là tên thành phố được gợi ý
                val cursor = binding.searchView.suggestionsAdapter.getItem(position) as Cursor
                val suggestion = cursor.getString(cursor.getColumnIndex("cityName"))
                binding.searchView.setQuery(suggestion, true) // Xác nhận query khi người dùng nhấp vào gợi ý
                return true
            }
        })

        // Lấy thông tin thời tiết cho thành phố mặc định
        getCurrentWeather(city)
    }

    // Phương thức để lấy danh sách thành phố
    private fun getlistCity() {
        GlobalScope.launch(Dispatchers.IO) {
            val response = try {
                RetrofitInstance.countryApi.getCountryList()
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
                val temp = response.body()!!
                for (i in temp.data) {
                    listCity.addAll(i.cities)
                }
            }
        }
    }

    // Phương thức để lấy thông tin thời tiết hiện tại cho một thành phố cụ thể
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

            if (response.isSuccessful && response.body() != null) {
                withContext(Dispatchers.Main) {
                    val data = response.body()!!

                    if (data.weather != null && data.weather.isNotEmpty()) {
                        val iconId = data.weather[0].icon

                        val imgUrl = "https://openweathermap.org/img/wn/$iconId@4x.png"

                        Picasso.get().load(imgUrl).into(binding.imageWeather)


                        // Lấy thông tin về múi giờ từ phản hồi API
                        val timezoneOffsetInSeconds = data.timezone
                        val hours = timezoneOffsetInSeconds / 3600
                        val minutes = (timezoneOffsetInSeconds % 3600) / 60
                        val timezoneOffset = String.format("%02d:%02d", hours, minutes)

                        // Tạo một đối tượng SimpleDateFormat với múi giờ tương ứng
                        val dateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                        dateFormat.timeZone = TimeZone.getTimeZone("GMT$timezoneOffset")

                        binding.tvSunrise.text = dateFormat.format(data.sys.sunrise * 1000)
                        binding.tvSunset.text = dateFormat.format(data.sys.sunset * 1000)

                        // Cập nhật giao diện người dùng với thông tin thời tiết nhận được
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
                        // Hiển thị thông báo nếu không có dữ liệu thời tiết
                        Toast.makeText(this@MainActivity, "Weather data is not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}