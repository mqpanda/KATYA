package com.example.android_lab_5;

public class WeatherResponse {
    private Current current;

    public Current getCurrent() {
        return current;
    }

    public class Current {
        private double temp_c; // Температура в градусах Цельсия
        private double wind_kph; // Скорость ветра (км/ч)
        private double precip_mm; // Осадки (мм)

        public double getTempC() {
            return temp_c;
        }

        public double getWindKph() {
            return wind_kph;
        }

        public double getPrecipMm() {
            return precip_mm;
        }
    }
}
