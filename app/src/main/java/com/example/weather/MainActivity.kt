package com.example.weather

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.weather.R
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.weather.ui.components.navdrawer.Drawer
import com.example.weather.ui.components.navdrawer.NavDrawerItem
import com.example.weather.ui.theme.WeatherTheme
import com.example.weather.ui.weather.WeatherScreen
import com.example.weather.ui.weatherdetail.WeatherDetailScreen
import com.example.weather.utils.Constants.REQUESTCODE
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*



@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
         fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), REQUESTCODE)
        } else {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val permission = Manifest.permission.ACCESS_FINE_LOCATION
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), REQUESTCODE)
                return
            }
            fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
                if (task.isSuccessful && task.result != null) {
                    lastLocation = task.result

                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)

                    sharedPreferences.edit().putString("lat", (lastLocation)!!.latitude.toString()).apply()
                    sharedPreferences.edit().putString("long", (lastLocation)!!.longitude.toString()).apply()
                    val geoCoder = Geocoder(
                        this,
                        Locale.getDefault()
                    )
                    var result: String = null.toString()
                    val addressList = geoCoder.getFromLocation(
                        (lastLocation)!!.latitude, (lastLocation)!!.longitude, 1
                    )
                    if ((addressList != null && addressList.size > 0)) {
                        val address = addressList.get(0)
                        val sb = StringBuilder()
                        for (i in 0 until address.maxAddressLineIndex) {
                            sb.append(address.getAddressLine(i)).append("")
                        }
                        sb.append(address.locality).append(", ")
                        sb.append(address.countryCode).append("")
                        result = sb.toString()
                        sharedPreferences.edit().putString("selectedAddress", result).apply()
                    }
                    setContent {
                        WeatherTheme {
                            MainScreen()
                        }
                    }
                }
                else {
                    setContent {
                        WeatherTheme {
                            MainScreen()
                        }
                    }
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    Toast.makeText(this@MainActivity,"No location detected. Make sure location is enabled on the device." ,Toast.LENGTH_SHORT).show()
                }
            }

        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUESTCODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION) ===
                                PackageManager.PERMISSION_GRANTED)) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    val permission = Manifest.permission.ACCESS_FINE_LOCATION
                    if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), REQUESTCODE)
                    }
                }
                return
            }
        }
    }
}





@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {

    val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()


    Scaffold(
        scaffoldState = scaffoldState,
        topBar = { TopBar(scope = scope, scaffoldState = scaffoldState) },
        drawerBackgroundColor = Color.White,
        drawerContent = {
            Drawer(
                scope = scope,
                scaffoldState = scaffoldState,
                navController = navController
            )
        }
    ) {
        Navigation(navController = navController)
    }
}

@Composable
fun TopBar(
    scope: CoroutineScope,
    scaffoldState: ScaffoldState

) {

    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences( LocalContext.current)
    val selectedAddress = sharedPreferences.getString("selectedAddress", null)
    TopAppBar(
        title = {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                    CompositionLocalProvider(
                        LocalContentAlpha provides ContentAlpha.high,
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(0.dp, 0.dp, 64.dp, 0.dp),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            text = selectedAddress!!,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    scaffoldState.drawerState.open()
                }
            }) {
                Image(
                    painter = painterResource(id = R.drawable.ic_menu),
                    stringResource(R.string.description)
                )
            }
        },
        backgroundColor = Color(0, 0, 0, 0),
        contentColor = Color.Black,
        elevation = 0.dp,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Navigation(navController: NavHostController) {
    NavHost(navController, startDestination = NavDrawerItem.Home.route) {
        composable(NavDrawerItem.Home.route) {
            WeatherScreen()
        }
        composable(NavDrawerItem.NextSevenDays.route) {
            WeatherDetailScreen()
        }
    }
}