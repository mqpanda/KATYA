package com.example.android_lab_5;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private WeatherApiService apiService;
    private static final String WEATHER_API_KEY = "a620645c9e934624a69200851241912";
    private LocationManager locationManager;
    private boolean isFirstLocationUpdate = true;
    private Handler gpsHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        // Инициализация карты
        mapView = findViewById(R.id.map);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(52.0976, 23.7341)); // брест

        EditText latitudeInput = findViewById(R.id.latitudeInput);
        EditText longitudeInput = findViewById(R.id.longitudeInput);
        Button searchButton = findViewById(R.id.searchButton);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        gpsHandler.postDelayed(() -> {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }, 10000);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WeatherApiService.class);

        // Обработчик кнопки для поиска погоды
        searchButton.setOnClickListener(v -> {
            String lat = latitudeInput.getText().toString();
            String lon = longitudeInput.getText().toString();

            if (lat.isEmpty() || lon.isEmpty()) {
                Toast.makeText(this, "Введите координаты", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double latitude = Double.parseDouble(lat);
                double longitude = Double.parseDouble(lon);

                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                    Toast.makeText(this, "Координаты некорректны", Toast.LENGTH_SHORT).show();
                    return;
                }

                fetchWeatherData(latitude, longitude);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Введите числовые значения для координат", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String coordinates = latitude + "," + longitude;
        apiService.getCurrentWeather(WEATHER_API_KEY, coordinates).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse.Current weather = response.body().getCurrent();
                    updateMap(latitude, longitude, weather);
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка соединения", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMap(double latitude, double longitude, WeatherResponse.Current weather) {
        GeoPoint location = new GeoPoint(latitude, longitude);

        // Создание маркера с информацией о погоде
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setTitle("Температура: " + weather.getTempC() + "°C");
        marker.setSnippet("Осадки: " + weather.getPrecipMm() + " мм\nСкорость ветра: " + weather.getWindKph() + " км/ч");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Очистка предыдущих слоев карты
        mapView.getOverlays().clear();
        mapView.getOverlays().add(marker);


        addWeatherIconOverlay(weather.getCondition().getText());

        // Центрирование карты
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(location);
    }

    // /////////////////////////////////////////////
    private String getIconForWeatherCondition(String condition) {
        if (condition.contains("Sunny")) {
            return "sunny_icon";
        } else if (condition.contains("Cloudy")) {
            return "cloudy_icon";
        } else if (condition.contains("Rain")) {
            return "rain_icon";
        } else if (condition.contains("Snow")) {
            return "snow_icon";
        } else {
            return "sunny_icon";
        }
    }



    private void addWeatherIconOverlay(String condition) {
        // Логика выбора иконки на основе текстового описания погоды
        String iconName = getIconForWeatherCondition(condition);
        int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());

        if (iconResId != 0) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), iconResId);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, false); // Уменьшение размера иконки

            Overlay iconOverlay = new Overlay() {
                @Override
                public void draw(Canvas canvas, MapView mapView, boolean shadow) {
                    if (!shadow) {
                        canvas.drawBitmap(scaledBitmap, 20, 20, null); // Отступы от краев (20px)
                    }
                }
            };

            // Очистка предыдущей иконки и добавление новой
            mapView.getOverlays().add(iconOverlay);

            Log.d("WeatherIcon", "Иконка погоды добавлена: " + iconName);
        } else {
            Log.e("WeatherIcon", "Не удалось найти ресурс для иконки: " + iconName);
        }
    }




    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (isFirstLocationUpdate) {
                fetchWeatherData(latitude, longitude);
                isFirstLocationUpdate = false;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };
}
