package edu.pmdm.gonzalez_victorimdbapp.models;

import java.util.List;

import java.util.List;

/**
 * Clase que modela la respuesta de la API para obtener las películas más populares.
 * Proporciona información como el título, año de lanzamiento, imagen principal y ID de la película.
 *
 * @version 1.0
 * @author Victor Gonzalez Villapalo
 */

public class PopularMoviesResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public static class Data {
        private TopMeterTitles topMeterTitles;

        public TopMeterTitles getTopMeterTitles() {
            return topMeterTitles;
        }
    }

    public static class TopMeterTitles {
        private List<Edge> edges;

        public List<Edge> getEdges() {
            return edges;
        }
    }

    public static class Edge {
        private Node node;

        public Node getNode() {
            return node;
        }
    }

    public static class Node {
        private String id;
        private TitleText titleText;
        private ReleaseYear releaseYear;
        private PrimaryImage primaryImage;

        public String getId() {
            return id;
        }

        public TitleText getTitleText() {
            return titleText;
        }

        public ReleaseYear getReleaseYear() {
            return releaseYear;
        }

        public PrimaryImage getPrimaryImage() {
            return primaryImage;
        }
    }

    public static class TitleText {
        private String text;

        public String getText() {
            return text;
        }
    }

    public static class ReleaseYear {
        private int year;

        public int getYear() {
            return year;
        }
    }

    public static class PrimaryImage {
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
