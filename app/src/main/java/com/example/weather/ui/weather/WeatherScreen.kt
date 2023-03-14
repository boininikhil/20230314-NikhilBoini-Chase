package com.example.weather.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.weather.R
import com.example.weather.ui.theme.Pink
import com.example.weather.ui.theme.Purple
import com.example.weather.ui.theme.Vazir
import com.example.weather.utils.getImageFromUrl
import com.example.weather.utils.getUVIndexColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun WeatherScreen(viewModel: WeatherViewModel = hiltViewModel()) {
    val scrollStateScreen = rememberScrollState()


    val isLoading by remember { viewModel.isLoading }
    val loadError by remember { viewModel.loadError }
    if (!isLoading && loadError.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .verticalScroll(scrollStateScreen)
        ) {
            SearchScreen(viewModel)
            TodayDateBox()
            CurrentWeatherBox()
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(com.example.weather.R.string.later_today_text),
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start,
                )
            }
            HourlyWeatherList()
        }
    } else if (loadError.isNotEmpty()) {
        RetrySection(error = loadError) {
            viewModel.loadCurrentWeatherData()
            viewModel.loadHourlyWeatherData()
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.Cyan)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SearchScreen(viewModel: WeatherViewModel  = hiltViewModel()) {
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    var query by remember { mutableStateOf("") }
    var places by remember { mutableStateOf(listOf<Address>()) }
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search for places") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                val addresses = try {
                    geocoder.getFromLocationName(query, 10)
                } catch (e: IOException) {
                    emptyList<Address>()
                }
                places = addresses!!
            }
        }) {
            Text("Search",color = Color.Black)

        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(places) { address ->
                    val fullAddress = address.getAddressLine(0)
                    Text(
                        text = fullAddress,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                // Get the address and store it in shared preferences
                                val selectedAddress = address.getAddressLine(0)
                                val lat = address.latitude.toString()
                                val long = address.longitude.toString()
                                Log.e("Test", selectedAddress);
                                sharedPreferences
                                    .edit()
                                    .putString("selectedAddress", selectedAddress)
                                    .apply()
                                sharedPreferences
                                    .edit()
                                    .putString("lat", lat)
                                    .apply()
                                sharedPreferences
                                    .edit()
                                    .putString("long", long)
                                    .apply()
                                viewModel.loadHourlyWeatherData()
                                viewModel.loadCurrentWeatherData()



                            }

                    )
                }
            }
        }
    }
}







@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun TodayDateBox(viewModel: WeatherViewModel = hiltViewModel()) {
    val currentDate by remember { viewModel.currentDate }
    val isLoading by remember { viewModel.isLoading }
    val loadingError by remember { viewModel.loadError }
    if (!isLoading) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentDate,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = Vazir,
                        fontSize = 19.sp
                    )
                }
            }
        }
    } else {
        Timber.e(loadingError)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun CurrentWeatherBox(viewModel: WeatherViewModel = hiltViewModel()) {

    val currentTemp by remember { viewModel.currentTemp }
    val currentWeatherType by remember { viewModel.currentWeatherType }
    val currentHumidity by remember { viewModel.currentHumidity }
    val currentUV by remember { viewModel.currentUV }
    val currentImgUrl by remember { viewModel.currentImgUrl }
    val isLoading by remember { viewModel.isLoading }
    val uvIndexColor = getUVIndexColor(currentUV)

    if (!isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp, 16.dp, 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Purple)
                    .padding(4.dp)
            ) {
                Column {
                    Text(
                        text = currentTemp,
                        color = Color.White,
                        fontSize = 72.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        fontFamily = Vazir
                    )
                    Text(
                        text = currentWeatherType,
                        color = Color.White,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(0.dp, 0.dp, 24.dp, 8.dp))
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_humidity),
                                    contentDescription = stringResource(R.string.description)
                                )
                                Text(
                                    text = "Humidity $currentHumidity",
                                    color = Color.White,
                                    modifier = Modifier
                                        .padding(6.dp, 12.dp),
                                    fontSize = 18.sp
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_wind),
                                    contentDescription = stringResource(R.string.description)
                                )
                                Text(
                                    text = "UV $currentUV",
                                    color = uvIndexColor,
                                    modifier = Modifier
                                        .padding(6.dp, 12.dp),
                                    fontSize = 18.sp
                                )
                            }
                        }

                        Image(
                            painter = painterResource(id = getImageFromUrl(currentImgUrl)),
                            contentDescription = stringResource(R.string.description),
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun HourlyWeatherList(viewModel: WeatherViewModel = hiltViewModel()) {
    val hourlyWeatherList by remember { viewModel.hourlyWeatherList }
    val isLoading by remember { viewModel.isLoading }

    LazyRow {
        items(hourlyWeatherList) {
            if (!isLoading) {
                HourlyWeatherBox(
                    time = it.time,
                    imgUrl = it.imgUrl,
                    temp = it.temp,
                    tempHigh = it.highTemp,
                    tempLow = it.lowTemp
                )
            }
        }
    }

}


@Composable
fun HourlyWeatherBox(
    time: String,
    imgUrl: String,
    temp: String,
    tempHigh: String,
    tempLow: String
) {
    Box(modifier = Modifier.padding(8.dp, 4.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Pink)
                .padding(4.dp)
                .size(width = 200.dp, height = 270.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = time,
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(0.dp, 8.dp, 0.dp, 8.dp))
                )

                Image(
                    painter = painterResource(id = getImageFromUrl(imgUrl)),
                    contentDescription = stringResource(R.string.description),
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = temp,
                    color = Color.White,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(0.dp, 0.dp, 0.dp, 8.dp))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp, 0.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ExpandMore,
                            contentDescription = stringResource(R.string.description),
                            modifier = Modifier.size(40.dp),
                            Color.White
                        )
                        Text(
                            text = tempLow,
                            color = Color.White,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(PaddingValues(0.dp, 0.dp, 0.dp, 8.dp))
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp, 0.dp)
                    ) {
                        Icon(
                            Icons.Filled.ExpandLess,
                            contentDescription = stringResource(R.string.description),
                            modifier = Modifier.size(40.dp),
                            Color.White
                        )
                        Text(
                            text = tempHigh,
                            color = Color.White,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(PaddingValues(0.dp, 0.dp, 0.dp, 8.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetrySection(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error, color = Color.Red, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onRetry() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(R.string.retry_text), color = Color.White)
        }
    }
}