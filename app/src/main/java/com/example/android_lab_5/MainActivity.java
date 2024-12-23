package com.example.android_lab_5;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private WeatherApiService apiService;
    private static final String WEATHER_API_KEY = "a620645c9e934624a69200851241912";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Настройка конфигурации OSMDroid
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Установка макета
        setContentView(R.layout.activity_main);

        // Инициализация карты
        mapView = findViewById(R.id.map);
        mapView.setBuiltInZoomControls(true);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(55.75, 37.61)); // Москва

        // Поля для ввода координат и кнопка
        EditText latitudeInput = findViewById(R.id.latitudeInput);
        EditText longitudeInput = findViewById(R.id.longitudeInput);
        Button searchButton = findViewById(R.id.searchButton);

        // Настройка Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(WeatherApiService.class);

        // Кнопка для поиска погоды
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

                if (latitude < -90 || latitude > 90) {
                    Toast.makeText(this, "Широта должна быть от -90 до 90", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (longitude < -180 || longitude > 180) {
                    Toast.makeText(this, "Долгота должна быть от -180 до 180", Toast.LENGTH_SHORT).show();
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
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setTitle("Температура: " + weather.getTempC() + "°C");
        marker.setSnippet("Осадки: " + weather.getPrecipMm() + " мм\nСкорость ветра: " + weather.getWindKph() + " км/ч");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // Очистка предыдущих слоев
        mapView.getOverlays().clear();

        // Добавление маркера
        mapView.getOverlays().add(marker);

        // Добавление круга, визуализирующего осадки
        addRainOverlay(location, weather.getPrecipMm());

        // Центрирование карты
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(location);
    }

    private void addRainOverlay(GeoPoint location, double precipMm) {
        // Радиус круга в зависимости от осадков (чем больше осадки, тем больше радиус)
        double radiusInMeters = precipMm * 1000; // Пример: 1 мм осадков = 1 км радиуса

        Polygon circle = new Polygon(mapView);
        circle.setPoints(Polygon.pointsAsCircle(location, radiusInMeters));
        circle.setFillColor(Color.argb(50, 0, 0, 255)); // Полупрозрачный синий цвет
        circle.setStrokeColor(Color.BLUE);
        circle.setStrokeWidth(2.0f);

        mapView.getOverlays().add(circle);
    }
}
