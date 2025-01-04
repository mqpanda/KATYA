package com.example.android_lab_5;

public class WeatherResponse {
    private Current current;

    public Current getCurrent() {
        return current;
    }

    public class Current {
        private double temp_c;
        private double wind_kph;
        private double precip_mm;
        private Condition condition;

        public double getTempC() {
            return temp_c;
        }

        public double getWindKph() {
            return wind_kph;
        }

        public double getPrecipMm() {
            return precip_mm;
        }

        public Condition getCondition() {
            return condition;
        }
    }

    public class Condition {
        private String text;

        public String getText() {
            return text;
        }
    }
}
