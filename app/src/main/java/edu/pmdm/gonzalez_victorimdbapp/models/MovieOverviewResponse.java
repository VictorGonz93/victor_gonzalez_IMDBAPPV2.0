package edu.pmdm.gonzalez_victorimdbapp.models;

import com.google.gson.annotations.SerializedName;

/**
 * Clase que modela la respuesta de la API para obtener información detallada de una película.
 * Incluye datos como el título, fecha de lanzamiento, resumen, calificación y trama de la película.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */
public class MovieOverviewResponse {
    @SerializedName("data")
    public Data data;

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName("title")
        public Title title;

        public Title getTitle() {
            return title;
        }
    }

    public static class Title {
        @SerializedName("titleText")
        public TitleText titleText;

        @SerializedName("releaseDate")
        public ReleaseDate releaseDate;

        @SerializedName("ratingsSummary")
        public RatingsSummary ratingsSummary;

        @SerializedName("plot")
        public Plot plot;

        public TitleText getTitleText() {
            return titleText;
        }

        public ReleaseDate getReleaseDate() {
            return releaseDate;
        }

        public RatingsSummary getRatingsSummary() {
            return ratingsSummary;
        }

        public Plot getPlot() {
            return plot;
        }
    }

    public static class TitleText {
        @SerializedName("text")
        public String text;

        public String getText() {
            return text;
        }
    }


    public static class ReleaseDate {
        @SerializedName("day")
        public Integer day;

        @SerializedName("month")
        public Integer month;

        @SerializedName("year")
        public Integer year;

        public Integer getDay() {
            return day;
        }

        public Integer getMonth() {
            return month;
        }

        public Integer getYear() {
            return year;
        }
    }

    public static class RatingsSummary {
        @SerializedName("aggregateRating")
        public Double aggregateRating;

        public Double getAggregateRating() {
            return aggregateRating;
        }
    }

    public static class Plot {
        @SerializedName("plotText")
        public PlotText plotText;

        public PlotText getPlotText() {
            return plotText;
        }
    }

    public static class PlotText {
        @SerializedName("plainText")
        public String plainText;

        public String getPlainText() {
            return plainText;
        }
    }
}
